package com.harouat.syncedauth.data

/** One stored authenticator entry. Mirrors the original Flutter app's data model. */
data class Account(
    val id: Long = 0L,
    val name: String,
    val issuer: String,
    val secret: String,
    val digits: Int = 6,
    val period: Int = 30,
    /** Pinned accounts always sort before the rest. */
    val pinned: Boolean = false,
    /** Manual ordering; lower sorts first within its pinned group. 0 = unset (new rows get MAX+1). */
    val sortOrder: Int = 0,
)

fun Account.normalized(): Account = copy(
    name = name.trim(),
    issuer = issuer.trim(),
    secret = secret.replace(" ", "").replace("-", "").uppercase().filter { it in "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567" },
)
