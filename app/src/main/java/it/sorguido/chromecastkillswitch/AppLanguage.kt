// SPDX-License-Identifier: GPL-3.0-or-later
// Original app code; see LICENSE and LICENSE-ADDITIONAL-PERMISSION.
package it.sorguido.chromecastkillswitch

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/** Only the first system language matters; app-specific preferences never select the language. */
internal object AppLanguage {
    fun selected(primary: Locale?): Locale = if (primary?.language == "it") Locale.ITALIAN else Locale.ENGLISH

    fun systemLocale(context: Context): Locale {
        val system = if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java).systemLocales
        } else {
            Resources.getSystem().configuration.locales
        }
        return selected(if (system.isEmpty) null else system[0])
    }

    fun wrap(context: Context): Context {
        val locale = systemLocale(context)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(locale))
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
    }
}
