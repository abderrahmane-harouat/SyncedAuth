package com.harouat.syncedauth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.harouat.syncedauth.data.Account
import com.harouat.syncedauth.totp.Totp
import com.harouat.syncedauth.ui.theme.LocalSaColors
import com.harouat.syncedauth.ui.theme.SaType
import kotlinx.coroutines.launch

/** Groups code digits for reading aloud: 6 digits -> 3+3, 8 digits -> 4+4. */
fun formatCode(code: String): String = when (code.length) {
    6 -> code.substring(0, 3) + " " + code.substring(3)
    8 -> code.substring(0, 4) + " " + code.substring(4)
    else -> code
}

/**
 * White rounded account card: brand tile + names + countdown ring, divider,
 * monospaced code with "next" preview.
 *
 * Swipe actions (Material3 SwipeToDismissBox): drag left fires Delete
 * (which asks for confirmation), drag right toggles pin. Actions fire on
 * release past the threshold and the card always springs back closed.
 * Tapping the card copies the code.
 */
@Composable
fun CodeCard(
    account: Account,
    nowSeconds: Long,
    query: String,
    onCopy: (String) -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sa = LocalSaColors.current
    val counter = nowSeconds / account.period
    val currentCode = remember(account.secret, counter, account.digits) {
        Totp.codeForCounter(account.secret, counter, account.digits)
    }
    val nextCode = remember(account.secret, counter, account.digits) {
        Totp.codeForCounter(account.secret, counter + 1, account.digits)
    }
    val remaining = (account.period - (nowSeconds % account.period)).toInt().coerceAtLeast(1)

    // Swipe left fires Delete (which asks for confirmation), swipe right
    // toggles pin. The card fully swipes away on release past the threshold,
    // then immediately animates back to closed after firing the action.
    //
    // Deliberately NOT rememberSwipeToDismissBoxState: that one is
    // rememberSaveable and persists the dismissed value. Pinning re-sorts the
    // list, which can recreate this item's composition mid-dismiss — restoring
    // the card fully swiped off-screen with gestures disabled (permanently
    // invisible). A fresh state always starts centered.
    var lastFired by remember { mutableStateOf<SwipeToDismissBoxValue?>(null) }
    val scope = rememberCoroutineScope()
    val dismissState = remember(account.id) {
        SwipeToDismissBoxState(
            initialValue = SwipeToDismissBoxValue.Settled,
            positionalThreshold = { totalDistance -> totalDistance * 0.35f },
        )
    }
    // Rearm once the card has fully closed again, so the same swipe can be
    // repeated. Also guards against duplicate onDismiss callbacks, which
    // would otherwise toggle the pin straight back off.
    LaunchedEffect(dismissState) {
        snapshotFlow { dismissState.settledValue }
            .collect { settled -> if (settled == SwipeToDismissBoxValue.Settled) lastFired = null }
    }
    // Material3 re-fires onDismiss whenever the lambda passed to it changes
    // identity (any recomposition while the card is settling). Route the
    // action through a stable lambda holding up-to-date callbacks instead.
    val fireAction by rememberUpdatedState<(SwipeToDismissBoxValue) -> Unit> { direction ->
        if (lastFired != direction) {
            lastFired = direction
            when (direction) {
                SwipeToDismissBoxValue.EndToStart -> onDelete()
                SwipeToDismissBoxValue.StartToEnd -> onTogglePin()
                SwipeToDismissBoxValue.Settled -> Unit
            }
        }
    }
    val onDismissed = remember(dismissState, scope) {
        { direction: SwipeToDismissBoxValue ->
            fireAction(direction)
            scope.launch { dismissState.reset() }
            Unit
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val revealingDelete = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (revealingDelete) sa.error else sa.accent),
                contentAlignment = if (revealingDelete) {
                    Alignment.CenterEnd
                } else {
                    Alignment.CenterStart
                },
            ) {
                Row(
                    Modifier.padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = if (revealingDelete) {
                            Icons.Outlined.Delete
                        } else if (account.pinned) {
                            Icons.Rounded.PushPin
                        } else {
                            Icons.Outlined.PushPin
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = when {
                            revealingDelete -> "Delete"
                            account.pinned -> "Unpin"
                            else -> "Pin to top"
                        },
                        style = SaType.body2Medium,
                        color = Color.White,
                    )
                }
            }
        },
        onDismiss = onDismissed,
    ) {
        // Card surface.
        Column(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(sa.card)
                .clickable { onCopy(currentCode) }
                .border(1.dp, sa.cardBorder, RoundedCornerShape(16.dp)),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ServiceTile(
                    initial = account.issuer.ifBlank { account.name }.ifBlank { "·" },
                    brand = remember(account.id, account.name, account.issuer) {
                        BrandIcons.forAccount(account.issuer, account.name)
                    },
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (account.pinned) {
                            Icon(
                                imageVector = Icons.Rounded.PushPin,
                                contentDescription = "Pinned",
                                tint = sa.accent,
                                modifier = Modifier
                                    .size(13.dp)
                                    .padding(end = 0.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                        }
                        val title = account.issuer.ifBlank { account.name }
                        HighlightText(
                            text = title,
                            query = query,
                            style = SaType.body1Medium,
                            color = sa.textNormal,
                        )
                    }
                    if (account.issuer.isNotBlank() && account.name.isNotBlank()) {
                        Text(
                            text = account.name,
                            style = SaType.body2.copy(color = sa.textWeak),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                CountdownRing(secondsRemaining = remaining, period = account.period)
            }

            HorizontalDivider(thickness = 1.dp, color = sa.cardBorder)

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatCode(currentCode),
                    style = SaType.code.copy(color = sa.textNormal, textDirection = TextDirection.Ltr),
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Next",
                        style = SaType.body3.copy(color = sa.textWeak),
                    )
                    Text(
                        text = formatCode(nextCode),
                        style = SaType.monoNext.copy(color = sa.textWeak, textDirection = TextDirection.Ltr),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightText(
    text: String,
    query: String,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color,
) {
    val sa = LocalSaColors.current
    val q = query.trim()
    if (q.isEmpty()) {
        Text(
            text = text,
            style = style.copy(color = color),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        return
    }
    val annotated = buildAnnotatedString {
        append(text)
        var idx = 0
        while (true) {
            val found = text.indexOf(q, idx, ignoreCase = true)
            if (found < 0) break
            addStyle(
                SpanStyle(color = sa.accent, fontWeight = FontWeight.Bold),
                found,
                (found + q.length).coerceAtMost(text.length),
            )
            idx = found + q.length
        }
    }
    Text(
        text = annotated,
        style = style.copy(color = color),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
