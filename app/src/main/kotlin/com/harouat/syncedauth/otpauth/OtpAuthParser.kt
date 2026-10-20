package com.harouat.syncedauth.otpauth

import com.harouat.syncedauth.data.Account
import com.harouat.syncedauth.data.normalized
import com.harouat.syncedauth.totp.Totp

/**
 * The outcome of decoding a scanned QR code.
 *
 * - [drafts] are accounts ready to store (already normalized).
 * - [skipped] carries a human-readable reason for every entry the app can't
 *   handle; empty for a fully-importable code.
 *
 * A result with both lists empty means the payload wasn't an authenticator QR
 * at all.
 */
data class OtpParseResult(
    val drafts: List<Account> = emptyList(),
    val skipped: List<String> = emptyList(),
)

/**
 * Understands the two QR payloads used in the wild:
 *
 * 1. `otpauth://totp/<label>?secret=…&issuer=…` — the URI every service
 *    (GitHub, Binance, Discord…) shows when pairing a third-party authenticator.
 * 2. `otpauth-migration://offline?data=<protobuf>` — what Google Authenticator
 *    produces from "Transfer accounts › Export accounts". The base64 payload is
 *    a protobuf message, decoded hand-rolled in [GoogleAuthMigration] so the
 *    app keeps carrying no protobuf runtime.
 *
 * HOTP entries and SHA-256/512 algorithms are skipped *with a visible reason*
 * rather than silently dropped, because codes generated for them would be wrong.
 */
object OtpAuthParser {

    fun parse(text: String): OtpParseResult = when {
        text.startsWith("otpauth-migration", ignoreCase = true) -> parseGoogleExport(text)
        text.startsWith("otpauth://", ignoreCase = true) -> parseSingle(text)
        else -> OtpParseResult()
    }

    // ---- otpauth:// single-URI ---------------------------------------------

    private fun parseSingle(raw: String): OtpParseResult {
        val noScheme = raw.trim().removePrefix("otpauth://")
        val body = noScheme.substringBefore('?')
        val query = noScheme.substringAfter('?', "")
        val type = body.substringBefore('/').trim().uppercase()

        // Label is "Issuer:account" or just "account", percent-encoded.
        val label = body.substringAfter('/', "")
        val colon = label.indexOf(':')
        val issuerFromLabel = pctDecode(label.take(colon.coerceAtLeast(0)))
        val account = pctDecode(if (colon >= 0) label.substring(colon + 1) else label).trim()

        val p = query.toQueryParams()
        val issuer = p["issuer"].orEmpty().trim().ifBlank { issuerFromLabel }
        val secret = p["secret"].orEmpty().trim()
        val name = account.ifBlank { issuer }

        if (Totp.base32Decode(secret).isEmpty()) {
            return OtpParseResult(
                skipped = listOf("The QR code didn't contain a valid secret key"),
            )
        }

        val digits = p["digits"].orEmpty().toIntOrNull()?.coerceIn(6, 10) ?: 6
        val period = p["period"].orEmpty().toIntOrNull()?.coerceAtLeast(1) ?: 30

        if (type == "HOTP") {
            return OtpParseResult(
                skipped = listOf(
                    "${issuer.ifBlank { name.ifBlank { "This entry" } }} uses HOTP codes, " +
                        "which SyncedAuth doesn't support yet",
                ),
            )
        }

        return OtpParseResult(
            drafts = listOf(
                Account(name = name, issuer = issuer, secret = secret, digits = digits, period = period)
                    .normalized(),
            ),
        )
    }

    // ---- otpauth-migration (Google Authenticator export) -------------------

    private fun parseGoogleExport(raw: String): OtpParseResult {
        val query = raw.trim().substringAfter('?', "")
        val dataParam = query.toQueryParams()["data"].orEmpty()
        val payload = runCatching {
            GoogleAuthMigration.base64Decode(pctDecode(dataParam))
        }.getOrElse { return OtpParseResult(skipped = listOf("The export data couldn't be decoded")) }
        if (payload.isEmpty()) return OtpParseResult(skipped = listOf("The export data is empty"))

        val entries = runCatching { GoogleAuthMigration.decode(payload) }
            .getOrElse { return OtpParseResult(skipped = listOf("The export data couldn't be read")) }
        if (entries.isEmpty()) return OtpParseResult(skipped = listOf("The export data is empty"))

        val drafts = ArrayList<Account>(entries.size)
        val skipped = ArrayList<String>(entries.size)
        entries.forEach { e ->
            val label = e.issuer.ifBlank { e.name }.ifBlank { "An entry" }
            when {
                e.secret.isEmpty() ->
                    skipped += "$label has an unreadable secret key"
                e.type == GoogleAuthMigration.OTP_TYPE_HOTP ->
                    skipped += "$label uses HOTP codes, which SyncedAuth doesn't support yet"
                e.algorithm == GoogleAuthMigration.ALG_SHA256 ->
                    skipped += "$label uses SHA-256, which SyncedAuth doesn't support yet"
                e.algorithm == GoogleAuthMigration.ALG_SHA512 ->
                    skipped += "$label uses SHA-512, which SyncedAuth doesn't support yet"
                e.algorithm == GoogleAuthMigration.ALG_MD5 ->
                    skipped += "$label uses MD5, which isn't a supported TOTP algorithm"
                else -> drafts += Account(
                    name = e.name.trim(),
                    issuer = e.issuer.trim(),
                    secret = Totp.base32Encode(e.secret),
                    digits = when (e.digits) {
                        GoogleAuthMigration.DIGITS_EIGHT -> 8
                        else -> 6
                    },
                    period = 30, // Google export has no period field; GA always used 30s.
                ).normalized()
            }
        }

        return OtpParseResult(drafts, skipped)
    }

    // ---- shared helpers ------------------------------------------------------

    /**
     * Splits a query string into decoded key/value pairs. Decoding is strict:
     * only `%XX` is turned into a byte (a literal `+` stays a `+`), so base64
     * payloads survive untouched — unlike `Uri.getQueryParameter`, which treats
     * `+` as a space.
     */
    private fun String.toQueryParams(): Map<String, String> {
        if (isBlank()) return emptyMap()
        return split('&')
            .filter { it.isNotEmpty() }
            .associate { pair ->
                val eq = pair.indexOf('=')
                val key = if (eq >= 0) pair.substring(0, eq) else pair
                val value = if (eq >= 0) pair.substring(eq + 1) else ""
                pctDecode(key) to pctDecode(value)
            }
    }

    /** Decodes `%XX` sequences only; everything else (including `+`) is kept. */
    internal fun pctDecode(s: String): String {
        if ('%' !in s) return s
        val out = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%' && i + 2 <= s.length - 1) {
                val hex = s.substring(i + 1, i + 3)
                val v = hex.toIntOrNull(16)
                if (v != null) {
                    out.append(v.toChar())
                    i += 3
                    continue
                }
            }
            out.append(c)
            i++
        }
        return out.toString()
    }
}
