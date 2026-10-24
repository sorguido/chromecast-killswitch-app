// SPDX-License-Identifier: GPL-3.0-or-later
// Original app code; see LICENSE and LICENSE-ADDITIONAL-PERMISSION.
package it.sorguido.chromecastkillswitch

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test fun onlyItalianPrimarySelectsItalian() {
        listOf("it", "it-IT", "it-CH").forEach {
            assertEquals(Locale.ITALIAN, AppLanguage.selected(Locale.forLanguageTag(it)))
        }
        listOf("en", "en-US", "fr-FR", "de-DE", "es-ES", "ar", "zh-CN").forEach {
            assertEquals(Locale.ENGLISH, AppLanguage.selected(Locale.forLanguageTag(it)))
        }
        assertEquals(Locale.ENGLISH, AppLanguage.selected(null))
    }
}
