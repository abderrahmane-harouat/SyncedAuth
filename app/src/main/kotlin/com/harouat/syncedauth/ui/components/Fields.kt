package com.harouat.syncedauth.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.harouat.syncedauth.ui.theme.LocalSaColors
import com.harouat.syncedauth.ui.theme.SaType

/**
 * Filled text field with a floating label inside the field and a trailing clear ×,
 * matching the reference form style.
 */
@Composable
fun ProtonField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val sa = LocalSaColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(sa.inputFill)
            .border(1.dp, sa.inputBorder, RoundedCornerShape(12.dp))
            .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 2.dp),
    ) {
        Text(text = label, style = SaType.caption.copy(color = sa.textWeak))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = SaType.body1.copy(color = sa.textNormal),
                cursorBrush = SolidColor(sa.accent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(Modifier.padding(vertical = 10.dp)) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,
                                style = SaType.body1.copy(color = sa.textPlaceholder),
                                maxLines = 1,
                            )
                        }
                        inner()
                    }
                },
            )
            AnimatedVisibility(visible = value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = sa.textPlaceholder,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * Segmented control: gray rounded track with a white selected pill.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sa = LocalSaColors.current
    Row(
        modifier = modifier
            .height(36.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(100))
            .background(sa.inputFill)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(100))
                    .background(if (isSelected) sa.card else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    style = SaType.body2Medium,
                    color = if (isSelected) sa.textNormal else sa.textWeak,
                )
            }
        }
    }
}
