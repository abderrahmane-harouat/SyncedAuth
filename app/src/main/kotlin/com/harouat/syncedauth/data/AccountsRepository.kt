package com.harouat.syncedauth.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Plain SQLite storage with the same table schema as the Flutter app
 * (database `authenticator.db`, table `accounts`) plus two local columns
 * (`pinned`, `sort_order`) for pin-to-top and manual drag ordering.
 * Backups stay compatible; the extra columns are never exported as required keys.
 */
class AccountsRepository(context: Context) {

    private val mutex = Mutex()

    private class Db(context: Context) : SQLiteOpenHelper(context, "authenticator.db", null, 2) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE accounts(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "issuer TEXT NOT NULL, " +
                    "secret TEXT NOT NULL, " +
                    "digits INTEGER NOT NULL DEFAULT 6, " +
                    "period INTEGER NOT NULL DEFAULT 30, " +
                    "pinned INTEGER NOT NULL DEFAULT 0, " +
                    "sort_order INTEGER NOT NULL DEFAULT 0)"
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0")
                // Preserve the list the user saw before the update: alphabetical.
                db.execSQL(
                    "UPDATE accounts SET sort_order = " +
                        "(SELECT COUNT(*) FROM accounts AS t2 WHERE t2.name < accounts.name)"
                )
            }
        }
    }

    private val db by lazy { Db(context.applicationContext) }

    suspend fun getAll(): List<Account> = withContext(Dispatchers.IO) {
        val list = ArrayList<Account>()
        db.readableDatabase.rawQuery(
            "SELECT id, name, issuer, secret, digits, period, pinned, sort_order " +
                "FROM accounts ORDER BY pinned DESC, sort_order ASC, id ASC",
            null,
        ).use { c ->
            while (c.moveToNext()) {
                list += Account(
                    id = c.getLong(0),
                    name = c.getString(1),
                    issuer = c.getString(2),
                    secret = c.getString(3),
                    digits = c.getInt(4),
                    period = c.getInt(5),
                    pinned = c.getInt(6) != 0,
                    sortOrder = c.getInt(7),
                )
            }
        }
        list
    }

    suspend fun insert(account: Account): Long = withContext(Dispatchers.IO) {
        mutex.withLock { insertLocked(account) }
    }

    private fun insertLocked(account: Account): Long =
        db.writableDatabase.insert(
            "accounts",
            null,
            ContentValues().apply {
                put("name", account.name)
                put("issuer", account.issuer)
                put("secret", account.secret)
                put("digits", account.digits)
                put("period", account.period)
                put("pinned", if (account.pinned) 1 else 0)
                // sortOrder == 0 means "append after everything else".
                if (account.sortOrder == 0) {
                    put("sort_order", nextSortOrder())
                } else {
                    put("sort_order", account.sortOrder)
                }
            },
        )

    /** Highest existing sort_order, computed inside the caller's transaction/lock. */
    private fun nextSortOrder(): Int =
        db.writableDatabase.rawQuery("SELECT COALESCE(MAX(sort_order), -1) FROM accounts", null)
            .use { c -> if (c.moveToFirst()) c.getInt(0) + 1 else 0 }

    suspend fun setPinned(id: Long, pinned: Boolean) = withContext(Dispatchers.IO) {
        mutex.withLock {
            db.writableDatabase.execSQL(
                "UPDATE accounts SET pinned = ${if (pinned) 1 else 0} WHERE id = $id"
            )
            Unit
        }
    }

    /** Persists the display order: id at index i gets sort_order i. One transaction. */
    suspend fun renumber(orderedIds: List<Long>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val wdb = db.writableDatabase
            wdb.beginTransaction()
            try {
                val stmt = wdb.compileStatement("UPDATE accounts SET sort_order = ? WHERE id = ?")
                orderedIds.forEachIndexed { index, id ->
                    stmt.bindLong(0 + 1, index.toLong())
                    stmt.bindLong(2, id)
                    stmt.executeUpdateDelete()
                }
                wdb.setTransactionSuccessful()
            } finally {
                wdb.endTransaction()
            }
            Unit
        }
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        mutex.withLock {
            db.writableDatabase.delete("accounts", "id = ?", arrayOf(id.toString()))
            Unit
        }
    }

    suspend fun replaceAll(accounts: List<Account>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val wdb = db.writableDatabase
            wdb.beginTransaction()
            try {
                wdb.delete("accounts", null, null)
                accounts.forEach(::insertLocked)
                wdb.setTransactionSuccessful()
            } finally {
                wdb.endTransaction()
            }
        }
    }
}
