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

package obsidian.server.util.config

import com.uchuhimo.konf.ConfigSpec
import obsidian.server.Obsidian.config

object ObsidianConfig : ConfigSpec("obsidian") {
  /**
   * The address that the server will bind to.
   *
   * `obsidian.host`
   */
  val Host by optional("0.0.0.0")

  /**
   * The server port.
   *
   * `obsidian.port`
   */
  val Port by optional(3030)

  /**
   * The server password.
   *
   * `obsidian.password`
   */
  val Password by optional("")

  /**
   * Whether obsidian should immediately start providing frames after connecting to the voice server.
   *
   * `immediately-provide`
   */
  val ImmediatelyProvide by optional(true, "immediately-provide")

  /**
   * Whether the `Client-Name` header is required for Clients.
   *
   * `require-client-name`
   */
  val RequireClientName by optional(false, "require-client-name")

  /**
   * Used to validate a string given as authorization.
   *
   * @param given The given authorization string.
   *
   * @return true, if the given authorization matches the configured password.
   */
  fun validateAuth(given: String?): Boolean = when {
    config[Password].isEmpty() -> true
    else -> given == config[Password]
  }

  object PlayerUpdates : ConfigSpec("player-updates") {
    /**
     * The delay (in milliseconds) between each player update.
     *
     * `obsidian.player-updates.interval`
     */
    val Interval by optional(5000L, "interval")

    /**
     * Whether the filters object should be sent with Player Updates
     *
     * `obsidian.player-updates.send-filters`
     */
    val SendFilters by optional(true, "send-filters")
  }

  object Lavaplayer : ConfigSpec("lavaplayer") {
    /**
     * Whether garbage collection should be monitored.
     *
     * `obsidian.lavaplayer.gc-monitoring`
     */
    val GcMonitoring by optional(false, "gc-monitoring")

    /**
     * Whether lavaplayer shouldn't allocate audio frames
     *
     * `obsidian.lavaplayer.non-allocating`
     */
    val NonAllocating by optional(false, "non-allocating")

    /**
     * Names of sources that will be enabled.
     *
     * `obsidian.lavaplayer.enabled-sources`
     */
    val EnabledSources by optional(
      setOf(
        "youtube",
        "yarn",
        "bandcamp",
        "twitch",
        "vimeo",
        "nico",
        "soundcloud",
        "local",
        "http"
      ), "enabled-sources"
    )

    /**
     * Whether `scsearch:` should be allowed.
     *
     * `obsidian.lavaplayer.allow-scsearch`
     */
    val AllowScSearch by optional(true, "allow-scsearch")

    object RateLimit : ConfigSpec("rate-limit") {
      /**
       * Ip blocks to use.
       *
       * `obsidian.lavaplayer.rate-limit.ip-blocks`
       */
      val IpBlocks by optional(emptyList<String>(), "ip-blocks")

      /**
       * IPs which should be excluded from usage by the route planner
       *
       * `obsidian.lavaplayer.rate-limit.excluded-ips`
       */
      val ExcludedIps by optional(emptyList<String>(), "excluded-ips")

      /**
       * The route planner strategy to use.
       *
       * `obsidian.lavaplayer.rate-limit.strategy`
       */
      val Strategy by optional("rotate-on-ban")

      /**
       * Whether a search 429 should trigger marking the ip as failing.
       *
       * `obsidian.lavaplayer.rate-limit.search-triggers-fail`
       */
      val SearchTriggersFail by optional(true, "search-triggers-fail")

      /**
       *  -1 = use default lavaplayer value | 0 = infinity | >0 = retry will happen this numbers times
       *
       *  `obsidian.lavaplayer.rate-limit.retry-limit`
       */
      val RetryLimit by optional(-1, "retry-limit")
    }

    object Nico : ConfigSpec("nico") {
      /**
       * The email to use for the Nico Source.
       *
       * `obsidian.lavaplayer.nico.email`
       */
      val Email by optional("")

      /**
       * The password to use for the Nico Source.
       *
       * `obsidian.lavaplayer.nico.password`
       */
      val Password by optional("")
    }

    object YouTube : ConfigSpec("youtube") {
      /**
       * Whether `ytsearch:` should be allowed.
       *
       * `obsidian.lavaplayer.youtube.allow-ytsearch`
       */
      val AllowSearch by optional(true, "allow-search")

      /**
       * Total number of pages (100 tracks per page) to load
       *
       * `obsidian.lavaplayer.youtube.playlist-page-limit`
       */
      val PlaylistPageLimit by optional(6, "playlist-page-limit")
    }
  }
}