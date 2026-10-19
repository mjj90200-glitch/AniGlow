package com.aniglow.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniglow.app.ui.theme.FireflyGreen
import com.aniglow.app.ui.theme.WarmBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBackTopBar(title: String, onBack: () -> Unit, subtitle: String? = null) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Box(
                    modifier = Modifier.size(38.dp).background(FireflyGreen.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = Color(0xFF37414D), modifier = Modifier.size(20.dp))
                }
            }
        },
        title = {
            Column {
                Text(title, color = Color(0xFF242E3A), fontWeight = FontWeight.Black, fontSize = 18.sp)
                subtitle?.let { Text(it, color = Color(0xFF8F98A4), fontSize = 10.sp, fontWeight = FontWeight.Medium) }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmBackground.copy(alpha = 0.96f)),
    )
}
