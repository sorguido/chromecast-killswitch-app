// SPDX-License-Identifier: GPL-3.0-or-later
// Original app code; see LICENSE and LICENSE-ADDITIONAL-PERMISSION.
package it.sorguido.chromecastkillswitch.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.sorguido.chromecastkillswitch.R
import it.sorguido.chromecastkillswitch.core.Failure
import it.sorguido.chromecastkillswitch.transport.DiscoveryFailure
import it.sorguido.chromecastkillswitch.transport.TransportReason

/** Keep messages unresolved in retained state; resolve using the current UI configuration. */
data class UiText(@StringRes val resource: Int) {
    @Composable fun resolve(): String = stringResource(resource)
}

internal fun Failure.uiText() = UiText(when (this) {
    Failure.BUSY -> R.string.failure_busy
    Failure.READ_FAILED -> R.string.failure_read
    Failure.IDENTITY_UNAVAILABLE -> R.string.failure_identity
    Failure.WRONG_TARGET -> R.string.failure_wrong_target
    Failure.UNSUPPORTED_TARGET -> R.string.failure_unsupported
    Failure.WRONG_USER -> R.string.failure_user
    Failure.PROTON_MISSING -> R.string.failure_proton
    Failure.CONFIGURATION_CONFLICT -> R.string.failure_conflict
    Failure.BOOT_UNVERIFIABLE -> R.string.failure_boot
    Failure.WRITE_FAILED -> R.string.failure_write
    Failure.READBACK_FAILED -> R.string.failure_readback
    Failure.STORAGE_FAILED -> R.string.failure_storage
    Failure.REBOOT_UNCERTAIN -> R.string.failure_reboot
})

internal fun TransportReason.uiText() = UiText(when (this) {
    TransportReason.GUID_MISMATCH -> R.string.transport_mismatch
    TransportReason.PAIRING_FAILED -> R.string.transport_pairing
    TransportReason.NOT_PAIRED -> R.string.transport_not_paired
    TransportReason.CONNECTION_FAILED -> R.string.connection_failed
    TransportReason.NETWORK_DENIED -> R.string.network_denied
    TransportReason.IDENTITY_STORAGE -> R.string.transport_identity
})

internal fun DiscoveryFailure.uiText() = UiText(when (this) {
    DiscoveryFailure.NETWORK_DENIED -> R.string.network_denied
    DiscoveryFailure.UNAVAILABLE -> R.string.discovery_unavailable
    DiscoveryFailure.FAILED -> R.string.discovery_failed
    DiscoveryFailure.RESOLUTION -> R.string.discovery_resolution
})
