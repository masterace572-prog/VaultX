package com.vaultx.user.autofill

import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.*
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.vaultx.user.domain.repository.AccountRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class VaultXAutofillService : AutofillService() {

    @Inject
    lateinit var accountRepository: AccountRepository

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        try {
            val context = request.fillContexts.lastOrNull()
            if (context == null) {
                callback.onSuccess(null)
                return
            }

            val structure = context.structure

            // Find username and password nodes
            val parser = AssistStructureParser()
            parser.parse(structure)

            if (parser.usernameNode == null && parser.passwordNode == null) {
                callback.onSuccess(null)
                return
            }

            val requestPackageName = structure.activityComponent?.packageName ?: ""

            // Don't autofill ourselves
            if (requestPackageName == "com.vaultx.user") {
                callback.onSuccess(null)
                return
            }

            serviceScope.launch {
                try {
                    val accounts = accountRepository.getAllAccountsSync()
                    val platformPackages = mapOf(
                        com.vaultx.user.data.model.PlatformType.INSTAGRAM to listOf("com.instagram.android"),
                        com.vaultx.user.data.model.PlatformType.TWITTER to listOf("com.twitter.android", "com.x.twitter"),
                        com.vaultx.user.data.model.PlatformType.FACEBOOK to listOf("com.facebook.katana", "com.facebook.lite", "com.facebook.orca"),
                        com.vaultx.user.data.model.PlatformType.GOOGLE to listOf("com.google.android.gms", "com.android.chrome", "com.google.android.youtube"),
                        com.vaultx.user.data.model.PlatformType.DISCORD to listOf("com.discord")
                    )

                    val platformDomains = mapOf(
                        com.vaultx.user.data.model.PlatformType.INSTAGRAM to listOf("instagram.com"),
                        com.vaultx.user.data.model.PlatformType.TWITTER to listOf("twitter.com", "x.com"),
                        com.vaultx.user.data.model.PlatformType.FACEBOOK to listOf("facebook.com"),
                        com.vaultx.user.data.model.PlatformType.GOOGLE to listOf("google.com"),
                        com.vaultx.user.data.model.PlatformType.DISCORD to listOf("discord.com")
                    )

                    val matchedAccounts = accounts.filter { account ->
                        // Explicit package/url match
                        (account.appPackageName.isNotBlank() && account.appPackageName.equals(requestPackageName, ignoreCase = true)) ||
                        (parser.webDomain != null && account.websiteUrl.isNotBlank() && account.websiteUrl.contains(parser.webDomain!!, ignoreCase = true)) ||
                        // Platform fallback match (Zero-config magic)
                        platformPackages[account.platformType]?.contains(requestPackageName) == true ||
                        (parser.webDomain != null && platformDomains[account.platformType]?.any { parser.webDomain!!.contains(it, ignoreCase = true) } == true)
                    }

                    if (matchedAccounts.isEmpty()) {
                        callback.onSuccess(null)
                        return@launch
                    }

                    val fillResponseBuilder = FillResponse.Builder()

                    for (account in matchedAccounts) {
                        try {
                            val datasetBuilder = Dataset.Builder()
                            val displayString = account.username ?: account.platformLabel
                            val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                                setTextViewText(android.R.id.text1, "VaultX · $displayString")
                            }

                            var hasValue = false
                            if (parser.usernameNode?.autofillId != null && account.username != null) {
                                datasetBuilder.setValue(
                                    parser.usernameNode!!.autofillId!!,
                                    AutofillValue.forText(account.username),
                                    presentation
                                )
                                hasValue = true
                            }
                            if (parser.passwordNode?.autofillId != null) {
                                datasetBuilder.setValue(
                                    parser.passwordNode!!.autofillId!!,
                                    AutofillValue.forText(account.password),
                                    presentation
                                )
                                hasValue = true
                            }
                            if (hasValue) {
                                fillResponseBuilder.addDataset(datasetBuilder.build())
                            }
                        } catch (e: Exception) {
                            // Skip this account if dataset building fails
                            continue
                        }
                    }

                    callback.onSuccess(fillResponseBuilder.build())
                } catch (e: IllegalStateException) {
                    // Vault is locked. Prompt for authentication.
                    val authIntent = android.content.Intent(this@VaultXAutofillService, AutofillAuthActivity::class.java)
                    val pendingIntent = android.app.PendingIntent.getActivity(
                        this@VaultXAutofillService,
                        1001,
                        authIntent,
                        android.app.PendingIntent.FLAG_CANCEL_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                        setTextViewText(android.R.id.text1, "Unlock VaultX to Autofill")
                    }
                    val ids = listOfNotNull(parser.usernameNode?.autofillId, parser.passwordNode?.autofillId).toTypedArray()
                    if (ids.isNotEmpty()) {
                        val fillResponse = FillResponse.Builder()
                            .setAuthentication(ids, pendingIntent.intentSender, presentation)
                            .build()
                        callback.onSuccess(fillResponse)
                    } else {
                        callback.onSuccess(null)
                    }
                } catch (e: Exception) {
                    // Don't crash — just return null (no fill)
                    try {
                        callback.onSuccess(null)
                    } catch (_: Exception) { }
                }
            }
        } catch (e: Exception) {
            // Top-level safety net
            try {
                callback.onSuccess(null)
            } catch (_: Exception) { }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}

class AssistStructureParser {
    var usernameNode: AssistStructure.ViewNode? = null
    var passwordNode: AssistStructure.ViewNode? = null
    var webDomain: String? = null

    fun parse(structure: AssistStructure) {
        try {
            val windowNodeCount = structure.windowNodeCount
            for (i in 0 until windowNodeCount) {
                val windowNode = structure.getWindowNodeAt(i)
                val rootNode = windowNode.rootViewNode
                traverseNode(rootNode)
            }
        } catch (_: Exception) { }
    }

    private fun traverseNode(node: AssistStructure.ViewNode) {
        try {
            if (webDomain == null && node.webDomain != null) {
                webDomain = node.webDomain
            }

            val autofillHints = node.autofillHints
            if (autofillHints != null) {
                if (autofillHints.contains(android.view.View.AUTOFILL_HINT_USERNAME) ||
                    autofillHints.contains(android.view.View.AUTOFILL_HINT_EMAIL_ADDRESS)) {
                    usernameNode = node
                } else if (autofillHints.contains(android.view.View.AUTOFILL_HINT_PASSWORD)) {
                    passwordNode = node
                }
            } else {
                val className = node.className ?: ""
                val inputType = node.inputType
                if (className.contains("EditText") || className.contains("TextInputEditText")) {
                    val variation = inputType and android.text.InputType.TYPE_MASK_VARIATION
                    if (variation == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                        variation == android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD) {
                        if (passwordNode == null) passwordNode = node
                    } else if (variation == android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                               variation == android.text.InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) {
                        if (usernameNode == null) usernameNode = node
                    }
                }
            }

            for (i in 0 until node.childCount) {
                traverseNode(node.getChildAt(i))
            }
        } catch (_: Exception) { }
    }
}
