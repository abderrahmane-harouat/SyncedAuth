package com.harouat.syncedauth.otpauth

import java.util.Base64

/**
 * Minimal decoder for Google Authenticator's "transfer accounts" payload
 * (`otpauth-migration://offline?data=<base64 protobuf>`).
 *
 * The schema is a fixed, well-known message, so the protobuf wire format is
 * parsed by hand instead of pulling in a protobuf runtime:
 *
 * ```
 * message MigrationPayload {            // only field 1 is imported
 *   repeated OtpParameters otp_parameters = 1;
 *   int32 version = 2;                  // ignored
 *   int32 batch_size = 3;               // one QR's batch gets imported at a time
 *   int32 batch_index = 4;
 *   string batch_id = 5;                // field numbers differ across app versions
 * }
 * message OtpParameters {
 *   bytes  secret    = 1;   enum Algorithm algorithm = 4;  int64 counter = 7;
 *   string name      = 2;   enum DigitCount  digits   = 5;
 *   string issuer    = 3;   enum OtpType     type     = 6;
 * }
 * ```
 *
 * Unknown fields are skipped (wire types 0/1/2/5), so extra keys from newer
 * Google Authenticator versions keep decoding. Enum constants match Google's
 * published schema:
 *
 * - algorithm: 1 / 2 / 3 / 4 = SHA1 / SHA256 / SHA512 / MD5
 * - type: 0 / 1 / 2 = unspecified / HOTP / TOTP
 * - digits: 1 / 2 = six / eight
 */
object GoogleAuthMigration {

    const val ALG_SHA256 = 2
    const val ALG_SHA512 = 3
    const val ALG_MD5 = 4
    const val OTP_TYPE_HOTP = 1
    const val DIGITS_EIGHT = 2

    /** One OTP entry inside the migration payload. */
    data class Entry(
        val secret: ByteArray,
        val name: String,
        val issuer: String,
        val algorithm: Int,
        val digits: Int,
        val type: Int,
        val counter: Long,
    )

    fun decode(payload: ByteArray): List<Entry> {
        val reader = Reader(payload)
        val entries = ArrayList<Entry>()
        while (reader.hasNext()) {
            val tag = reader.nextTag() ?: break // truncated trailing data — keep what we got
            if (tag.number == 1 && tag.isLengthDelimited) {
                val bytes = reader.readBytes() ?: break
                entries += parseEntry(bytes)
            } else if (!reader.skip(tag.wireType)) break
        }
        return entries
    }

    private fun parseEntry(data: ByteArray): Entry {
        val reader = Reader(data)
        var secret = ByteArray(0)
        var name = ""
        var issuer = ""
        var algorithm = 0
        var digits = 0
        var type = 0
        var counter = 0L

        while (reader.hasNext()) {
            val tag = reader.nextTag() ?: break
            when {
                tag.number == 1 && tag.isLengthDelimited -> secret = reader.readBytes() ?: ByteArray(0)
                tag.number == 2 && tag.isLengthDelimited -> name = reader.readString() ?: ""
                tag.number == 3 && tag.isLengthDelimited -> issuer = reader.readString() ?: ""
                tag.number == 4 && tag.wireType == 0 -> algorithm = reader.readVarint()?.toInt() ?: 0
                tag.number == 5 && tag.wireType == 0 -> digits = reader.readVarint()?.toInt() ?: 0
                tag.number == 6 && tag.wireType == 0 -> type = reader.readVarint()?.toInt() ?: 0
                tag.number == 7 && tag.wireType == 0 -> counter = reader.readVarint() ?: 0L
                else -> if (!reader.skip(tag.wireType)) break
            }
        }
        return Entry(secret, name, issuer, algorithm, digits, type, counter)
    }

    // ---- protobuf wire reader -------------------------------------------------

    private class Tag(val number: Int, val wireType: Int) {
        val isLengthDelimited get() = wireType == 2
    }

    private class Reader(private val data: ByteArray) {
        var offset = 0
            private set

        fun hasNext() = offset < data.size

        /** Reads the next field tag, or null when the data is truncated. */
        fun nextTag(): Tag? {
            val key = readVarint() ?: return null
            return Tag((key ushr 3).toInt(), (key and 0x7).toInt())
        }

        fun readVarint(): Long? {
            var value = 0L
            var shift = 0
            while (offset < data.size) {
                val b = data[offset].toInt() and 0xFF
                offset++
                value = value or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) return value
                shift += 7
                if (shift > 63) return null // over-long varint
            }
            return null
        }

        fun readBytes(): ByteArray? {
            val length = readVarint()?.toInt() ?: return null
            if (length < 0 || offset + length > data.size) return null
            val out = data.copyOfRange(offset, offset + length)
            offset += length
            return out
        }

        fun readString(): String? = readBytes()?.toString(Charsets.UTF_8)

        /** Consume any unparsed field so decoding can carry on. */
        fun skip(wireType: Int): Boolean = when (wireType) {
            0 -> readVarint() != null
            1 -> if (offset + 8 <= data.size) { offset += 8; true } else false
            2 -> readBytes() != null
            5 -> if (offset + 4 <= data.size) { offset += 4; true } else false
            else -> false // unsupported wire type — stop to avoid corrupt data
        }
    }

    // ---- base64 ----------------------------------------------------------------

    /**
     * Tolerant base64 for the `data` query param: accepts both the standard
     * and URL-safe alphabets, missing or partial `=` padding, and stray
     * whitespace.
     */
    fun base64Decode(value: String): ByteArray {
        var text = value.trim()
            .replace('-', '+')
            .replace('_', '/')
            .filterNot { it.isWhitespace() }
        val remainder = text.length % 4
        if (remainder == 1) text = text.dropLast(1) // broken tail byte — drop it
        else if (remainder != 0) text = text + "=".repeat(4 - remainder)
        return if (text.isEmpty()) ByteArray(0) else Base64.getDecoder().decode(text)
    }
}
