// SPDX-License-Identifier: GPL-3.0-or-later
// Original app code; see LICENSE and LICENSE-ADDITIONAL-PERMISSION.
package it.sorguido.chromecastkillswitch

import android.os.Bundle
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.sorguido.chromecastkillswitch.ui.KillSwitchApp
import it.sorguido.chromecastkillswitch.ui.UiActions

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    // A system change must still be honored if Android has a saved per-app locale override.
    private val languageChanges = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_LOCALE_CHANGED) updateLanguageIfNeeded()
        }
    }

    private fun updateLanguageIfNeeded() {
        if (resources.configuration.locales[0].language != AppLanguage.systemLocale(this).language) recreate()
    }

    private val model: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filter = IntentFilter(Intent.ACTION_LOCALE_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(languageChanges, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(languageChanges, filter)
        enableEdgeToEdge()
        setContent {
            KillSwitchApp(model.state.collectAsStateWithLifecycle().value, UiActions(
                onRefresh = model::refresh, onDiscover = model::discover,
                onPair = model::pair, onConnect = model::connect, onOperate = model::operate,
            ))
        }
    }
    override fun onStart() { super.onStart(); updateLanguageIfNeeded(); model.foreground() }
    override fun onDestroy() { unregisterReceiver(languageChanges); super.onDestroy() }
    override fun onStop() { model.background(); super.onStop() }
}
