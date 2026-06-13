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
                    val matchedAccounts = accounts.filter { account ->
                        account.appPackageName.equals(requestPackageName, ignoreCase = true) ||
                        (parser.webDomain != null && account.websiteUrl.isNotBlank() &&
                            account.websiteUrl.contains(parser.webDomain!!, ignoreCase = true))
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
                if (className.contains("EditText")) {
                    if (inputType and android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD != 0 ||
                        inputType and android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD != 0) {
                        if (passwordNode == null) passwordNode = node
                    } else if (inputType and android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS != 0) {
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
