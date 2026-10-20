package com.harouat.syncedauth.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.harouat.syncedauth.ui.theme.LocalSaColors
import com.harouat.syncedauth.ui.theme.SaType

/** Gray pill search bar anchored at the bottom of the home screen. */
@Composable
fun SearchPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val sa = LocalSaColors.current
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(100))
            .background(sa.pillFill),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = sa.textPlaceholder,
            modifier = Modifier
                .padding(start = 14.dp)
                .size(20.dp),
        )
        Box(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = SaType.body1.copy(color = sa.textPlaceholder),
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = SaType.body1.copy(color = sa.textNormal),
                cursorBrush = SolidColor(sa.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AnimatedVisibility(visible = value.isNotEmpty()) {
            IconButton(
                onClick = { onValueChange("") },
                modifier = Modifier.size(36.dp),
            ) {
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
