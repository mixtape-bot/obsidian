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

package obsidian.bedrock.media

import io.netty.buffer.ByteBuf
import obsidian.bedrock.MediaConnection
import obsidian.bedrock.codec.Codec
import obsidian.bedrock.codec.OpusCodec
import obsidian.bedrock.UserConnectedEvent
import obsidian.bedrock.gateway.SpeakingFlags
import obsidian.bedrock.on

abstract class OpusAudioFrameProvider(val connection: MediaConnection) : MediaFrameProvider {
  override var frameInterval = OpusCodec.FRAME_DURATION

  private var speakingMask = SpeakingFlags.NORMAL
  private var counter = 0
  private var lastProvide = false
  private var lastSpeaking = false
  private var lastFramePolled: Long = 0
  private var speaking = false

  private val userConnectedJob = connection.on<UserConnectedEvent> {
    if (speaking) {
      connection.updateSpeakingState(speakingMask)
    }
  }

  override fun dispose() {
    userConnectedJob.cancel()
  }

  override fun canSendFrame(codec: Codec): Boolean {
    if (codec.payloadType != OpusCodec.PAYLOAD_TYPE) {
      return false
    }

    if (counter > 0) {
      return true
    }

    val provide = canProvide()
    if (lastProvide != provide) {
      lastProvide = provide;
      if (!provide) {
        counter = SILENCE_FRAME_COUNT;
        return true;
      }
    }

    return provide;
  }

  override suspend fun retrieve(codec: Codec?, buf: ByteBuf?, timestamp: IntReference?): Boolean {
    if (codec?.payloadType != OpusCodec.PAYLOAD_TYPE) {
      return false
    }

    if (counter > 0) {
      counter--
      buf!!.writeBytes(OpusCodec.SILENCE_FRAME)
      if (speaking) {
        setSpeaking(false)
      }

      timestamp!!.add(960)
      return false
    }

    val startIndex = buf!!.writerIndex()
    retrieveOpusFrame(buf)

    val written = buf.writerIndex() != startIndex
    if (written && !speaking) {
      setSpeaking(true)
    }

    if (!written) {
      counter = SILENCE_FRAME_COUNT
    }

    val now = System.currentTimeMillis()
    val changeTalking = now - lastFramePolled > OpusCodec.FRAME_DURATION

    lastFramePolled = now
    if (changeTalking) {
      setSpeaking(written)
    }

    timestamp!!.add(960)
    return false
  }

  private suspend fun setSpeaking(state: Boolean) {
    speaking = state
    if (speaking != lastSpeaking) {
      lastSpeaking = state
      connection.updateSpeakingState(if (state) speakingMask else 0)
    }
  }


  /**
   * Called every time Opus frame poller tries to retrieve an Opus audio frame.
   *
   * @return If this method returns true, Bedrock will attempt to retrieve an Opus audio frame.
   */
  abstract fun canProvide(): Boolean

  /**
   * If [canProvide] returns true, this method will attempt to retrieve an Opus audio frame.
   *
   *
   * This method must not block, otherwise it might cause severe performance issues, due to event loop thread
   * getting blocked, therefore it's recommended to load all data before or in parallel, not when Bedrock frame poller
   * calls this method. If no data gets written, the frame won't be sent.
   *
   * @param targetBuffer the target [ByteBuf] audio data should be written to.
   */
  abstract fun retrieveOpusFrame(targetBuffer: ByteBuf)

  companion object {
    private const val SILENCE_FRAME_COUNT = 5
  }
}