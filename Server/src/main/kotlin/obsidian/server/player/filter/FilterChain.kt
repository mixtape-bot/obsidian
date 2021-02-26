/*
 * Obsidian
 * Copyright (C) 2021 Mixtape-Bot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package obsidian.server.player.filter

import com.github.natanbc.lavadsp.natives.TimescaleNativeLibLoader
import com.sedmelluq.discord.lavaplayer.filter.AudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.PcmFilterFactory
import com.sedmelluq.discord.lavaplayer.filter.UniversalPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import obsidian.server.io.Filters
import obsidian.server.player.Link
import obsidian.server.player.filter.impl.*

class FilterChain(val link: Link) {
  var channelMix: ChannelMixFilter? = null
  var equalizer: EqualizerFilter? = null
  var karaoke: KaraokeFilter? = null
  var lowPass: LowPassFilter? = null
  var rotation: RotationFilter? = null
  var timescale: TimescaleFilter? = null
  var tremolo: TremoloFilter? = null
  var vibrato: VibratoFilter? = null
  var volume: VolumeFilter? = null


  /**
   * All enabled filters.
   */
  val enabled: List<Filter>
    get() = listOfNotNull(channelMix, equalizer, karaoke, lowPass, rotation, timescale, tremolo, vibrato, volume)

  /**
   * Get the filter factory.
   */
  fun getFilterFactory(): FilterFactory {
    return FilterFactory()
  }

  /**
   * Applies all enabled filters to the player.
   */
  fun apply() {
    if (enabled.isNotEmpty()) {
      link.player.setFilterFactory(getFilterFactory())
    }
  }

  inner class FilterFactory : PcmFilterFactory {
    override fun buildChain(
      audioTrack: AudioTrack?,
      format: AudioDataFormat,
      output: UniversalPcmAudioFilter
    ): MutableList<AudioFilter> {
      val list: MutableList<FloatPcmAudioFilter> = mutableListOf()

      for (filter in enabled) {
        val audioFilter = filter.build(format, list.removeLastOrNull() ?: output)
          ?: continue

        list.add(audioFilter)
      }

      @Suppress("UNCHECKED_CAST")
      return list as MutableList<AudioFilter>
    }
  }

  companion object {
    /**
     * Whether the timescale filter is enabled.
     */
    val TIMESCALE_ENABLED: Boolean by lazy {
      try {
        TimescaleNativeLibLoader.loadTimescaleLibrary()
        true
      } catch (ex: Throwable) {
        false
      }
    }

    fun from(link: Link, filters: Filters): FilterChain {
      return FilterChain(link).apply {
        channelMix = filters.channelMix
        equalizer = filters.equalizer
        karaoke = filters.karaoke
        lowPass = filters.lowPass
        rotation = filters.rotation
        timescale = filters.timescale
        tremolo = filters.tremolo
        vibrato = filters.vibrato

        filters.volume?.let {
          volume = VolumeFilter(it)
        }
      }
    }
  }
}