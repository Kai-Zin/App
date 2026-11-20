package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val BeVietnamPro = FontFamily(
    Font(R.font.be_vietnam_pro, FontWeight.Normal),
    Font(R.font.be_vietnam_pro, FontWeight.Medium),
    Font(R.font.be_vietnam_pro, FontWeight.SemiBold),
    Font(R.font.be_vietnam_pro, FontWeight.Bold),
    Font(R.font.be_vietnam_pro, FontWeight.ExtraBold)
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

fun buildAppTypography(scale: Float = 0.90f): Typography {
    val s = scale.coerceIn(0.75f, 1.25f)
    return Typography(
        headlineMedium = TextStyle(
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.Bold,
            fontSize = (20f * s).sp,
            lineHeight = (26f * s).sp
        ),
        titleLarge = TextStyle(
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.Bold,
            fontSize = (17.5f * s).sp,
            lineHeight = (23f * s).sp
        ),
        titleMedium = TextStyle(
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.Bold,
            fontSize = (14.5f * s).sp,
            lineHeight = (20f * s).sp
        ),
        titleSmall = TextStyle(
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13.5f * s).sp,
            lineHeight = (18f * s).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.Normal,
            fontSize = (13.5f * s).sp,
            lineHeight = (19f * s).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.Normal,
            fontSize = (12.5f * s).sp,
            lineHeight = (17.5f * s).sp
        ),
        bodySmall = TextStyle(
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.Normal,
            fontSize = (11.5f * s).sp,
            lineHeight = (16f * s).sp
        ),
        labelLarge = TextStyle(
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13f * s).sp,
            lineHeight = (18f * s).sp
        ),
        labelMedium = TextStyle(
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.SemiBold,
            fontSize = (11.5f * s).sp,
            lineHeight = (15f * s).sp
        ),
        labelSmall = TextStyle(
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.Medium,
            fontSize = (10.5f * s).sp,
            lineHeight = (14f * s).sp
        )
    )
}

val Typography = buildAppTypography(0.90f)
