package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// =========================================================================
// 🌟 动漫联名限定色彩体系 (Anime Collab Limited Edition Themes)
// =========================================================================

enum class AnimeSkinTheme(
    val id: String,
    val title: String,
    val subTitle: String,
    val seriesName: String,
    val badge: String,
    val quote: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val containerColor: Color,
    val bgGradientStart: Color,
    val bgGradientEnd: Color,
    val darkBg: Color
) {
    EVA_MECHA(
        id = "eva_mecha",
        title = "初号觉醒 · 终焉机甲",
        subTitle = "NEO-01 EVANGELION AWAKEN",
        seriesName = "新世纪机甲科幻联名",
        badge = "⚡ 同步率 400% 暴走",
        quote = "「勇敢的少年啊，快去创造奇迹！—— 职场全通关」",
        primaryColor = Color(0xFF7B2CBF),     // EVA 幻紫
        secondaryColor = Color(0xFF00F5D4),   // 觉醒荧光绿
        accentColor = Color(0xFFFF0055),      // 暴走赤红
        containerColor = Color(0xFFF3E8FF),
        bgGradientStart = Color(0xFF240046),
        bgGradientEnd = Color(0xFF7B2CBF),
        darkBg = Color(0xFF0C0714)
    ),
    SHONEN_FLAME(
        id = "shonen_flame",
        title = "炽热炎柱 · 破晓斩鬼",
        subTitle = "SHONEN NICHIRIN FLAME",
        seriesName = "热血王道少年漫联名",
        badge = "🔥 炎之呼吸 · 玖之型",
        quote = "「心を燃やせ！把心燃烧起来，斩获梦中名企 Offer！」",
        primaryColor = Color(0xFFE63946),     // 炎柱炽红
        secondaryColor = Color(0xFFFFB703),   // 日轮金黄
        accentColor = Color(0xFF2A9D8F),      // 碧蓝水刃
        containerColor = Color(0xFFFFE5E8),
        bgGradientStart = Color(0xFF9D0208),
        bgGradientEnd = Color(0xFFDC2F02),
        darkBg = Color(0xFF140708)
    ),
    SAKURA_STARLIGHT(
        id = "sakura_starlight",
        title = "星夜祈愿 · 魔法樱落",
        subTitle = "MAGICAL STARLIGHT SAKURA",
        seriesName = "奇幻治愈少女物语联名",
        badge = "🌸 纯爱契约 · 彗星之夜",
        quote = "「在跨越千万光年的星空下，与命中注定的搭档相遇。」",
        primaryColor = Color(0xFFFF5D8F),     // 魔法樱粉
        secondaryColor = Color(0xFF70D6FF),   // 彗星星蓝
        accentColor = Color(0xFF9D4EDD),      // 暮夜幽紫
        containerColor = Color(0xFFFFEEF3),
        bgGradientStart = Color(0xFFFF758F),
        bgGradientEnd = Color(0xFFFFB3C1),
        darkBg = Color(0xFF160914)
    ),
    CYBER_DIVA(
        id = "cyber_diva",
        title = "电波歌姬 · 未来极光",
        subTitle = "CYBER POP DIVA 01",
        seriesName = "虚拟偶像赛博次元联名",
        badge = "🎵 电子之海 · 音域全开",
        quote = "「用最高分贝的电波，唱响属于新一代年轻人的科创未来！」",
        primaryColor = Color(0xFF00E5FF),     // 初音极光绿蓝
        secondaryColor = Color(0xFFFF007F),   // 赛博电音粉
        accentColor = Color(0xFF7928CA),      // 脉冲电音紫
        containerColor = Color(0xFFE0F7FA),
        bgGradientStart = Color(0xFF00B4D8),
        bgGradientEnd = Color(0xFF0077B6),
        darkBg = Color(0xFF05131A)
    ),
    CLASSIC_STUDIO(
        id = "classic_studio",
        title = "极简商务 · 职场先锋",
        subTitle = "ORIGINAL CAREER PRO",
        seriesName = "官方标准版",
        badge = "💎 职场精英",
        quote = "「专注技能沉淀，连接全国高价值岗位与考证资源。」",
        primaryColor = Color(0xFF4361EE),
        secondaryColor = Color(0xFF3F37C9),
        accentColor = Color(0xFF4CC9F0),
        containerColor = Color(0xFFEEF2FF),
        bgGradientStart = Color(0xFF3A0CA3),
        bgGradientEnd = Color(0xFF4361EE),
        darkBg = Color(0xFF0F172A)
    )
}
