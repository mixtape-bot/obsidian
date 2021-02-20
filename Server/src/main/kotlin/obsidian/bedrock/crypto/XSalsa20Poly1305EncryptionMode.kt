package obsidian.bedrock.crypto

import io.netty.buffer.ByteBuf

class XSalsa20Poly1305EncryptionMode : EncryptionMode {
  override val name: String = "xsalsa20_poly1305"

  private val extendedNonce = ByteArray(24)
  private val m = ByteArray(984)
  private val c = ByteArray(984)
  private val nacl = TweetNaclFastInstanced()

  override fun box(opus: ByteBuf, start: Int, output: ByteBuf, secretKey: ByteArray): Boolean {
    for (i in c.indices) {
      m[i] = 0
      c[i] = 0
    }

    for (i in 0 until start) m[i + 32] = opus.readByte()

    output.getBytes(0, extendedNonce, 0, 12)
    if (nacl.cryptoSecretboxXSalsa20Poly1305(c, m, start + 32, extendedNonce, secretKey) == 0) {
      for (i in 0 until start + 16) {
        output.writeByte(c[i + 16].toInt())
      }

      return true
    }

    return false
  }
}