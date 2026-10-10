package com.example.ui.enterprise

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.CareerViewModel

/**
 * 🏢 企业详细信息编辑界面 (全屏弹窗编辑)
 * 支持编辑：企业简称、全称、行业类别、人员规模、融资阶段、企业口号、地址、福利待遇标签
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyDetailEditDialog(
    viewModel: CareerViewModel,
    onDismiss: () -> Unit
) {
    val currentProfile by viewModel.enterpriseProfile.collectAsState()

    var shortName by remember { mutableStateOf(currentProfile.shortName) }
    var companyName by remember { mutableStateOf(currentProfile.companyName) }
    var industry by remember { mutableStateOf(currentProfile.industry) }
    var scale by remember { mutableStateOf(currentProfile.scale) }
    var financeStage by remember { mutableStateOf(currentProfile.financeStage) }
    var address by remember { mutableStateOf(currentProfile.address) }
    var creditCode by remember { mutableStateOf(currentProfile.creditCode) }
    var slogan by remember { mutableStateOf(currentProfile.slogan) }
    var welfareTags by remember { mutableStateOf(currentProfile.welfareTags) }

    var newTagInput by remember { mutableStateOf("") }

    val presetScales = listOf("20-99人", "100-499人", "500-999人", "1000-9999人", "10000人以上")
    val presetStages = listOf("已上市", "未上市", "国企/央企", "A轮/B轮", "不需要融资")
    val recommendedWelfare = listOf("六险一金", "全额公积金", "双休弹性制", "技能津贴", "年终奖丰厚", "租房补贴", "免费三餐", "导师制", "股票期权")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "编辑企业详细信息",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_company_edit_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "取消返回")
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                viewModel.updateCompanyProfile(
                                    companyName = companyName,
                                    shortName = shortName,
                                    industry = industry,
                                    scale = scale,
                                    financeStage = financeStage,
                                    address = address,
                                    creditCode = creditCode,
                                    slogan = slogan,
                                    welfareTags = welfareTags
                                )
                                onDismiss()
                            },
                            enabled = shortName.isNotBlank() && companyName.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .testTag("save_company_edit_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("保存生效", fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("放弃修改")
                        }
                        Button(
                            onClick = {
                                viewModel.updateCompanyProfile(
                                    companyName = companyName,
                                    shortName = shortName,
                                    industry = industry,
                                    scale = scale,
                                    financeStage = financeStage,
                                    address = address,
                                    creditCode = creditCode,
                                    slogan = slogan,
                                    welfareTags = welfareTags
                                )
                                onDismiss()
                            },
                            enabled = shortName.isNotBlank() && companyName.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("保存修改", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
            ) {
                // 1. 实时企业卡片预览
                item {
                    Text(
                        text = "🏛️ 实时企业主页预览",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF0D47A1), Color(0xFF1976D2))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (shortName.isNotBlank()) shortName.take(2) else "企业",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (shortName.isNotBlank()) shortName else "企业简称",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "已核验",
                                            tint = Color(0xFF1976D2),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${if (industry.isNotBlank()) industry else "未分类行业"} · ${if (scale.isNotBlank()) scale else "规模未填"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (companyName.isNotBlank()) companyName else "企业法定全称",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (slogan.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "口号：$slogan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (welfareTags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    welfareTags.take(4).forEach { tag ->
                                        Surface(
                                            color = Color(0xFFE3F2FD),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = tag,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF1565C0),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. 企业名称与信用标识
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "🏷️ 企业品牌与工商资质",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = shortName,
                                onValueChange = { shortName = it },
                                label = { Text("企业简称 (如：星澜智科 / 极光云联)") },
                                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_company_shortname_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = companyName,
                                onValueChange = { companyName = it },
                                label = { Text("企业法定全称 (如：星澜数智科技 (深圳) 有限公司)") },
                                leadingIcon = { Icon(Icons.Default.Domain, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_company_fullname_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // 3. 行业类别与规模
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "🌐 行业类别与人员规模",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = industry,
                                onValueChange = { industry = it },
                                label = { Text("所属行业赛道 (如：互联网 / 高新软件 / 人工智能)") },
                                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // 规模预设快捷选择
                            Column {
                                Text(
                                    text = "人员规模：",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    presetScales.take(3).forEach { pScale ->
                                        FilterChip(
                                            selected = scale == pScale,
                                            onClick = { scale = pScale },
                                            label = { Text(pScale, fontSize = 11.sp) }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    presetScales.drop(3).forEach { pScale ->
                                        FilterChip(
                                            selected = scale == pScale,
                                            onClick = { scale = pScale },
                                            label = { Text(pScale, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            // 融资阶段预设快捷选择
                            Column {
                                Text(
                                    text = "融资发展阶段：",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    presetStages.take(3).forEach { pStage ->
                                        FilterChip(
                                            selected = financeStage == pStage,
                                            onClick = { financeStage = pStage },
                                            label = { Text(pStage, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. 企业宣传口号与办公地址
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "📍 品牌口号与总部地址",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = slogan,
                                onValueChange = { slogan = it },
                                label = { Text("企业愿景口号 / 招聘Slogan") },
                                leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("公司官方总部常驻地址") },
                                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 3,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // 5. 福利待遇标签库编辑
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "🎁 企业福利待遇标签",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "精准展示公司硬核福利，吸引高意向持证候选人投递简历。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // 当前已选择标签列表 (支持点击删除)
                            if (welfareTags.isNotEmpty()) {
                                Text(
                                    text = "已配置福利 (点击可移除)：",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    welfareTags.chunked(3).forEach { rowTags ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            rowTags.forEach { tag ->
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.clickable {
                                                        welfareTags = welfareTags.filter { it != tag }
                                                    }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = tag,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "删除标签",
                                                            modifier = Modifier.size(12.dp),
                                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 自定义输入标签
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newTagInput,
                                    onValueChange = { newTagInput = it },
                                    label = { Text("添加自定义福利待遇") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Button(
                                    onClick = {
                                        val trimmed = newTagInput.trim()
                                        if (trimmed.isNotBlank() && !welfareTags.contains(trimmed)) {
                                            welfareTags = welfareTags + trimmed
                                            newTagInput = ""
                                        }
                                    },
                                    enabled = newTagInput.isNotBlank(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("+ 添加")
                                }
                            }

                            // 推荐快捷添加
                            Text(
                                text = "快捷推荐：",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                recommendedWelfare.filter { !welfareTags.contains(it) }.chunked(3).forEach { rowTags ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowTags.forEach { tag ->
                                            SuggestionChip(
                                                onClick = {
                                                    if (!welfareTags.contains(tag)) {
                                                        welfareTags = welfareTags + tag
                                                    }
                                                },
                                                label = { Text("+ $tag", fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
