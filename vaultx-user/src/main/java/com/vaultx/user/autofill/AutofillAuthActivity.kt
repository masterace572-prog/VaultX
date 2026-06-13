package com.vaultx.user.autofill

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Stub activity for autofill authentication flow.
 * Currently just finishes immediately — biometric gate is handled
 * at the vault-unlock level (VaultLockScreen) rather than per-autofill-request.
 */
@AndroidEntryPoint
class AutofillAuthActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // For now, just finish. The autofill service handles everything directly.
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}
