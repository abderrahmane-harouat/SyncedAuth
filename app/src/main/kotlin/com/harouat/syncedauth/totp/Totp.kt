package com.harouat.syncedauth.totp

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * RFC 6238 TOTP generator (HMAC-SHA1), the same algorithm the Flutter app used.
 */
object Totp {

    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun base32Decode(secret: String): ByteArray {
        val cleaned = secret.uppercase().replace(Regex("[^A-Z2-7]"), "")
        val out = ArrayList<Byte>(cleaned.length * 5 / 8 + 1)
        var buffer = 0
        var bits = 0
        for (c in cleaned) {
            if (c == '=') break
            val idx = BASE32_ALPHABET.indexOf(c)
            if (idx < 0) continue
            buffer = (buffer shl 5) or idx
            bits += 5
            if (bits >= 8) {
                out.add(((buffer shr (bits - 8)) and 0xFF).toByte())
                bits -= 8
            }
        }
        return out.toByteArray()
    }

    /** RFC 4648 base32 without padding (A–Z, 2–7), for secrets decoded from raw bytes. */
    fun base32Encode(bytes: ByteArray): String {
        val out = StringBuilder(((bytes.size + 4) / 5) * 8)
        var buffer = 0
        var bits = 0
        for (byte in bytes) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bits += 8
            while (bits >= 5) {
                out.append(BASE32_ALPHABET[(buffer shr (bits - 5)) and 0x1F])
                bits -= 5
            }
        }
        if (bits > 0) out.append(BASE32_ALPHABET[(buffer shl (5 - bits)) and 0x1F])
        return out.toString()
    }

    fun codeForCounter(secret: String, counter: Long, digits: Int = 6): String {
        val key = base32Decode(secret)
        if (key.isEmpty()) return "0".repeat(digits)
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array())
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val value = ((hash[offset].toLong() and 0x7F) shl 24) or
                ((hash[offset + 1].toLong() and 0xFF) shl 16) or
                ((hash[offset + 2].toLong() and 0xFF) shl 8) or
                (hash[offset + 3].toLong() and 0xFF)
        var modulus = 1L
        repeat(digits) { modulus *= 10 }
        return (value % modulus).toString().padStart(digits, '0')
    }

    fun currentCounter(period: Int = 30): Long =
        System.currentTimeMillis() / 1000 / period

    fun currentCode(secret: String, digits: Int = 6, period: Int = 30): String =
        codeForCounter(secret, currentCounter(period), digits)

    fun nextCode(secret: String, digits: Int = 6, period: Int = 30): String =
        codeForCounter(secret, currentCounter(period) + 1, digits)

    fun secondsRemaining(period: Int = 30): Int =
        (period - (System.currentTimeMillis() / 1000 % period)).toInt()
}
