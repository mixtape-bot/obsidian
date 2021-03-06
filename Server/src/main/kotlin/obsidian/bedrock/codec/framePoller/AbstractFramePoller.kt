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

package obsidian.bedrock.codec.framePoller

import io.netty.buffer.ByteBufAllocator
import io.netty.channel.EventLoopGroup
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import obsidian.bedrock.Bedrock
import obsidian.bedrock.MediaConnection

abstract class AbstractFramePoller(protected val connection: MediaConnection) : FramePoller {
  /**
   * Whether we're polling or not.
   */
  override var polling = false

  /**
   * The [ByteBufAllocator] to use.
   */
  protected val allocator: ByteBufAllocator = Bedrock.byteBufAllocator

  /**
   * The [EventLoopGroup] being used.
   */
  protected val eventLoop: EventLoopGroup = Bedrock.eventLoopGroup

  /**
   * The [eventLoop] as a [ExecutorCoroutineDispatcher]
   */
  protected val eventLoopDispatcher = eventLoop.asCoroutineDispatcher()
}