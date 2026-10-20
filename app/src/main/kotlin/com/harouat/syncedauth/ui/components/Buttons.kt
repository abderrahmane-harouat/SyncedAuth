package com.harouat.syncedauth.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harouat.syncedauth.ui.theme.LocalSaColors
import com.harouat.syncedauth.ui.theme.SaType

/** Full-width pill button with the primary purple gradient and a soft glow. */
@Composable
fun GradientButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val sa = LocalSaColors.current
    Box(
        modifier = modifier
            .height(52.dp)
            .shadow(elevation = 10.dp, shape = CircleShape, spotColor = sa.focus, ambientColor = sa.accent)
            .clip(CircleShape)
            .then(
                if (enabled) Modifier.background(sa.buttonGradient) else Modifier.background(sa.pillFill)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = SaType.body1Bold,
            color = if (enabled) Color.White else sa.textPlaceholder,
        )
    }
}

/** Secondary pill button: transparent with a thin gray border. */
@Composable
fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sa = LocalSaColors.current
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = CircleShape,
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, sa.cardBorder),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = label, style = SaType.body1Bold, color = sa.textNormal)
        }
    }
}

/** Round white gear button, top-right of the home header. */
@Composable
fun GearButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val sa = LocalSaColors.current
    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = sa.card,
        border = androidx.compose.foundation.BorderStroke(1.dp, sa.cardBorder),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = sa.textWeak,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * Small tile carrying the service's real brand logo, full-bleed like an app
 * icon; falls back to the issuer initial when the brand is unknown.
 */
@Composable
fun ServiceTile(
    initial: String,
    brand: BrandIcon? = null,
    modifier: Modifier = Modifier,
) {
    val sa = LocalSaColors.current
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(
                // Vector marks are monochrome/multicolor glyphs designed for a
                // light square; bitmaps carry their own full-bleed background.
                if (brand is BrandIcon.Vector || brand == null) {
                    Modifier
                        .background(Color.White)
                        .border(1.dp, Color(0x14000000), RoundedCornerShape(10.dp))
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (brand) {
            // Color.Unspecified so the vector's own brand colors are not tinted over.
            is BrandIcon.Vector -> Icon(
                painter = painterResource(brand.res),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp),
            )
            is BrandIcon.Bitmap -> Image(
                painter = painterResource(brand.res),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp),
            )
            null -> Text(
                text = initial.take(1).uppercase(),
                style = SaType.body1Bold.copy(fontSize = 17.sp),
                color = sa.accent,
            )
        }
    }
}
