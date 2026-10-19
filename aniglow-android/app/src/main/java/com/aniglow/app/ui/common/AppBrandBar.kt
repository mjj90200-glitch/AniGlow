package com.aniglow.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniglow.app.data.model.AuthUser
import com.aniglow.app.ui.theme.FireflyBright
import com.aniglow.app.ui.theme.FireflyGreen
import com.aniglow.app.ui.theme.SakuraPink
import com.aniglow.app.ui.theme.SkyBlue
import com.aniglow.app.ui.theme.WarmBackground

@Composable
fun AppBrandBar(
    user: AuthUser?,
    onSearch: () -> Unit,
    onAccount: () -> Unit,
    onMenu: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WarmBackground.copy(alpha = 0.96f))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(46.dp)
                .shadow(14.dp, RoundedCornerShape(16.dp), ambientColor = FireflyBright.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(FireflyBright, FireflyGreen))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(25.dp))
        }
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Text(
                "萤火番舍",
                style = TextStyle(
                    brush = Brush.linearGradient(listOf(SkyBlue, FireflyGreen, SakuraPink)),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                ),
            )
            Text("ANIGLOW", color = Color(0xFF98A0AB), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
        }
        BrandRoundButton(Icons.Outlined.Search, "搜索", onSearch)
        Spacer(Modifier.size(7.dp))
        Surface(
            modifier = Modifier.height(42.dp).clickable(onClick = onAccount),
            shape = CircleShape,
            color = FireflyGreen,
            shadowElevation = 7.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (user == null) 13.dp else 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Outlined.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text(user?.visibleName?.take(4) ?: "登录", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.size(7.dp))
        BrandRoundButton(Icons.Outlined.Menu, "菜单", onMenu)
    }
}

@Composable
private fun BrandRoundButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(42.dp).clickable(onClick = onClick),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.82f),
        shadowElevation = 4.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = Color(0xFF667080), modifier = Modifier.size(21.dp))
        }
    }
}
