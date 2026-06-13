package com.vaultx.user.autofill

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.vaultx.user.presentation.theme.VaultXTheme
import com.vaultx.user.presentation.ui.auth.VaultLockScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity for autofill authentication flow.
 * Hosts the VaultLockScreen to unlock the vault before autofilling.
 */
@AndroidEntryPoint
class AutofillAuthActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VaultXTheme {
                VaultLockScreen(
                    onUnlockSuccess = {
                        setResult(Activity.RESULT_OK)
                        finish()
                    },
                    onFallbackToLogin = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
}
