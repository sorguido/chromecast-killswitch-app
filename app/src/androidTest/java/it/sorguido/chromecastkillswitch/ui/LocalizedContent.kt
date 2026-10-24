// SPDX-License-Identifier: GPL-3.0-or-later
// Original app code; see LICENSE and LICENSE-ADDITIONAL-PERMISSION.
package it.sorguido.chromecastkillswitch.ui

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** Test-only resource context. Does not change phone language or ship in the app. */
@Composable
internal fun LocalizedContent(locale: Locale = Locale.ITALIAN, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val configuration = Configuration(LocalConfiguration.current).apply { setLocales(LocaleList(locale)) }
    val localized = base.createConfigurationContext(configuration)
    CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides configuration, content = content)
}
