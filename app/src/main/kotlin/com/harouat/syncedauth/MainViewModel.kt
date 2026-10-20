package com.harouat.syncedauth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.harouat.syncedauth.data.Account
import com.harouat.syncedauth.data.AccountsRepository
import com.harouat.syncedauth.data.BackupCodec
import com.harouat.syncedauth.data.normalized
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Stable display order: pinned first, then manual order, then insertion. */
private val displayOrder = compareBy<Account>({ !it.pinned }, { it.sortOrder }, { it.id })

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AccountsRepository(app)

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val visibleAccounts: StateFlow<List<Account>> =
        combine(_accounts, _query) { list, q ->
            if (q.isBlank()) list
            else list.filter {
                it.name.contains(q.trim(), ignoreCase = true) ||
                    it.issuer.contains(q.trim(), ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pinnedCount: StateFlow<Int> = _accounts
        .map { list -> list.count { it.pinned } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage

    private var lastDeleted: Account? = null

    init {
        viewModelScope.launch {
            _accounts.value = repo.getAll()
            _loaded.value = true
        }
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun addAccount(name: String, issuer: String, secret: String, digits: Int, period: Int) {
        viewModelScope.launch {
            repo.insert(
                Account(name = name, issuer = issuer, secret = secret, digits = digits, period = period)
                    .normalized()
            )
            _accounts.value = repo.getAll()
        }
    }

    /** Stores every account scanned off a QR code, then reports what landed. */
    fun addAccounts(newAccounts: List<Account>) {
        if (newAccounts.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                newAccounts.forEach { repo.insert(it.normalized()) }
                _accounts.value = repo.getAll()
            }
                .onSuccess {
                    val plural = if (newAccounts.size == 1) "account" else "accounts"
                    message("${newAccounts.size} $plural added from the QR code")
                }
                .onFailure { message("Couldn't save the scanned accounts") }
        }
    }

    /** Deletes immediately; caller offers an "undo" snackbar that calls [undoDelete]. */
    fun delete(account: Account) {
        lastDeleted = account
        viewModelScope.launch {
            repo.delete(account.id)
            _accounts.value = repo.getAll()
        }
    }

    fun undoDelete() {
        val account = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch {
            // Insert without the stale id; keep pin + sort slot so undo restores position.
            repo.insert(account.copy(id = 0L, sortOrder = account.sortOrder))
            _accounts.value = repo.getAll()
        }
    }

    /** Pin/unpin; the list re-sorts immediately and the change is persisted. */
    fun togglePin(account: Account) {
        val updated = account.copy(pinned = !account.pinned)
        viewModelScope.launch {
            repo.setPinned(account.id, updated.pinned)
            _accounts.value = _accounts.value.map { if (it.id == account.id) updated else it }
                .sortedWith(displayOrder)
        }
    }

    /**
     * Live reorder during a drag: applies the move to the in-memory list only.
     * Call [commitReorder] when the finger lifts to persist it.
     */
    fun reorderInCache(fromIndex: Int, toIndex: Int) {
        val list = _accounts.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        list.add(toIndex, list.removeAt(fromIndex))
        _accounts.value = list
    }

    /** Persists the current list order as sort_order 0..n-1. */
    fun commitReorder() {
        viewModelScope.launch {
            repo.renumber(_accounts.value.map { it.id })
        }
    }

    fun exportJson(): String = BackupCodec.encode(_accounts.value)

    /** Replaces all accounts with the ones in [text]. Returns the imported count. */
    suspend fun importJson(text: String): Int {
        val imported = BackupCodec.decode(text)
        repo.replaceAll(imported)
        _accounts.value = repo.getAll()
        return imported.size
    }

    fun message(msg: String) {
        _uiMessage.value = msg
    }

    fun consumeMessage() {
        _uiMessage.value = null
    }
}
