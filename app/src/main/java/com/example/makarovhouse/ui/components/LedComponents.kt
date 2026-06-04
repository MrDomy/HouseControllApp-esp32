package com.example.makarovhouse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.makarovhouse.ui.theme.BrandBlack
import com.example.makarovhouse.ui.theme.BrandGrayLight
import com.example.makarovhouse.ui.theme.BrandWhite
import com.example.makarovhouse.ui.theme.BrandGrayDark

@Composable
fun GroupButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(45.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandGrayDark)
    ) {
        Text(text, fontSize = 12.sp)
    }
}

@Composable
fun LedItemView(index: Int, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, BrandGrayLight, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val isLight = (color.red * 0.299 + color.green * 0.587 + color.blue * 0.114) > 0.5
        Text(
            text = "${index + 1}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isLight) BrandBlack else BrandWhite
        )
    }
}
