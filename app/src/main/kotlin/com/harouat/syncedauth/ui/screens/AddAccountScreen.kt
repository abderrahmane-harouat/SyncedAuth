package com.harouat.syncedauth.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.harouat.syncedauth.MainViewModel
import com.harouat.syncedauth.ui.components.ProtonField
import com.harouat.syncedauth.ui.components.SegmentedControl
import com.harouat.syncedauth.ui.theme.LocalSaColors
import com.harouat.syncedauth.ui.theme.SaType

@Composable
fun AddAccountScreen(
    vm: MainViewModel,
    onClose: () -> Unit,
    onScanQr: () -> Unit,
) {
    val sa = LocalSaColors.current
    val focusManager = LocalFocusManager.current

    var name by rememberSaveable { mutableStateOf("") }
    var issuer by rememberSaveable { mutableStateOf("") }
    var secret by rememberSaveable { mutableStateOf("") }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var digitsIndex by rememberSaveable { mutableIntStateOf(0) }
    var periodIndex by rememberSaveable { mutableIntStateOf(0) }
    val digits = listOf(6, 8)[digitsIndex]
    val period = listOf(30, 60)[periodIndex]
    val valid = name.isNotBlank() && secret.isNotBlank()

    val nameFocus = remember { FocusRequester() }

    fun save() {
        if (!valid) return
        focusManager.clearFocus()
        vm.addAccount(name = name, issuer = issuer, secret = secret, digits = digits, period = period)
        onClose()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(sa.pageGradient)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top bar: close | title | save
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = sa.textNormal,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Add account",
                        style = SaType.body18.copy(fontWeight = FontWeight.Bold),
                        color = sa.textNormal,
                    )
                }
                Box(Modifier.width(64.dp), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        text = "Save",
                        style = SaType.body1Bold,
                        color = if (valid) sa.accent else sa.textPlaceholder,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = valid) { save() }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }

            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QRScanOption(onClick = onScanQr)

                Spacer(Modifier.height(6.dp))

                ProtonField(
                    label = "Account (required)",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "eg: username@email.com",
                    modifier = Modifier.focusRequester(nameFocus),
                )
                ProtonField(
                    label = "Issuer (optional)",
                    value = issuer,
                    onValueChange = { issuer = it },
                    placeholder = "eg: Binance",
                )
                ProtonField(
                    label = "Secret key (required)",
                    value = secret,
                    onValueChange = { secret = it },
                    placeholder = "Secret Key",
                    keyboardType = KeyboardType.Ascii,
                )
                Text(
                    text = "Enter the secret key of your account — a long string of letters and digits.",
                    style = SaType.body2,
                    color = sa.textWeak,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )

                // Advanced options card
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(sa.card)
                        .border(1.dp, sa.cardBorder, RoundedCornerShape(12.dp)),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { advanced = !advanced }
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Advanced options",
                            style = SaType.body1Medium,
                            color = sa.textNormal,
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = if (advanced) Icons.Outlined.Remove else Icons.Outlined.Add,
                            contentDescription = null,
                            tint = sa.textWeak,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    AnimatedVisibility(visible = advanced) {
                        Column(
                            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column {
                                Text(
                                    text = "Digits",
                                    style = SaType.caption,
                                    color = sa.textWeak,
                                )
                                Spacer(Modifier.height(6.dp))
                                SegmentedControl(
                                    options = listOf("6", "8"),
                                    selected = digitsIndex,
                                    onSelect = { digitsIndex = it },
                                )
                            }
                            Column(Modifier.padding(bottom = 10.dp)) {
                                Text(
                                    text = "Refresh period (seconds)",
                                    style = SaType.caption,
                                    color = sa.textWeak,
                                )
                                Spacer(Modifier.height(6.dp))
                                SegmentedControl(
                                    options = listOf("30", "60"),
                                    selected = periodIndex,
                                    onSelect = { periodIndex = it },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Entry row to the scanner, styled like HomeScreen's settings rows so the add
 * screen reads as one consistent form.
 */
@Composable
private fun QRScanOption(onClick: () -> Unit) {
    val sa = LocalSaColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(sa.card)
            .border(1.dp, sa.cardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.QrCodeScanner,
            contentDescription = null,
            tint = sa.accent,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text = "Scan a QR code", style = SaType.body1Medium, color = sa.textNormal)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Setup code from any service, or a Google Authenticator transfer QR",
                style = SaType.body2,
                color = sa.textWeak,
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = sa.textPlaceholder,
            modifier = Modifier.size(20.dp),
        )
    }
}
