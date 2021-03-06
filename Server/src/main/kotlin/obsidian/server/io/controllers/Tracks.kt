/*
 * Copyright 2021 MixtapeBot and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package obsidian.server.io.controllers

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.future.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer
import obsidian.server.Obsidian
import obsidian.server.io.search.AudioLoader
import obsidian.server.io.search.LoadType
import obsidian.server.util.TrackUtil
import obsidian.server.util.kxs.AudioTrackSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("TracksController")

fun Routing.tracks() {
  authenticate {
    @Location("/loadtracks")
    data class LoadTracks(val identifier: String)

    get<LoadTracks> { data ->
      val result = AudioLoader(Obsidian.playerManager)
        .load(data.identifier)
        .await()

      if (result.exception != null) {
        logger.error("Track loading failed", result.exception)
      }

      val playlist = result.playlistName?.let {
        LoadTracksResponse.PlaylistInfo(name = it, selectedTrack = result.selectedTrack)
      }

      val exception = if (result.loadResultType == LoadType.LOAD_FAILED && result.exception != null) {
        LoadTracksResponse.Exception(
          message = result.exception!!.localizedMessage,
          severity = result.exception!!.severity
        )
      } else {
        null
      }

      val response = LoadTracksResponse(
        tracks = result.tracks.map(::getTrack),
        type = result.loadResultType,
        playlistInfo = playlist,
        exception = exception
      )

      context.respond(response)
    }

    @Location("/decodetrack")
    data class DecodeTrack(val track: String)

    get<DecodeTrack> {
      val track = TrackUtil.decode(it.track)
      context.respond(getTrackInfo(track))
    }

    post("/decodetracks") {
      val body = call.receive<DecodeTracksBody>()
      context.respond(body.tracks.map(::getTrackInfo))
    }
  }
}

private fun getTrack(audioTrack: AudioTrack): Track =
  Track(track = audioTrack, info = getTrackInfo(audioTrack))

private fun getTrackInfo(audioTrack: AudioTrack): Track.Info =
  Track.Info(
    title = audioTrack.info.title,
    uri = audioTrack.info.uri,
    identifier = audioTrack.info.identifier,
    author = audioTrack.info.author,
    length = audioTrack.duration,
    isSeekable = audioTrack.isSeekable,
    isStream = audioTrack.info.isStream,
    position = audioTrack.position
  )

@Serializable
data class DecodeTracksBody(@Serializable(with = AudioTrackListSerializer::class) val tracks: List<AudioTrack>)

@Serializable
data class LoadTracksResponse(
  @SerialName("load_type")
  val type: LoadType,
  @SerialName("playlist_info")
  val playlistInfo: PlaylistInfo?,
  val tracks: List<Track>,
  val exception: Exception?
) {
  @Serializable
  data class Exception(
    val message: String,
    val severity: FriendlyException.Severity
  )

  @Serializable
  data class PlaylistInfo(
    val name: String,
    @SerialName("selected_track")
    val selectedTrack: Int?
  )
}

@Serializable
data class Track(
  @Serializable(with = AudioTrackSerializer::class)
  val track: AudioTrack,
  val info: Info
) {
  @Serializable
  data class Info(
    val title: String,
    val author: String,
    val uri: String,
    val identifier: String,
    val length: Long,
    val position: Long,
    @SerialName("is_stream")
    val isStream: Boolean,
    @SerialName("is_seekable")
    val isSeekable: Boolean,
  )
}

// taken from docs lmao
object AudioTrackListSerializer : JsonTransformingSerializer<List<AudioTrack>>(ListSerializer(AudioTrackSerializer)) {
  override fun transformDeserialize(element: JsonElement): JsonElement =
    if (element !is JsonArray) {
      JsonArray(listOf(element))
    } else {
      element
    }
}
