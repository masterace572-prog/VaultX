package com.vaultx.user.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultx.user.data.model.*
import com.vaultx.user.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// AccountViewModel — full MVVM for account list, CRUD, search, password gen
// ─────────────────────────────────────────────────────────────────────────────

sealed class AccountUiState {
    object Idle    : AccountUiState()
    object Loading : AccountUiState()
    object Success : AccountUiState()
    data class Error(val message: String) : AccountUiState()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    // ── Account list (live) ───────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val accounts: StateFlow<List<AccountEntry>> = _searchQuery
        .debounce(300)
        .flatMapLatest { q ->
            if (q.isBlank()) accountRepository.observeAccounts()
            else accountRepository.searchAccounts(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Single account for detail/edit ────────────────────────────────────────
    private val _selectedAccount = MutableStateFlow<AccountEntry?>(null)
    val selectedAccount: StateFlow<AccountEntry?> = _selectedAccount.asStateFlow()

    // ── UI state for save/delete operations ───────────────────────────────────
    private val _uiState = MutableStateFlow<AccountUiState>(AccountUiState.Idle)
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    // ── Account count (for free tier limit) ──────────────────────────────────
    private val _accountCount = MutableStateFlow(0)
    val accountCount: StateFlow<Int> = _accountCount.asStateFlow()

    // ── Add/Edit form state ───────────────────────────────────────────────────
    private val _formPlatformType  = MutableStateFlow(PlatformType.INSTAGRAM)
    private val _formPlatformLabel = MutableStateFlow("")
    private val _formUsername      = MutableStateFlow("")
    private val _formPassword      = MutableStateFlow("")
    private val _formWebsiteUrl    = MutableStateFlow("")
    private val _formAppPackageName= MutableStateFlow("")
    private val _formIsGame        = MutableStateFlow(false)
    private val _formGameName      = MutableStateFlow("")
    private val _formGameId        = MutableStateFlow("")
    private val _formGameEmail     = MutableStateFlow("")
    private val _formGameDescription= MutableStateFlow("")

    val formPlatformType:  StateFlow<PlatformType> = _formPlatformType.asStateFlow()
    val formPlatformLabel: StateFlow<String>        = _formPlatformLabel.asStateFlow()
    val formUsername:      StateFlow<String>        = _formUsername.asStateFlow()
    val formPassword:      StateFlow<String>        = _formPassword.asStateFlow()
    val formWebsiteUrl:    StateFlow<String>        = _formWebsiteUrl.asStateFlow()
    val formAppPackageName:StateFlow<String>        = _formAppPackageName.asStateFlow()
    val formIsGame:        StateFlow<Boolean>       = _formIsGame.asStateFlow()
    val formGameName:      StateFlow<String>        = _formGameName.asStateFlow()
    val formGameId:        StateFlow<String>        = _formGameId.asStateFlow()
    val formGameEmail:     StateFlow<String>        = _formGameEmail.asStateFlow()
    val formGameDescription:StateFlow<String>       = _formGameDescription.asStateFlow()

    // ── Form handlers ─────────────────────────────────────────────────────────
    fun onPlatformTypeChanged(v: PlatformType) {
        _formPlatformType.value  = v
        // Auto-fill package name and website URL for known platforms
        val (pkg, url) = platformMetadata(v)
        _formAppPackageName.value = pkg
        _formWebsiteUrl.value = url
    }

    private fun platformMetadata(type: PlatformType): Pair<String, String> = when (type) {
        PlatformType.INSTAGRAM -> "com.instagram.android" to "https://www.instagram.com"
        PlatformType.TWITTER   -> "com.twitter.android" to "https://x.com"
        PlatformType.FACEBOOK  -> "com.facebook.katana" to "https://www.facebook.com"
        PlatformType.GOOGLE    -> "com.google.android.gms" to "https://accounts.google.com"
        PlatformType.DISCORD   -> "com.discord" to "https://discord.com"
        PlatformType.GAME      -> "" to ""
        PlatformType.CUSTOM    -> "" to ""
    }
    fun onPlatformLabelChanged(v: String)  { _formPlatformLabel.value = v }
    fun onUsernameChanged(v: String)       { _formUsername.value = v }
    fun onPasswordChanged(v: String)       { _formPassword.value = v }
    fun onWebsiteUrlChanged(v: String)     { _formWebsiteUrl.value = v }
    fun onAppPackageNameChanged(v: String) { _formAppPackageName.value = v }
    fun onIsGameChanged(v: Boolean)        { 
        _formIsGame.value = v 
        if (v) {
            val supported = listOf(PlatformType.GOOGLE, PlatformType.FACEBOOK, PlatformType.TWITTER, PlatformType.DISCORD)
            if (_formPlatformType.value !in supported) {
                _formPlatformType.value = PlatformType.GOOGLE
            }
        }
    }
    fun onGameNameChanged(v: String)       { _formGameName.value = v }
    fun onGameIdChanged(v: String)         { _formGameId.value = v }
    fun onGameEmailChanged(v: String)      { _formGameEmail.value = v }
    fun onGameDescriptionChanged(v: String){ _formGameDescription.value = v }

    // ── CRUD operations ───────────────────────────────────────────────────────

    fun saveAccount(onResult: (Boolean, String?) -> Unit) {
        if (_formPassword.value.isBlank()) {
            _uiState.value = AccountUiState.Error("Password cannot be empty")
            onResult(false, "Password cannot be empty")
            return
        }
        if (!_formIsGame.value && _formUsername.value.isBlank()) {
            _uiState.value = AccountUiState.Error("Username or email is required")
            onResult(false, "Username or email is required")
            return
        }
        if (_formIsGame.value && _formGameName.value.isBlank()) {
            _uiState.value = AccountUiState.Error("Game name is required")
            onResult(false, "Game name is required")
            return
        }
        if (_formIsGame.value && _formPlatformLabel.value.isBlank()) {
            _uiState.value = AccountUiState.Error("Account label / nickname is required")
            onResult(false, "Account label / nickname is required")
            return
        }
        viewModelScope.launch {
            _uiState.value = AccountUiState.Loading
            val entry = buildAccountEntry()
            val result = accountRepository.saveAccount(entry)
            if (result.isSuccess) {
                refreshCount()
                _uiState.value = AccountUiState.Success
                onResult(true, "Account saved successfully")
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Failed to save"
                _uiState.value = AccountUiState.Error(msg)
                onResult(false, msg)
            }
        }
    }

    fun updateAccount(onResult: (Boolean, String?) -> Unit) {
        val current = _selectedAccount.value ?: return
        viewModelScope.launch {
            _uiState.value = AccountUiState.Loading
            val updated = buildAccountEntry().copy(
                id        = current.id,
                createdAt = current.createdAt
            )
            val result = accountRepository.updateAccount(updated)
            if (result.isSuccess) {
                _uiState.value = AccountUiState.Success
                onResult(true, "Account updated successfully")
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Failed to update"
                _uiState.value = AccountUiState.Error(msg)
                onResult(false, msg)
            }
        }
    }

    fun deleteAccount(id: String) {
        viewModelScope.launch {
            _uiState.value = AccountUiState.Loading
            val result = accountRepository.deleteAccount(id)
            _uiState.value = if (result.isSuccess) {
                refreshCount()
                AccountUiState.Success
            } else {
                AccountUiState.Error(result.exceptionOrNull()?.message ?: "Failed to delete")
            }
        }
    }

    fun loadAccountForEdit(id: String) {
        viewModelScope.launch {
            val entry = accountRepository.getAccountById(id) ?: return@launch
            _selectedAccount.value = entry
            _formPlatformType.value  = entry.platformType
            _formPlatformLabel.value = entry.platformLabel
            _formUsername.value      = entry.username ?: ""
            _formPassword.value      = entry.password
            _formWebsiteUrl.value    = entry.websiteUrl
            _formAppPackageName.value= entry.appPackageName
            _formIsGame.value        = entry.gameAccount != null
            entry.gameAccount?.let { g ->
                _formGameName.value       = g.gameName
                _formGameId.value         = g.inGameId ?: ""
                _formGameEmail.value      = g.email ?: ""
                _formGameDescription.value= g.description ?: ""
            }
        }
    }

    fun resetForm() {
        _formPlatformType.value  = PlatformType.INSTAGRAM
        _formPlatformLabel.value = ""
        _formUsername.value      = ""
        _formPassword.value      = ""
        _formWebsiteUrl.value    = ""
        _formAppPackageName.value= ""
        _formIsGame.value        = false
        _formGameName.value      = ""
        _formGameId.value        = ""
        _formGameEmail.value     = ""
        _formGameDescription.value= ""
        _uiState.value           = AccountUiState.Idle
        _selectedAccount.value   = null
    }

    fun onSearchQueryChanged(q: String) { _searchQuery.value = q }

    fun resetState() { _uiState.value = AccountUiState.Idle }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildAccountEntry() = AccountEntry(
        id            = UUID.randomUUID().toString(),
        platformType  = _formPlatformType.value,
        platformLabel = _formPlatformLabel.value.ifBlank { _formPlatformType.value.displayName },
        username      = _formUsername.value.takeIf { it.isNotBlank() },
        password      = _formPassword.value,
        websiteUrl    = _formWebsiteUrl.value,
        appPackageName= _formAppPackageName.value,
        gameAccount   = if (_formIsGame.value) GameAccountDetails(
            gameName      = _formGameName.value,
            inGameId      = _formGameId.value.takeIf { it.isNotBlank() },
            email         = _formGameEmail.value.takeIf { it.isNotBlank() },
            description   = _formGameDescription.value.takeIf { it.isNotBlank() }
        ) else null
    )

    private fun refreshCount() {
        viewModelScope.launch {
            _accountCount.value = accountRepository.countAccounts()
        }
    }

    init { refreshCount() }
}
