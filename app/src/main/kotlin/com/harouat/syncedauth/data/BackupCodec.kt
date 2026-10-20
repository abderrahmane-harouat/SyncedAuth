package com.harouat.syncedauth.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * JSON backup format — compatible with the Flutter app's export:
 * {"version":"1.0","exported_at":"...","accounts":[{name,issuer,secret,digits,period,...}]}
 *
 * The five original keys are always present so the Flutter app can still read our file.
 * Two extra local-only keys ("pinned", "order") carry pin-to-top and manual ordering;
 * the Flutter app ignores unknown keys, so round-tripping stays safe. Old backups
 * (without them) import fine too.
 */
object BackupCodec {

    fun encode(accounts: List<Account>): String {
        val arr = JSONArray()
        accounts.forEachIndexed { index, a ->
            arr.put(
                JSONObject()
                    .put("name", a.name)
                    .put("issuer", a.issuer)
                    .put("secret", a.secret)
                    .put("digits", a.digits)
                    .put("period", a.period)
                    .put("pinned", if (a.pinned) 1 else 0)
                    .put("order", index)
            )
        }
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
        return JSONObject()
            .put("version", "1.0")
            .put("exported_at", fmt.format(Date()))
            .put("accounts", arr)
            .toString()
    }

    fun decode(json: String): List<Account> {
        val root = JSONObject(json)
        val arr = root.optJSONArray("accounts") ?: throw IllegalArgumentException("missing accounts array")
        val list = ArrayList<Account>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val secret = o.optString("secret").trim()
            if (secret.isEmpty()) continue
            list += Account(
                name = o.optString("name"),
                issuer = o.optString("issuer"),
                secret = secret,
                digits = o.optInt("digits", 6).coerceIn(6, 10),
                period = o.optInt("period", 30).coerceIn(1, 3600),
                pinned = o.optInt("pinned", 0) != 0,
                sortOrder = o.optInt("order", i + 1),
            )
        }
        if (list.isEmpty()) throw IllegalArgumentException("no valid accounts in backup")
        return list
    }
}
