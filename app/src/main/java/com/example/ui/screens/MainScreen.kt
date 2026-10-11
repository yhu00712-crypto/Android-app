package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import com.example.data.model.*
import com.example.data.skills.CareerPlannerSkill
import com.example.data.skills.CareerPlanningReport
import com.example.data.model.UserRoleMode
import com.example.ui.components.DeepSeekApiDialog
import com.example.ui.enterprise.EnterpriseMainScreen
import com.example.ui.enterprise.RoleSelectionDialog
import com.example.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: CareerViewModel,
    modifier: Modifier = Modifier
) {
    val currentRoleMode by viewModel.userRoleMode.collectAsState()
    val showRoleSelectionDialog by viewModel.showRoleSelectionDialog.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        if (currentRoleMode == UserRoleMode.ENTERPRISE) {
            // 🏢 企业招聘端
            EnterpriseMainScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 👤 个人求职者端 (现有内容全部完整保留为个人账户)
            PersonalMainScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 🌟 新用户点开软件后弹出的企业与个人账户选择界面 (支持随时在界面切换)
        if (showRoleSelectionDialog) {
            RoleSelectionDialog(
                viewModel = viewModel,
                onDismissRequest = {
                    if (currentRoleMode != UserRoleMode.UNSELECTED) {
                        viewModel.showRoleSelectionDialog.value = false
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalMainScreen(
    viewModel: CareerViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val activeRecruiter by viewModel.activeRecruiter.collectAsState()

    BackHandler(enabled = (activeRecruiter != null || currentTab != 0)) {
        if (activeRecruiter != null) {
            viewModel.activeRecruiter.value = null
        } else if (currentTab != 0) {
            viewModel.currentTab.value = 0
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (activeRecruiter == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { viewModel.currentTab.value = 0 },
                        icon = { Icon(Icons.Default.Work, contentDescription = "招聘求职") },
                        label = { Text("招聘求职", fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { viewModel.currentTab.value = 1 },
                        icon = { Icon(Icons.Default.People, contentDescription = "搭伙圈") },
                        label = { Text("搭伙圈", fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { viewModel.currentTab.value = 2 },
                        icon = { 
                            BadgedBox(
                                badge = {
                                    val recruitersList by viewModel.recruiters.collectAsState()
                                    val totalUnread = recruitersList.count { it.hasUnread }
                                    if (totalUnread > 0) {
                                        Badge { Text(totalUnread.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Email, contentDescription = "求职信使")
                            }
                        },
                        label = { Text("求职信使", fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { viewModel.currentTab.value = 3 },
                        icon = { Icon(Icons.Default.Person, contentDescription = "个人主页") },
                        label = { Text("个人主页", fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
            ) {
                when (currentTab) {
                    0 -> JobRecruitmentTabScreen(viewModel)
                    1 -> CampusCoCreateTabScreen(viewModel)
                    2 -> ChatTabScreen(viewModel)
                    3 -> UserProfileTabScreen(viewModel)
                }

                // DeepSeek / AI 大模型 API 配置弹窗 (支持全局调出)
                val showApiConfigDialog by viewModel.showApiConfigDialog.collectAsState()
                if (showApiConfigDialog) {
                    DeepSeekApiDialog(viewModel = viewModel)
                }
            }
        }
    }
}

// =========================================================
// 🔍 TAB 0：双向证书检索主界面
// =========================================================
@Composable
fun SearchTabScreen(viewModel: CareerViewModel) {}


// --- 职业卡片Composables ---
@Composable
fun OccupationCard(
    occupation: Occupation,
    isExpanded: Boolean,
    relatedCerts: List<CertificateWithRelationInfo>,
    aiMatches: List<com.example.data.network.AiMatchedItem>? = null,
    isAiLoading: Boolean = false,
    onTriggerAi: () -> Unit = {},
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = occupation.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text(occupation.category, fontSize = 10.sp) },
                            modifier = Modifier.height(20.dp)
                        )
                        if (occupation.id.startsWith("OCC_0")) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "☁️ 后端数据库",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "预计起薪: ${occupation.salaryExpectation}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE65100)
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = occupation.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            // 展开部分：查询映射到的证书清单与官方背书
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏆 双向考证通关路径分析",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = onTriggerAi,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "重新推导",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 1. 优先展示：国家标准知识库核定的权威资质要求 (100% 真实调用数据库)
                    if (relatedCerts.isNotEmpty()) {
                        val mandatory = relatedCerts.filter { it.requirementType == "MANDATORY" }
                        val preferred = relatedCerts.filter { it.requirementType == "PREFERRED" }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🏛️", fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "官方核定考证资质 (国家数据库严格对齐)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                if (mandatory.isNotEmpty()) {
                                    Column {
                                        Text(
                                            text = "🔴 准入类证书 (法定硬性 · 无证不得执业):",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD32F2F)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "依法实行从业准入：关系国家安全与公众生命财产安全，无证不得执业。",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFFD32F2F).copy(alpha = 0.85f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    mandatory.forEach { rel ->
                                        CertRelationRow(rel.certificate.name, rel.benefitDescription)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (preferred.isNotEmpty()) {
                                    Column {
                                        Text(
                                            text = "🟡 水平评价类证书 (能力评价 · 供选聘与职称参考):",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0284C7)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "非强制执业准入：衡量专业技术水平，供用人单位聘用、定岗定薪与评定职称参考。",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF0284C7).copy(alpha = 0.85f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    preferred.forEach { rel ->
                                        CertRelationRow(rel.certificate.name, rel.benefitDescription)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // 2. 补充展示：Gemini AI 智能推导与云端政策深度解析
                    if (aiMatches != null && aiMatches.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("✨", fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "AI 深度智库评析 (政策条款与加分权重)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                aiMatches.forEach { aiItem ->
                                    AiRelationRow(
                                        title = aiItem.title,
                                        requirementType = aiItem.requirementType,
                                        score = aiItem.score,
                                        analysis = aiItem.policyAnalysis
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // 3. 如果正在加载中
                    if (isAiLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🔮 AI 智脑正在结合法规与知识库推导对口证书...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (relatedCerts.isEmpty() && aiMatches.isNullOrEmpty()) {
                        Text(
                            text = "点击上方刷新按钮，即可由 AI 智脑智能分析该岗位的考证红线与加分资质。",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CertRelationRow(certName: String, benefitDescription: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
    ) {
        Text(
            text = certName,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = benefitDescription,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
    }
}

// --- 证书卡片Composables ---
@Composable
fun CertificateCard(
    certificate: Certificate,
    isExpanded: Boolean,
    relatedOccs: List<OccupationWithRelationInfo>,
    aiMatches: List<com.example.data.network.AiMatchedItem>? = null,
    isAiLoading: Boolean = false,
    onTriggerAi: () -> Unit = {},
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = certificate.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val policyInfo = QualificationPolicyHelper.getCategoryInfo(certificate.name)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (policyInfo.isAccess) Color(0xFFFEE2E2) else Color(0xFFE0F2FE),
                        border = BorderStroke(0.6.dp, if (policyInfo.isAccess) Color(0xFFEF4444) else Color(0xFF38BDF8))
                    ) {
                        Text(
                            text = if (policyInfo.isAccess) "🚨 准入类：无证不得执业" else "💡 水平评价类：衡量技术水平，非强制执业",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (policyInfo.isAccess) Color(0xFFB91C1C) else Color(0xFF0369A1),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "发证机构: ${certificate.authority}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = "•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "考试频次: ${certificate.examFrequency}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "💡 证书简介: ${certificate.description}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val policyInfo = QualificationPolicyHelper.getCategoryInfo(certificate.name)
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (policyInfo.isAccess) Color(0xFFFEF2F2) else Color(0xFFF0F9FF),
                border = BorderStroke(0.6.dp, if (policyInfo.isAccess) Color(0xFFFCA5A5) else Color(0xFFBAE6FD)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (policyInfo.isAccess) "🚨【准入类说明】" else "💡【水平评价类说明】",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (policyInfo.isAccess) Color(0xFFB91C1C) else Color(0xFF0369A1)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = policyInfo.slogan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (policyInfo.isAccess) Color(0xFFDC2626) else Color(0xFF0284C7)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.5.dp))
                    Text(
                        text = policyInfo.explanation,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 展开部分：查询持本证书可以拓展的岗位和加分通路
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🚀 持本证可双向拓展的高薪求职跑道",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        IconButton(
                            onClick = onTriggerAi,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "重新反向推导",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 1. 优先展示：国家标准知识库核定的权威对口岗位 (100% 真实调用数据库)
                    if (relatedOccs.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🏛️", fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "官方核定对口执业岗位 (国家数据库严格对齐)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                relatedOccs.forEach { rel ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surface,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = rel.occupation.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (rel.requirementType == "MANDATORY") "🔴 准入类 (无证不得执业)" else "🟡 水平评价类 (衡量技术水平)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (rel.requirementType == "MANDATORY") Color(0xFFD32F2F) else Color(0xFF0284C7)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = if (rel.requirementType == "MANDATORY") "【准入类规定】依法实行准入控制，无证不得执业。"
                                                   else "【水平评价类说明】衡量专业技术水平与实操能力，非强制执业准入，供用人单位聘任及定级参考。",
                                            fontSize = 10.5.sp,
                                            color = if (rel.requirementType == "MANDATORY") Color(0xFFDC2626).copy(alpha = 0.85f) else Color(0xFF0284C7).copy(alpha = 0.85f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = rel.benefitDescription,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // 2. 补充展示：Gemini AI 智能反向拓宽与云端分析
                    if (aiMatches != null && aiMatches.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "✨ AI 智脑反向拓宽就业方向 (政策条款与加分权重)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                aiMatches.forEach { aiItem ->
                                    AiRelationRow(
                                        title = aiItem.title,
                                        requirementType = aiItem.requirementType,
                                        score = aiItem.score,
                                        analysis = aiItem.policyAnalysis
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // 3. 如果正在加载中
                    if (isAiLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🔮 AI 智脑正在极速反向拓宽对口岗位...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else if (relatedOccs.isEmpty() && aiMatches.isNullOrEmpty()) {
                        Text(
                            text = "点击上方刷新按钮，即可由 AI 智脑智能反向推导该证书可以拓展的高薪职业跑道。",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiRelationRow(
    title: String,
    requirementType: String,
    score: Int,
    analysis: String
) {
    val isMandatory = requirementType == "MANDATORY"
    val badgeColor = if (isMandatory) Color(0xFFD32F2F) else Color(0xFF0284C7)
    val badgeText = if (isMandatory) "🚨 准入类 (无证不得执业)" else "💡 水平评价类 (衡量技术水平)"
    val policyNotice = if (isMandatory) "依法实行从业准入：关系公共安全与公众利益，无证不得执业。"
                       else "非强制执业准入：衡量专业技术水平，供用人单位聘用、定岗定薪与评定职称参考。"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
            .border(0.5.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$score%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeColor
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = policyNotice,
            fontSize = 10.5.sp,
            color = badgeColor.copy(alpha = 0.85f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = analysis,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
    }
}

// =========================================================
// 💬 TAB 1：求职信使 (含已读未读状态，去除冗余看板)
// =========================================================
@Composable
fun ChatTabScreen(viewModel: CareerViewModel) {
    val activeRecruiter by viewModel.activeRecruiter.collectAsState()

    AnimatedContent(
        targetState = activeRecruiter,
        transitionSpec = {
            slideInHorizontally { width -> if (targetState != null) width else -width } + fadeIn() togetherWith
            slideOutHorizontally { width -> if (targetState != null) -width else width } + fadeOut()
        },
        label = "ChatScreenTransition"
    ) { recruiter ->
        if (recruiter == null) {
            RecruitersListScreen(viewModel)
        } else {
            ActiveChatRoomScreen(viewModel, recruiter)
        }
    }
}

@Composable
fun RecruitersListScreen(viewModel: CareerViewModel) {
    val recruitersList by viewModel.recruiters.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor, primaryColor.copy(alpha = 0.8f)),
                            startY = 0f,
                            endY = size.height
                        )
                    )
                }
                .padding(vertical = 24.dp, horizontal = 20.dp)
        ) {
            Column {
                Text(
                    text = "求职顾问与企业信使",
                    fontSize = 22.sp,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "在此直接与企业HR、面试官模拟对话 · 已读未读状态一目了然",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            val aiMentors = recruitersList.filter { it.id == "AGENT_RESUME_POLISH" || it.id == "TUTOR_AI" }
            val hrList = recruitersList.filter { it.id != "AGENT_RESUME_POLISH" && it.id != "TUTOR_AI" }

            if (aiMentors.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨ AI 导师与智能工坊 (置顶)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FilledTonalButton(
                            onClick = { viewModel.openApiConfigDialog() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DeepSeek API", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(aiMentors) { recruiter ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.startChatWith(recruiter) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 头像
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color(recruiter.avatarColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (recruiter.id == "AGENT_RESUME_POLISH") "🔮" else "🧭",
                                    fontSize = 24.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = recruiter.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "📌 置顶",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = recruiter.lastMessageTime,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = recruiter.company,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = recruiter.lastMessage,
                                    fontSize = 13.sp,
                                    color = if (recruiter.hasUnread) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (recruiter.hasUnread) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            // 已读未读红点标记
                            if (recruiter.hasUnread) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏢 名企官方 HR & 面试官直聊",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "共 ${hrList.size} 位名企招聘负责人",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(hrList) { recruiter ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.startChatWith(recruiter) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 头像
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(recruiter.avatarColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = recruiter.name.take(1),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = recruiter.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = recruiter.lastMessageTime,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = recruiter.company,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = recruiter.lastMessage,
                                fontSize = 13.sp,
                                color = if (recruiter.hasUnread) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (recruiter.hasUnread) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        // 已读未读红点标记
                        if (recruiter.hasUnread) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveChatRoomScreen(viewModel: CareerViewModel, recruiter: Recruiter) {
    val messages by viewModel.currentChatMessages.collectAsState()
    var inputMessageText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
    ) {
        // 顶部对话框 Header
        TopAppBar(
            title = {
                Column {
                    Text(recruiter.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(recruiter.company, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.closeChat() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                if (recruiter.id == "TUTOR_AI" || recruiter.id == "AGENT_RESUME_POLISH") {
                    FilledTonalButton(
                        onClick = { viewModel.openApiConfigDialog() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("API配置", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        )

        // 若为置顶 AI 导师，显示 DeepSeek 引擎接入状态横幅
        if (recruiter.id == "TUTOR_AI" || recruiter.id == "AGENT_RESUME_POLISH") {
            val deepSeekKey by viewModel.deepSeekApiKey.collectAsState()
            val deepSeekModelName by viewModel.deepSeekModel.collectAsState()
            val provider by viewModel.aiProvider.collectAsState()

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openApiConfigDialog() },
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🤖", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "DeepSeek 驱动 ($deepSeekModelName)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (deepSeekKey.isNotBlank()) Color(0xFF2E7D32) else Color(0xFFE65100)
                            ) {
                                Text(
                                    text = if (deepSeekKey.isNotBlank()) "已连通" else "点此配置Key",
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = if (deepSeekKey.isNotBlank()) "点击调整模型或修改 API 密钥" else "未配置私有密钥（点击快速添加 DeepSeek API Key）",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "配置 >",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 聊天泡泡区域
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->
                ChatBubble(msg = msg)
            }
        }

        // 底部快捷回复栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val suggestions = when (recruiter.id) {
                "HR_BYTEDANCE" -> listOf("我有法考A证，何时可以面试？", "岗位需要考什么证书？")
                "TECH_HUAWEI" -> listOf("我有软考中级，请问怎么匹配？", "技术岗位需要哪些证书要求？")
                "TUTOR_AI" -> listOf("我该怎么规划我的大学考证树？", "帮我分析低门槛高成长岗位")
                "AGENT_RESUME_POLISH" -> listOf("在比赛中主要负责做PPT和写策划案", "在学生会负责拉赞助和跑腿联系商家", "社团活动中主要负责组织和签到")
                else -> listOf("需要哪些法定红线证书？", "起薪待遇如何？")
            }
            suggestions.forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { viewModel.sendMessage(suggestion) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(suggestion, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 底部输入栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputMessageText,
                onValueChange = { inputMessageText = it },
                placeholder = { Text(if (recruiter.id == "TUTOR_AI" || recruiter.id == "AGENT_RESUME_POLISH") "向 AI 专家提问或粘贴经历..." else "发送消息给面试官...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    val text = inputMessageText.trim()
                    if (text.isNotEmpty()) {
                        inputMessageText = ""
                        viewModel.sendMessage(text)
                        focusManager.clearFocus()
                    }
                })
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    val text = inputMessageText.trim()
                    if (text.isNotEmpty()) {
                        inputMessageText = ""
                        viewModel.sendMessage(text)
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送", tint = Color.White)
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.sender == "USER"
    val planningReport = remember(msg.content) {
        if (!isUser) CareerPlannerSkill.parseReport(msg.content) else null
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(if (planningReport != null) 0.95f else 0.85f)
        ) {
            if (planningReport != null) {
                CareerPlanReportBubble(report = planningReport)
            } else {
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isUser) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 2.dp,
                                bottomEnd = if (isUser) 2.dp else 16.dp
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = msg.content,
                        fontSize = 14.sp,
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = msg.timestamp,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                // 已读未读标记
                if (isUser) {
                    Text(
                        text = if (msg.isRead) "已读" else "送达",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (msg.isRead) Color(0xFF0288D1) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                } else {
                    Text(
                        text = "已读",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun CareerPlanReportBubble(report: CareerPlanningReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. 顶部 Header 与匹配度得分徽章
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧭", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "定制职业规划方案",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (report.matchScore >= 80) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    border = BorderStroke(
                        1.dp,
                        if (report.matchScore >= 80) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                    )
                ) {
                    Text(
                        text = "综合匹配度: ${report.matchScore}分",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (report.matchScore >= 80) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // 2. 总体一句话评估
            if (report.careerSummary.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                        Text("💡", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = report.careerSummary,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 3. 核心优势与潜在差距
            if (report.advantages.isNotEmpty() || report.gaps.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (report.advantages.isNotEmpty()) {
                        Text("🟢 核心优势亮点", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        report.advantages.forEach { adv ->
                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(start = 4.dp)) {
                                Text("✓", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(adv, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                            }
                        }
                    }

                    if (report.gaps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("⚠️ 劣势与短板突破点", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        report.gaps.forEach { gap ->
                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(start = 4.dp)) {
                                Text("!", fontSize = 11.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(gap, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }

            // 4. 短期与中期成长路线图
            if (report.shortTermRoadmap != null || report.midTermRoadmap != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📅 阶段推进路线图", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    report.shortTermRoadmap?.let { st ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("🚀 ${st.timeline}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                st.actionItems.forEach { action ->
                                    Text("• $action", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp, start = 4.dp))
                                }
                            }
                        }
                    }

                    report.midTermRoadmap?.let { mt ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("📈 ${mt.timeline}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                mt.actionItems.forEach { action ->
                                    Text("• $action", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp, start = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 5. 建议考取的重点证书
            if (report.recommendedCertificates.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("📜 重点推荐考证清单", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    report.recommendedCertificates.forEach { cert ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cert.certName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(cert.reason, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (cert.priority.uppercase()) {
                                        "HIGH" -> Color(0xFFFFEBEE)
                                        "MEDIUM" -> Color(0xFFFFF3E0)
                                        else -> Color(0xFFF5F5F5)
                                    }
                                ) {
                                    Text(
                                        text = when (cert.priority.uppercase()) {
                                            "HIGH" -> "极高推荐"
                                            "MEDIUM" -> "建议考取"
                                            else -> "备选参考"
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (cert.priority.uppercase()) {
                                            "HIGH" -> Color(0xFFC62828)
                                            "MEDIUM" -> Color(0xFFEF6C00)
                                            else -> Color(0xFF616161)
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

// =========================================================
// 🌳 TAB 3：个人主页 (User Profile & Career Goal Portfolio)
// =========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileTabScreen(viewModel: CareerViewModel) {
    val selectedOccupationId by viewModel.selectedTreeOccupationId.collectAsState()
    val occupation by viewModel.selectedTreeOccupation.collectAsState()
    val treeCerts by viewModel.treeRequiredCerts.collectAsState()
    val coCreatePostsList by viewModel.coCreatePosts.collectAsState()
    val isProfileCompleted by viewModel.isProfileCompleted.collectAsState()
    val userMbti by viewModel.userMbti.collectAsState()
    var showMbtiDialog by remember { mutableStateOf(false) }
    
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    // 个人资料相关响应式状态
    val userNickname by viewModel.userNickname.collectAsState()
    val userRealName by viewModel.userRealName.collectAsState()
    val userPhone by viewModel.userPhone.collectAsState()
    val userSchool by viewModel.userSchool.collectAsState()
    val userGrade by viewModel.userGrade.collectAsState()
    val userMajor by viewModel.userMajor.collectAsState()
    val resumeTargetJob by viewModel.resumeTargetJob.collectAsState()
    val resumeExpectCity by viewModel.resumeExpectCity.collectAsState()
    val resumeSkillTags by viewModel.resumeSkillTags.collectAsState()
    val resumeCertifications by viewModel.resumeCertifications.collectAsState()
    
    var showEditDetailsScreen by remember { mutableStateOf(false) }
    var showResumeOptimizationScreen by remember { mutableStateOf(false) }
    val showOnlineResumeScreen by viewModel.showOnlineResumeScreen.collectAsState()
    
    // 过滤出用户已经“加入”或“预约”的帖子
    val joinedPosts = coCreatePostsList.filter { it.isJoined }
    
    var showOccDropdown by remember { mutableStateOf(false) }
    val allOccupations = remember { com.example.data.database.StaticDataStore.getOccupations().take(20) } // 供快速设定职业意向

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
        // 1. 顶部个人简历名片渐变横幅
        val primaryColor = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor, primaryColor.copy(alpha = 0.8f)),
                            startY = 0f,
                            endY = size.height
                        )
                    )
                }
                .padding(top = 28.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 头像，带亮白色圆环，点击可编辑资料
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .border(3.dp, Color.White, CircleShape)
                            .background(Color(0xFF4CC9F0), CircleShape)
                            .clickable { showEditDetailsScreen = true }
                            .testTag("avatar_profile_click"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (userNickname.isBlank() || userNickname == "待填写") "待" else userNickname.firstOrNull()?.toString() ?: "待",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showEditDetailsScreen = true }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userNickname,
                                fontSize = 20.sp,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isProfileCompleted) "🥇 高校科创先锋" else "🌱 新用户 (待完善)",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (userMbti != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE07A5F))
                                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "🔮 $userMbti",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (userSchool == "待填写" && userMajor == "待填写") "教育背景：待填写 (点击完善)" else "$userSchool · $userMajor$userGrade",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = if (userPhone == "待填写") "手机号: 待填写" else "手机号: $userPhone",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                // 名片状态一键完善卡片（点击后直接跳转至详细资料编辑页，具有极强实用价值）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isProfileCompleted) Color.White.copy(alpha = 0.15f) else Color(0xFFFFB703).copy(alpha = 0.25f))
                        .border(1.dp, if (isProfileCompleted) Color.White.copy(alpha = 0.3f) else Color(0xFFFFB703).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { showEditDetailsScreen = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isProfileCompleted) Color(0xFF4CAF50) else Color(0xFFFF3D00))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isProfileCompleted) "名片状态：✨ 资质饱满，才华全亮 (100%)" else "名片状态：⚠️ 个人信息待填写 (去完善 0%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = if (isProfileCompleted) "管理档案 ⚙️" else "完善资料 ✍️",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isProfileCompleted) Color.White.copy(alpha = 0.8f) else Color(0xFFFFF9C4)
                    )
                }
            }
        }

        // 2. 核心数据资产看板 (Stats Grid)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val totalCertsCount = treeCerts.count { it.requirementType == "MANDATORY" || it.requirementType == "PREFERRED" }
            ProfileStatCard(
                value = if (totalCertsCount > 0) "2/${totalCertsCount}" else "2/3",
                label = "点亮证书",
                modifier = Modifier.weight(1f).testTag("stat_certs")
            )
            ProfileStatCard(
                value = joinedPosts.size.toString(),
                label = "已入搭伙",
                modifier = Modifier.weight(1f).testTag("stat_teams")
            )
            ProfileStatCard(
                value = "1",
                label = "信使聊友",
                modifier = Modifier.weight(1f).testTag("stat_chats")
            )
            ProfileStatCard(
                value = if (isProfileCompleted) "95%" else "0%",
                label = "简历饱满度",
                modifier = Modifier
                    .weight(1f)
                    .clickable { showResumeOptimizationScreen = true }
                    .testTag("stat_score")
            )
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // 🎨 动漫 IP 皮肤与日夜间模式装扮中心卡片 (置于点亮证书、已入搭伙统计看板下方)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                 Column(modifier = Modifier.padding(14.dp)) {
                    // DeepSeek / AI 大模型 API 配置项
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openApiConfigDialog() }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🤖", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                val deepSeekKey by viewModel.deepSeekApiKey.collectAsState()
                                val deepSeekModelName by viewModel.deepSeekModel.collectAsState()
                                Text(
                                    text = "DeepSeek AI 接口配置 (API Key)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (deepSeekKey.isNotBlank()) "已配置私有密钥 · 当前模型: $deepSeekModelName" else "未配置私有密钥 · 点击添加新 API",
                                    fontSize = 10.sp,
                                    color = if (deepSeekKey.isNotBlank()) MaterialTheme.colorScheme.primary else Color(0xFFD9534F)
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = { viewModel.openApiConfigDialog() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("配置", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleDarkMode(!isDarkMode) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isDarkMode) "🌙" else "☀️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isDarkMode) "夜间模式 (深色护眼)" else "日间模式 (明亮清晰)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isDarkMode) "高反差极夜护眼" else "纯白底色与深黑字，清晰明亮",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode(it) },
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }
                }
            }
        }

        // 🏢 身份切换卡片：切换至企业招聘端 (我要招人)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { viewModel.selectRole(UserRoleMode.ENTERPRISE, rememberChoice = true) }
                    .testTag("switch_to_enterprise_profile_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1565C0).copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.2.dp, Color(0xFF1565C0).copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1565C0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "切换至「企业招聘端」",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D47A1)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF1565C0).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "我要招人",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF1565C0),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "发布在招职位 · 检索国家法定持证人才 · AI初筛 · 邀约面试",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "切换",
                        tint = Color(0xFF1565C0)
                    )
                }
            }
        }

        // 📋 个人在线标准简历卡片 (放在 MBTI 职场先锋认证 正上方)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { viewModel.navigateToOnlineResume(editMode = false) }
                    .testTag("online_resume_entry_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.2.dp, Color(0xFF4361EE).copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color(0xFF4361EE), Color(0xFF3F37C9))
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📄", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "我的标准在线简历",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "全功能履历中心 · 名企直聘高通关模板",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 状态标签与进入箭头
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2E7D32))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "随时到岗",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = "进入简历",
                                tint = Color(0xFF4361EE),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // 简历核心提炼信息行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("意向岗位", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = occupation?.name ?: "全栈开发工程师",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("教育背景", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (userSchool == "待填写" && userMajor == "待填写") "待填写" else "$userSchool · $userMajor",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("权威资质", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (resumeCertifications.isEmpty()) "待填写 (暂无)" else resumeCertifications.take(2).joinToString(" / ") { it.take(6) },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 点击查看完整履历、在线编辑保存或一键投递直聘",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "查看完整简历 ➔",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF4361EE)
                        )
                    }
                }
            }
        }

        // 🔮 MBTI 职场先锋认证板块
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (userMbti == null) Color(0xFF6C63FF).copy(alpha = 0.06f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (userMbti == null) Color(0xFF6C63FF).copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = if (userMbti == null) Color(0xFF6C63FF).copy(alpha = 0.15f) else Color(0xFFE07A5F).copy(alpha = 0.15f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (userMbti == null) Color(0xFF6C63FF) else Color(0xFFE07A5F),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "🔮 MBTI 职场先锋认证",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        TextButton(
                            onClick = { showMbtiDialog = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp).testTag("certify_mbti_btn")
                        ) {
                            Text(
                                text = if (userMbti == null) "去认证 ➔" else "重新测试 ⚙️",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (userMbti == null) Color(0xFF6C63FF) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (userMbti == null) {
                        Text(
                            text = "零基础找不到方向？MBTI不只是算命！一键选择您的16型人格，智能解锁由搭伙圈 AI 实验室推演的【专属软硬实力配比】与【绝配行业证书路径】，让你的性格优势变成简历里的绝对加分战绩！",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val mbtiDetails = getMbtiDetails(userMbti!!)
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFE07A5F).copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${mbtiDetails.code} · ${mbtiDetails.title}",
                                        color = Color(0xFFE07A5F),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "💡 职场本命优势: ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = mbtiDetails.strengths,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "🎯 绝配推荐证书路线: ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = mbtiDetails.recommendedCerts,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (showMbtiDialog) {
            MbtiCertificationDialog(
                onDismiss = { showMbtiDialog = false },
                onConfirm = { mbtiCode ->
                    viewModel.userMbti.value = mbtiCode
                    showMbtiDialog = false
                }
            )
        }

        // 📄 AI 智能简历优化专区 (AI Resume Optimizer Banner Entry)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showResumeOptimizationScreen = true }
                    .testTag("resume_optimizer_banner_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4361EE).copy(alpha = 0.05f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFF4361EE).copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF4361EE).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFF4361EE),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📄 智能科创简历精修与诊断",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "提取当前已通关学籍与合规证书，一键打磨大厂含金量核心词，饱满度拉满！",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "去精修 ➔",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4361EE)
                    )
                }
            }
        }

        // 3. 🎯 我的目标职业与备考路径 (打通职业树数据)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🎯 生涯奋斗目标",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // 更换目标职业按钮
                TextButton(
                    onClick = { showOccDropdown = true },
                    modifier = Modifier.testTag("change_target_btn")
                ) {
                    Text("设定意向 ▾", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4361EE))
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = occupation?.name ?: "法务助理/合规专员",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "所属行业领域: ${occupation?.category ?: "专业服务"} · 规划通关路径中",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = showOccDropdown,
                    onDismissRequest = { showOccDropdown = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .heightIn(max = 280.dp)
                ) {
                    allOccupations.forEach { occ ->
                        DropdownMenuItem(
                            text = { Text("${occ.name} (${occ.category})", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            onClick = {
                                viewModel.selectedTreeOccupationId.value = occ.id
                                showOccDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 展示对应的备考/持证清单
            Text(
                text = "🔑 该意向职业的通关持证路径:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 基础筑基 (已点亮)
            ProfileCertNode(
                title = "大学英语六级 (CET-6)",
                desc = "普适语言实力背书，大厂网申、涉外商务、跨境运营基本准入资质。",
                status = "已点亮",
                isCompleted = true
            )

            // 专业通关证书列表
            if (treeCerts.isEmpty()) {
                ProfileCertNode(
                    title = "暂无特定专业持证红线",
                    desc = "该岗位更偏向于在搭伙圈打高水平项目、展示代码和解决具体业务问题的实力背书。",
                    status = "无门槛",
                    isCompleted = false
                )
            } else {
                treeCerts.forEachIndexed { index, certInfo ->
                    // 模拟前一两个证书为已考过，增加视觉饱满度
                    ProfileCertNode(
                        title = certInfo.certificate.name,
                        desc = certInfo.benefitDescription,
                        status = if (index == 0) "已点亮" else "备考中",
                        isCompleted = index == 0
                    )
                }
            }
        }

        // 4. 👥 我的「搭伙」动态 (Teaming Status)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(
                text = "👥 我的「搭伙圈」项目进度",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (joinedPosts.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "当前未加入任何搭伙项目或咨询预约",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { viewModel.currentTab.value = 2 }, // 跳转到搭伙圈
                            modifier = Modifier.testTag("jump_to_cocreate_btn")
                        ) {
                            Text("去「搭伙圈」看看好玩的项目 ➔", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4361EE))
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    joinedPosts.forEach { post ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (post.type == "比赛组队") Color(0xFFE8F5E9) else Color(0xFFFCE4EC))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(post.type, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (post.type == "比赛组队") Color(0xFF2E7D32) else Color(0xFFC2185B))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = post.targetPlatform,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = post.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "发起人: ${post.authorName}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                OutlinedButton(
                                    onClick = { viewModel.toggleJoinPost(post.id) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f)),
                                    modifier = Modifier.testTag("leave_post_${post.id}")
                                ) {
                                    Text("退出", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4.5 🔒 全程防诈与隐私隔离盾 (Global Anti-Fraud & Privacy Shield)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "🛡️ 隐私与反诈隔离保护",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth().testTag("anti_fraud_shield_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)), // 温暖的警示黄/橙色背景
                border = BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = "Security Shield",
                            tint = Color(0xFFD39E00),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "安全隔离：外部联系方式全程不可见",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF856404)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "本软件实行极其严格的防诈隔离机制。微信、QQ、手机号等外部社交账号在平台全程不予绑定、展示或开放可见。无论是发帖、组队还是成功申请搭伙，外部联系方式均100%不可见，从源头上斩断电信诈骗黑手。",
                        fontSize = 11.sp,
                        color = Color(0xFF856404).copy(alpha = 0.95f),
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "💡 安全沟通渠道：请全程使用平台内置「求职信使」进行沟通。严禁脱离平台在外部进行资金交易或交付验证码！",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF721C24),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // 5. ✍️ 我的求职首选及技能背书 (Skills portfolio)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "⚡ 科创技能与偏好背书",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // 意向地区
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("意向求职/实习地区:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(resumeExpectCity, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // 核心优势
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("团队定位角色:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(resumeTargetJob, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text("核心擅长技术栈标签:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))

                    if (resumeSkillTags.isEmpty()) {
                        Text("待填写 (可在个人在线标准简历中快速配置)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            resumeSkillTags.forEach { skill ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(skill, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. ⚙️ 账户与身份设置
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { viewModel.switchRole() }
                    .testTag("switch_to_enterprise_role_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1565C0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "切换至「企业招聘端」",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D47A1)
                        )
                        Text(
                            text = "发布在招职位 · 检索持证人才库 · 邀约面试",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "切换",
                        tint = Color(0xFF1565C0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 重新选择账户类型
            OutlinedButton(
                onClick = { viewModel.openRoleSelection() },
                modifier = Modifier.fillMaxWidth().testTag("reopen_role_selection_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("重新选择账户类型 (身份选择弹窗)", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 退出登录 / 重置为首次登录初始状态 (所有个人信息设为待填写)
            var showResetConfirmDialog by remember { mutableStateOf(false) }
            val context = LocalContext.current

            OutlinedButton(
                onClick = { showResetConfirmDialog = true },
                modifier = Modifier.fillMaxWidth().testTag("reset_profile_btn"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("重置个人信息为首次登录状态 (全量设为待填写)", fontWeight = FontWeight.SemiBold)
            }

            if (showResetConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showResetConfirmDialog = false },
                    title = { Text("重置为首次登录状态", fontWeight = FontWeight.Bold) },
                    text = { Text("确定要清空当前所有填写的个人信息并恢复为首次登录状态吗？个人姓名、就读高校、专业、联系方式及简历信息将全量重置为「待填写」。") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.resetProfileToDefault()
                                showResetConfirmDialog = false
                                Toast.makeText(context, "已重置为首次登录状态 (个人信息均已设为待填写)", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("确定重置", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetConfirmDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        }

        // 📄 个人在线标准简历全屏展示与编辑器
        AnimatedVisibility(
            visible = showOnlineResumeScreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            OnlineResumeScreen(
                viewModel = viewModel,
                onBack = {
                    viewModel.showOnlineResumeScreen.value = false
                    viewModel.resumeStartInEditMode.value = false
                }
            )
        }

        // 极其流畅的 Material 3 抽屉滑出式全屏编辑器
        AnimatedVisibility(
            visible = showEditDetailsScreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            UserDetailedProfileScreen(
                viewModel = viewModel,
                onBack = { showEditDetailsScreen = false }
            )
        }

        // AI 智能简历精修与诊断全屏抽屉
        AnimatedVisibility(
            visible = showResumeOptimizationScreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            ResumeOptimizationScreen(
                viewModel = viewModel,
                onBack = { showResumeOptimizationScreen = false }
            )
        }
    }
}

@Composable
fun ProfileStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ProfileCertNode(
    title: String,
    desc: String,
    status: String,
    isCompleted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(if (isCompleted) Color(0xFF4CAF50) else Color(0xFFFF9800))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isCompleted) Color(0xFFE8F5E9) else Color(0xFFFFF3E0))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = status,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
    }
}

// =========================================================
// 📭 空白提示页面
// =========================================================
@Composable
fun EmptyStateView(tip: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = tip,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

// =========================================================
// 🤖 职业未检索到时的 AI 结构化输出组件 (严格按照固定 JSON 规范)
// =========================================================
@Composable
fun AiNotFoundOutputView(
    searchQuery: String,
    searchMode: Int,
    isLoading: Boolean,
    result: com.example.data.network.AiSearchOutput?,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 📋 固定 JSON 格式输出, 1: 🎴 智能卡片视图

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 顶部状态横幅
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, if (searchMode == 0) Color(0xFF6C63FF).copy(alpha = 0.35f) else Color(0xFF009688).copy(alpha = 0.35f)),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (searchMode == 0) Color(0xFF6C63FF).copy(alpha = 0.15f) else Color(0xFF009688).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (searchMode == 0) "🎯 配证书 · AI规划" else "🔍 找职业 · AI拓维",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (searchMode == 0) Color(0xFF6C63FF) else Color(0xFF009688),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI 智脑推导",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    FilledTonalButton(
                        onClick = onRetry,
                        enabled = !isLoading,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (isLoading) "推导中..." else "🔄 重新生成",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "系统在现有数据库中未检索到职业「$searchQuery」，已自动调用大模型针对该职业进行专业深度推导，并按照固定 JSON 格式规范输出结构化匹配方案。",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        // 2. 加载中状态
        if (isLoading) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = if (searchMode == 0) Color(0xFF6C63FF) else Color(0xFF009688),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "正在调用 AI 大模型解析「$searchQuery」...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "严格遵循固定 JSON 数据规范输出中，请稍候...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (result != null) {
            // 3. Tab 切换栏：固定 JSON 格式 vs 智能卡片视图
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedTab = 0 }
                ) {
                    Text(
                        text = "📋 固定的 JSON 格式输出",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedTab = 1 }
                ) {
                    Text(
                        text = "🎴 智能卡片视图 (${result.items.size})",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // 4. Tab 0: 原始固定 JSON 格式展示与复制
            if (selectedTab == 0) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E1E2E), // 深色代码编辑器风格
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = Color(0xFFFF5F56), modifier = Modifier.size(10.dp)) {}
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(shape = CircleShape, color = Color(0xFFFFBD2E), modifier = Modifier.size(10.dp)) {}
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(shape = CircleShape, color = Color(0xFF27C93F), modifier = Modifier.size(10.dp)) {}
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "output.json",
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }

                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(result.jsonString))
                                    Toast.makeText(context, "已成功复制固定格式 JSON 到剪切板！", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF313244),
                                    contentColor = Color(0xFFA6E3A1)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("📋 一键复制 JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = result.jsonString,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFFA6E3A1)
                            )
                        }
                    }
                }
            } else {
                // 5. Tab 1: 智能卡片视图
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    result.items.forEachIndexed { index, item ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                val isMandatory = item.requirementType == "MANDATORY"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.title,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isMandatory) Color(0xFFFEE2E2) else Color(0xFFE0F2FE)
                                    ) {
                                        Text(
                                            text = if (isMandatory) "🚨 准入类 · 无证不得执业" else "💡 水平评价类 · 衡量技术水平",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMandatory) Color(0xFFDC2626) else Color(0xFF0284C7),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (isMandatory) "【准入类规定】国家依法依规实行从业准入，无证不得执业。"
                                           else "【水平评价类规定】用于评价专业技能素养，供招聘定岗定薪与职称评定参考，非强制执业准入。",
                                    fontSize = 10.5.sp,
                                    color = if (isMandatory) Color(0xFFDC2626).copy(alpha = 0.85f) else Color(0xFF0284C7).copy(alpha = 0.85f)
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFFF3CD)
                                    ) {
                                        Text(
                                            text = "匹配度 ${item.score}%",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF856404),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (searchMode == 0) "对应证书资质代码规范" else "对口岗位核心竞争力",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(10.dp)) {
                                        Text("📜 ", fontSize = 12.sp)
                                        Text(
                                            text = item.policyAnalysis,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // 6. 空结果但未开始推导提示
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "未检索到与「$searchQuery」匹配的记录",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "点击下方按钮立即调用 AI 专家按照固定 JSON 格式为您精准推导匹配",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text("🤖 立即调用 AI 推导并输出 JSON", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =========================================================
// 👥 TAB 1：大学生生涯互助 (组队与咨询)
// =========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusCoCreateTabScreen(viewModel: CareerViewModel) {
    val posts by viewModel.filteredCoCreatePosts.collectAsState()
    val selectedType by viewModel.selectedCoCreateType.collectAsState()
    val query by viewModel.coCreateSearchQuery.collectAsState()
    val isProfileCompleted by viewModel.isProfileCompleted.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showIncompleteDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 顶部渐变标题横幅
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.8f)),
                                    startY = 0f,
                                    endY = size.height
                                )
                            )
                        }
                        .padding(vertical = 24.dp, horizontal = 20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "高校搭伙科创圈",
                                fontSize = 22.sp,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF007200)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.testTag("create_post_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("发帖招募", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "打破学院壁垒：寻找跨学科比赛队友，获取真实在岗考证与实习内推咨询",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // 搜索过滤框与分类 Chips (可折叠顶栏一部分)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.coCreateSearchQuery.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cocreate_search_input"),
                        placeholder = { Text("搜索比赛、相关证书、学院或技能标签...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.coCreateSearchQuery.value = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "清除")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007200),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("全部", "比赛组队", "实习咨询").forEach { type ->
                            val isSelected = selectedType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectedCoCreateType.value = type },
                                label = { Text(type, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE8F5E9),
                                    selectedLabelColor = Color(0xFF007200)
                                ),
                                modifier = Modifier.testTag("filter_chip_${type}")
                            )
                        }
                    }
                }
            }

            // 列表展示
            if (posts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        EmptyStateView("未找到相关互助招募贴，请尝试搜索其他考证或比赛关键词")
                    }
                }
            } else {
                items(posts, key = { it.id }) { post ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        CoCreatePostCard(
                            post = post,
                            onJoinToggle = {
                                if (post.isJoined || isProfileCompleted) {
                                    viewModel.toggleJoinPost(post.id)
                                } else {
                                    showIncompleteDialog = true
                                }
                            },
                            onContact = { viewModel.contactAuthor(post) }
                        )
                    }
                }
            }
        }

        // 发帖招募弹窗
        if (showCreateDialog) {
            CreatePostDialog(
                onDismiss = { showCreateDialog = false },
                onSubmit = { type, title, platform, content, needed, certs ->
                    viewModel.addCoCreatePost(type, title, platform, content, needed, certs)
                    showCreateDialog = false
                }
            )
        }

        // 完善个人主页提醒弹窗 (风趣幽默版)
        if (showIncompleteDialog) {
            AlertDialog(
                onDismissRequest = { showIncompleteDialog = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(36.dp)) },
                title = {
                    Text(
                        text = "慢着，少侠！名片未亮 📭",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "哎呀！你现在的个人主页空荡荡的，简直比期末考的高数草稿纸还要白！\n\n纸包不住你的满腔才华！队长连你是写得一手硬核代码的“极客霸主”，还是做得出精美 PPT 商业企划的“路演演说家”都不知道，哪里敢随便拉你入队呀？\n\n快去「个人主页」涂点墨水、设定奋斗职业并亮出技术标签。实力拉满，大神队长才能一眼相中你，带你起飞！🏃‍♂️✨",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showIncompleteDialog = false
                            viewModel.navigateToOnlineResume(editMode = true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007200))
                    ) {
                        Text("这就去涂墨水 ➔", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showIncompleteDialog = false }) {
                        Text("先当个无名路人", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun CoCreatePostCard(
    post: CoCreatePost,
    onJoinToggle: () -> Unit,
    onContact: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 作者信息行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(post.authorAvatarColor), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.authorName.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = post.authorName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = post.authorMajor,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // 类型 Badge
                val badgeBgColor = if (post.type == "比赛组队") Color(0xFFE8F5E9) else Color(0xFFFCE4EC)
                val badgeTextColor = if (post.type == "比赛组队") Color(0xFF2E7D32) else Color(0xFFC2185B)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBgColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = post.type,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 帖子标题与平台
            Text(
                text = post.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB703),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "目标/载体: ${post.targetPlatform}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 帖子正文
            Text(
                text = post.content,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 关联证书标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔑 绑定证书:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(post.relatedCertTags) { tag: String ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 需求和职位卡片框
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = post.neededRoles,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 操作底栏 (人数/组队、加入、联系)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 人数标记
                val countText = if (post.type == "比赛组队") {
                    "👥 队伍进度: ${post.memberCount}/${post.memberMax} 人"
                } else {
                    "💬 已解答: ${post.memberCount} 人次"
                }
                Text(
                    text = countText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 交互按钮行
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (post.type == "比赛组队") {
                        val btnColor = if (post.isJoined) Color(0xFFFFF3CD) else Color(0xFF007200)
                        val btnTextColor = if (post.isJoined) Color(0xFF856404) else Color.White
                        Button(
                            onClick = onJoinToggle,
                            colors = ButtonDefaults.buttonColors(containerColor = btnColor, contentColor = btnTextColor),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = if (post.isJoined) BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.5f)) else null,
                            modifier = Modifier.testTag("join_post_btn_${post.id}")
                        ) {
                            Text(if (post.isJoined) "审核中..." else "申请加入", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        val btnColor = if (post.isJoined) Color(0xFFFFF3CD) else Color(0xFFC2185B)
                        val btnTextColor = if (post.isJoined) Color(0xFF856404) else Color.White
                        Button(
                            onClick = onJoinToggle,
                            colors = ButtonDefaults.buttonColors(containerColor = btnColor, contentColor = btnTextColor),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = if (post.isJoined) BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.5f)) else null,
                            modifier = Modifier.testTag("apply_consult_btn_${post.id}")
                        ) {
                            Text(if (post.isJoined) "审核中..." else "预约咨询", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onContact,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("contact_author_btn_${post.id}")
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("联系发起人", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostDialog(
    onDismiss: () -> Unit,
    onSubmit: (type: String, title: String, platform: String, content: String, needed: String, certs: List<String>) -> Unit
) {
    var type by remember { mutableStateOf("比赛组队") }
    var title by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var needed by remember { mutableStateOf("") }
    var certTagsStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发布互助招募贴", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 类型单选
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = type == "比赛组队", onClick = { type = "比赛组队" })
                        Text("比赛组队", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = type == "实习咨询", onClick = { type = "实习咨询" })
                        Text("实习咨询", fontSize = 13.sp)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("帖子标题 (例：招募数模Python程序员)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_input_title"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = platform,
                    onValueChange = { platform = it },
                    label = { Text("目标平台/比赛 (例：全国大学生挑战杯)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_input_platform"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = needed,
                    onValueChange = { needed = it },
                    label = { Text("招募角色/咨询方向 (例：招募：商业计划PPT答辩)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_input_needed"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = certTagsStr,
                    onValueChange = { certTagsStr = it },
                    label = { Text("绑定证书标签 (以逗号分隔，例：软考中级,CPA)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_input_certs"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("详细介绍 (说明你的技术栈、团队基础及期望要求)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_input_content"),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty() && content.isNotEmpty()) {
                        val certs = if (certTagsStr.isEmpty()) emptyList() else certTagsStr.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }
                        onSubmit(type, title, platform, content, needed, certs)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007200)),
                modifier = Modifier.testTag("dialog_submit_btn")
            ) {
                Text("提交发布")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("dialog_dismiss_btn")) {
                Text("取消")
            }
        }
    )
}

// =========================================================
// 🔮 MBTI 职场先锋认证辅助组件与数据模型
// =========================================================
data class MbtiDetails(
    val code: String,
    val title: String,
    val strengths: String,
    val recommendedCerts: String
)

fun getMbtiDetails(code: String): MbtiDetails {
    return when (code) {
        "INTJ" -> MbtiDetails("INTJ", "战略大师", "逻辑缜密，极具远见，善于拆解复杂系统并设计闭环框架。完美适配独立研发或顶层战略咨询。", "《系统集成项目管理工程师（软考中级）》、《证券从业资格》")
        "INTP" -> MbtiDetails("INTP", "思想极客", "理论分析狂，热爱前沿代码及系统重构，对底层算法复杂度非常敏感。适合担任核心后端开发或分析专家。", "《软件设计师（软考中级）》、《信息系统项目管理师（软考高级）》")
        "INFJ" -> MbtiDetails("INFJ", "精神领袖", "直觉极强，具备高度同理心。擅长深度洞察用户核心需求与团队心理疏导。完美适合产品经理及高壁垒咨询。", "《项目管理专业人士 PMP》、《心理咨询师》")
        "INFP" -> MbtiDetails("INFP", "理想主义", "追求精神共鸣，文字表达极具感染力。擅长将产品故事视觉化与情感化包装。极其契合新媒体运营、策划与HR岗。", "《企业人力资源管理师》、《英语六级/雅思》")
        "ENTJ" -> MbtiDetails("ENTJ", "铁血统帅", "天生领袖，结果导向，极具决断力。善于掌控全局项目排期并铁腕推进行动。极其契合大厂项目经理、创业者岗。", "《信息系统项目管理师（软考高级）》、《项目管理专业人士 PMP》")
        "ENTP" -> MbtiDetails("ENTP", "智多星", "思维活跃度极高，极度喜欢头脑风暴与商业破局，不走寻常路。极契合商业拓展（BD）、产品策划与增长运营岗。", "《系统规划与管理师》、《证券从业资格》")
        "ENFJ" -> MbtiDetails("ENFJ", "阳光灯塔", "极具领袖魅力的沟通大师，善于协调跨学科背景的多方利益相关者。适合项目交付专家、高校外联或大客户经理岗。", "《教师资格证》、《企业人力资源管理师》")
        "ENFP" -> MbtiDetails("ENFP", "活力先锋", "擅长快速与陌生人建立高频互动，具有天然的商业拓展爆发力。极其对口大厂增长运营、大客户谈判与BD岗位。", "《助理广告策划师》、《多媒体应用设计师》")
        "ISTJ" -> MbtiDetails("ISTJ", "稳健基石", "恪尽职守，极度注重数据精确度与合规红线，交付零差错。绝配后端合规、财务审计、法律顾问与系统运维岗位。", "《法律职业资格 A 证》、《初级会计师证》")
        "ISFJ" -> MbtiDetails("ISFJ", "守护天使", "极其细致，执行极具耐心，善于维护老客户及后勤平稳流转。完美对口大厂交付专家、售后合规与后勤管理岗。", "《初级会计师证》、《普通话等级证书》")
        "ESTJ" -> MbtiDetails("ESTJ", "高效教官", "注重标准与规则流程，执行力极强，能迅速建立秩序并拆解SOP。对口生产物流经理、安全主管与合规审计岗。", "《注册会计师 CPA》、《系统集成项目管理工程师》")
        "ESFJ" -> MbtiDetails("ESFJ", "社交润滑", "擅长建立和谐人际圈，具有出色的团队黏合度及多方沟通调停手腕。绝配大客户经理（AM）、公共关系及HRBP岗。", "《企业人力资源管理师》、《导游资格证》")
        "ISTP" -> MbtiDetails("ISTP", "孤勇创客", "动手能力极强，理性独立。喜欢分析物理/代码故障并敏捷修复。极其适合嵌入式研发、网络工程师或安全白帽子岗。", "《网络工程师（软考中级）》、《红帽认证 Linux 工程师》")
        "ISFP" -> MbtiDetails("ISFP", "自由艺境", "具备独特的美学敏感度与感性嗅觉。擅长制作高视觉表现力、高级感视觉物料。适合 UI/UX 设计、品牌视觉策划岗。", "《多媒体应用设计师（软考）》、《Adobe 视觉设计师》")
        "ESTP" -> MbtiDetails("ESTP", "破浪行者", "危机应对专家，极度务实，能在高压下迅速做出现场决策。极其适合销售战狼、公关危局处理及大客户开拓岗。", "《证券从业资格》、《项目管理专业人士 PMP》")
        "ESFP" -> MbtiDetails("ESFP", "聚光宠儿", "天生表演者，善于调动全场氛围。能为项目路演、发布会和社群裂变带来爆炸性的吸睛效应。对口路演主讲人、社群运营岗。", "《多媒体应用设计师》、《教师资格证》")
        else -> MbtiDetails("UNKNOWN", "隐世高手", "个性沉稳内敛，深藏不露。拥有尚未点亮的无限职场黑科技潜能。", "请点击上方去认证，解锁属于你的定制考证路径。")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MbtiCertificationDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val mbtiList = listOf(
        "INTJ", "INTP", "INFJ", "INFP",
        "ENTJ", "ENTP", "ENFJ", "ENFP",
        "ISTJ", "ISFJ", "ESTJ", "ESFJ",
        "ISTP", "ISFP", "ESTP", "ESFP"
    )
    var selectedMbti by remember { mutableStateOf("INTJ") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⚡ 认证你的 MBTI 职场本命",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "认证后，系统不仅会在您的个人名片上亮起专属金牌徽章，还将为您定制推送最具优势的考证路径与职场技能描述！",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 4x4 网格展示
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(240.dp)
                ) {
                    items(mbtiList.size) { index ->
                        val code = mbtiList[index]
                        val isSel = selectedMbti == code
                        val details = getMbtiDetails(code)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0xFFE07A5F) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    width = 1.dp,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedMbti = code }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = code,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = details.title.take(4),
                                    color = if (isSel) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedMbti) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007200))
            ) {
                Text("确认认证", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("先不认证", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

// =========================================================
// 📄 个人标准在线简历履历中心 (Online Resume Details & Editor)
// =========================================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnlineResumeScreen(
    viewModel: CareerViewModel,
    onBack: () -> Unit
) {
    val userRealName by viewModel.userRealName.collectAsState()
    val userNickname by viewModel.userNickname.collectAsState()
    val userPhone by viewModel.userPhone.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val userGender by viewModel.userGender.collectAsState()
    val userSchool by viewModel.userSchool.collectAsState()
    val userGrade by viewModel.userGrade.collectAsState()
    val userMajor by viewModel.userMajor.collectAsState()
    val userMbti by viewModel.userMbti.collectAsState()

    val resumeTargetJob by viewModel.resumeTargetJob.collectAsState()
    val resumeExpectSalary by viewModel.resumeExpectSalary.collectAsState()
    val resumeExpectCity by viewModel.resumeExpectCity.collectAsState()
    val resumeJobStatus by viewModel.resumeJobStatus.collectAsState()
    val resumeEducationDegree by viewModel.resumeEducationDegree.collectAsState()
    val resumeHonors by viewModel.resumeHonors.collectAsState()
    val resumeSelfIntro by viewModel.resumeSelfIntro.collectAsState()
    val resumeWorkExperiences by viewModel.resumeWorkExperiences.collectAsState()
    val resumeSkillTags by viewModel.resumeSkillTags.collectAsState()
    val resumeCertifications by viewModel.resumeCertifications.collectAsState()
    val resumeContestAwards by viewModel.resumeContestAwards.collectAsState()

    val resumeStartInEditMode by viewModel.resumeStartInEditMode.collectAsState()
    var isEditMode by remember(resumeStartInEditMode) { mutableStateOf(resumeStartInEditMode) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    
    // 经历增删改弹窗状态
    var showEditExpDialog by remember { mutableStateOf(false) }
    var editingExp by remember { mutableStateOf<ResumeWorkExp?>(null) }
    var showDeleteConfirmExp by remember { mutableStateOf<ResumeWorkExp?>(null) }

    // 其它各板块独立快速编辑弹窗状态
    var showEditBasicDialog by remember { mutableStateOf(false) }
    var showEditPrefsDialog by remember { mutableStateOf(false) }
    var showEditSelfIntroDialog by remember { mutableStateOf(false) }
    var showEditEduDialog by remember { mutableStateOf(false) }
    var showAddCertDialog by remember { mutableStateOf(false) }
    var showAddContestDialog by remember { mutableStateOf(false) }
    var showAddSkillDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (isEditMode) {
            isEditMode = false
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "个人标准在线简历",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "名企直聘高通关标准履历 · 全模块支持实时编辑",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (isEditMode) {
                                viewModel.isProfileCompleted.value = true
                                isEditMode = false
                            } else {
                                isEditMode = true
                            }
                        }
                    ) {
                        Text(
                            text = if (isEditMode) "💾 完成编辑" else "✏️ 快速编辑模式",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isEditMode) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4361EE)),
                        border = BorderStroke(1.dp, Color(0xFF4361EE).copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("导出 PDF / 图片", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onBack()
                            viewModel.currentTab.value = 3 // 跳转到求职信使直接直聘
                        },
                        modifier = Modifier.weight(1.2f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4361EE))
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("附带简历直连 HR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 💡 快速编辑模式提示条 (当从“这就去涂墨水”跳转或手动开启编辑时展示)
            if (isEditMode) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F5E9),
                    border = BorderStroke(1.2.dp, Color(0xFF4CAF50).copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth().testTag("resume_edit_mode_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✏️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "已为您开启标准简历在线编辑模式",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = "点击下方各版块铅笔图标或直接点击内容即可快捷修改履历，完善后点击右上角「完成编辑」。",
                                fontSize = 11.sp,
                                color = Color(0xFF388E3C),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // 1. 顶部个人名片头 (Personal Header Card - 支持编辑)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .border(2.5.dp, Color(0xFF4361EE), CircleShape)
                                .background(Color(0xFF4CC9F0), CircleShape)
                                .clickable { showEditBasicDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (userRealName.isBlank() || userRealName == "待填写") "待" else userRealName.firstOrNull()?.toString() ?: "待",
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = userRealName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFE3F2FD)
                                    ) {
                                        Text(
                                            text = if (userGender == "待填写" && userGrade == "待填写") "基本信息待填写" else "$userGender · $userGrade",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1976D2),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (userMbti != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFE07A5F).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "🔮 $userMbti",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFE07A5F),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { showEditBasicDialog = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "编辑基本信息",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (userSchool == "待填写" && userMajor == "待填写") "高校与专业背景：待填写" else "$userSchool · $userMajor",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (userPhone == "待填写") "📞 手机号待填写" else "📞 $userPhone",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (userEmail == "待填写") "✉️ 邮箱待填写" else "✉️ $userEmail",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // 求职状态切换条
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable { showStatusDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("求职状态:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = resumeJobStatus,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        Text("点击切换 ▾", fontSize = 10.sp, color = Color(0xFF4361EE), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 2. 🎯 求职意向 (Career Preferences - 可编辑)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 求职意向",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { showEditPrefsDialog = true },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("编辑意向", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showEditPrefsDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("期望职位:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(resumeTargetJob, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("期望薪资:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(resumeExpectSalary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("期望城市:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(resumeExpectCity, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("求职类型:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("全职 / 长期实习", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // 3. 💡 个人优势与自我评价 (Personal Strengths & Summary - 可编辑)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡 个人核心优势与职场自我评价",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { showEditSelfIntroDialog = true },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("编辑评价", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showEditSelfIntroDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = resumeSelfIntro,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                    )
                }
            }

            // 4. 🎓 教育经历 (Education - 可编辑)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (userSchool == "待填写") "🎓 教育背景 (待填写)" else "🎓 教育背景",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { showEditEduDialog = true },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("编辑学籍", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showEditEduDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (userSchool == "待填写") "就读高校: 待填写" else userSchool,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (resumeEducationDegree == "待填写") "学历: 待填写" else resumeEducationDegree,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (userMajor == "待填写") "专业：待填写" else "专业：$userMajor · 统招本科",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (resumeHonors == "待填写") "🏅 学业荣誉: 待填写" else "🏅 学业荣誉: $resumeHonors",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // 5. 💼 工作 / 实战项目经历 (🌟 包含【+ 添加】组件与全套可编辑 UI)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💼 工作与科创实战经历 (STAR 法则量化)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 🌟 右侧【+ 添加】组件
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF4361EE).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF4361EE).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            editingExp = null // 新增模式
                            showEditExpDialog = true
                        }
                        .testTag("add_experience_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加新经历",
                            tint = Color(0xFF4361EE),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "添加",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF4361EE)
                        )
                    }
                }
            }

            if (resumeWorkExperiences.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("暂无工作或科创实战经历", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                editingExp = null
                                showEditExpDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4361EE))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("立即添加第一段经历", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    resumeWorkExperiences.forEach { exp ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("work_exp_card_${exp.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = exp.company,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    // 编辑与删除操作按钮行
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                editingExp = exp
                                                showEditExpDialog = true
                                            },
                                            modifier = Modifier.size(28.dp).testTag("edit_exp_btn_${exp.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "编辑经历",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(2.dp))
                                        IconButton(
                                            onClick = { showDeleteConfirmExp = exp },
                                            modifier = Modifier.size(28.dp).testTag("delete_exp_btn_${exp.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "删除经历",
                                                tint = Color(0xFFE53935),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = exp.role,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = exp.period,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = exp.description,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }

            // 6. 📜 权威持证资质 (Verified Certifications - 可添加/删除)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📜 权威持证资质 (官方核验证照)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { showAddCertDialog = true },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("添加证书", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (resumeCertifications.isEmpty()) {
                        Text("暂无已核验证书，点击右上角「添加证书」拍照/上传凭证", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        resumeCertifications.forEach { cert ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text("🏆", fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = cert,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFE8F5E9)
                                    ) {
                                        Text(
                                            text = "已核验 ✔",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteResumeCert(cert) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "移除证书",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. 🥇 所获竞赛证书 (高校科创与学科竞赛 - 可添加/删除)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🥇 所获竞赛证书 (高校科创与学科竞赛)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { showAddContestDialog = true },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("add_contest_award_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("添加竞赛", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (resumeContestAwards.isEmpty()) {
                        Text("暂无竞赛获奖证书，点击右上角「添加竞赛」拍照/上传凭证", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        resumeContestAwards.forEach { award ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text("🎖️", fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = award,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (award.contains("金奖") || award.contains("国家") || award.contains("全国")) Color(0xFFFFF3E0) else Color(0xFFE8EAF6)
                                    ) {
                                        Text(
                                            text = if (award.contains("金奖") || award.contains("国家") || award.contains("全国")) "国奖认证 🎖️" else "省部认证 🏅",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (award.contains("金奖") || award.contains("国家") || award.contains("全国")) Color(0xFFE65100) else Color(0xFF3949AB),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteContestAward(award) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "移除竞赛证书",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 8. 🛠️ 核心专业技能栈 (Core Skills & Tools - 可添加/删除)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🛠️ 专业技能栈与工程工具",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { showAddSkillDialog = true },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("添加技能", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (resumeSkillTags.isEmpty()) {
                        Text("暂无技能标签，点击右上角「添加技能」配置核心技术栈 (待填写)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                        resumeSkillTags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "删除技能",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { viewModel.deleteResumeSkill(tag) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 🌟 1. 工作与科创经历【新增/编辑】全功能弹窗
        if (showEditExpDialog) {
            var tempCompany by remember { mutableStateOf(editingExp?.company ?: "") }
            var tempRole by remember { mutableStateOf(editingExp?.role ?: "") }
            var tempPeriod by remember { mutableStateOf(editingExp?.period ?: "2025.03 - 2025.09") }
            var tempDescription by remember { mutableStateOf(editingExp?.description ?: "") }

            AlertDialog(
                onDismissRequest = { showEditExpDialog = false },
                title = {
                    Text(
                        text = if (editingExp == null) "💼 添加工作/科创经历" else "✏️ 编辑工作/科创经历",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = tempCompany,
                            onValueChange = { tempCompany = it },
                            label = { Text("单位 / 科创项目组名称") },
                            placeholder = { Text("例：星澜数智科技 (深圳) 有限公司") },
                            modifier = Modifier.fillMaxWidth().testTag("input_exp_company"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = tempRole,
                            onValueChange = { tempRole = it },
                            label = { Text("担任角色 / 岗位职称") },
                            placeholder = { Text("例：全栈研发实习生 / 团队队长") },
                            modifier = Modifier.fillMaxWidth().testTag("input_exp_role"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = tempPeriod,
                            onValueChange = { tempPeriod = it },
                            label = { Text("时间周期") },
                            placeholder = { Text("例：2025.03 - 2025.09") },
                            modifier = Modifier.fillMaxWidth().testTag("input_exp_period"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = tempDescription,
                            onValueChange = { tempDescription = it },
                            label = { Text("STAR 法则实战业绩与成果") },
                            placeholder = { Text("例：负责微服务核心接口研发，通过 Kotlin 协程并发重构吞吐提升 35%...") },
                            modifier = Modifier.fillMaxWidth().testTag("input_exp_desc"),
                            minLines = 4,
                            maxLines = 8
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (tempCompany.isNotBlank() && tempRole.isNotBlank()) {
                                if (editingExp == null) {
                                    val newExp = ResumeWorkExp(
                                        id = "exp_${System.currentTimeMillis()}",
                                        company = tempCompany.trim(),
                                        role = tempRole.trim(),
                                        period = tempPeriod.trim(),
                                        description = tempDescription.trim()
                                    )
                                    viewModel.addWorkExperience(newExp)
                                } else {
                                    val updatedExp = editingExp!!.copy(
                                        company = tempCompany.trim(),
                                        role = tempRole.trim(),
                                        period = tempPeriod.trim(),
                                        description = tempDescription.trim()
                                    )
                                    viewModel.updateWorkExperience(updatedExp)
                                }
                                viewModel.isProfileCompleted.value = true
                                showEditExpDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4361EE)),
                        modifier = Modifier.testTag("save_exp_submit_btn")
                    ) {
                        Text("保存", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditExpDialog = false }) { Text("取消") }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 🌟 2. 删除经历确认弹窗
        if (showDeleteConfirmExp != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmExp = null },
                title = { Text("确认删除该段经历？", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = { Text("删除「${showDeleteConfirmExp?.company}」后将无法恢复。") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteWorkExperience(showDeleteConfirmExp!!.id)
                            showDeleteConfirmExp = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text("确认删除", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmExp = null }) { Text("取消") }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 🌟 3. 基本信息编辑弹窗 (Real Name, Gender, Grade, School, Major, Phone, Email)
        if (showEditBasicDialog) {
            var tempName by remember { mutableStateOf(if (userRealName == "待填写") "" else userRealName) }
            var tempGender by remember { mutableStateOf(if (userGender == "待填写") "" else userGender) }
            var tempGrade by remember { mutableStateOf(if (userGrade == "待填写") "" else userGrade) }
            var tempPhone by remember { mutableStateOf(if (userPhone == "待填写") "" else userPhone) }
            var tempEmail by remember { mutableStateOf(if (userEmail == "待填写") "" else userEmail) }

            AlertDialog(
                onDismissRequest = { showEditBasicDialog = false },
                title = { Text("👤 编辑基本信息", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            label = { Text("姓名") },
                            placeholder = { Text("待填写") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = tempGender,
                                onValueChange = { tempGender = it },
                                label = { Text("性别") },
                                placeholder = { Text("男 / 女") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = tempGrade,
                                onValueChange = { tempGrade = it },
                                label = { Text("年级") },
                                placeholder = { Text("如：大三 / 研一") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = tempPhone,
                            onValueChange = { tempPhone = it },
                            label = { Text("手机号") },
                            placeholder = { Text("待填写") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tempEmail,
                            onValueChange = { tempEmail = it },
                            label = { Text("邮箱") },
                            placeholder = { Text("待填写") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.userRealName.value = tempName.trim().ifBlank { "待填写" }
                            viewModel.userGender.value = tempGender.trim().ifBlank { "待填写" }
                            viewModel.userGrade.value = tempGrade.trim().ifBlank { "待填写" }
                            viewModel.userPhone.value = tempPhone.trim().ifBlank { "待填写" }
                            viewModel.userEmail.value = tempEmail.trim().ifBlank { "待填写" }
                            showEditBasicDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4361EE))
                    ) {
                        Text("保存", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditBasicDialog = false }) { Text("取消") }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 🌟 4. 求职意向编辑弹窗
        if (showEditPrefsDialog) {
            var tempTargetJob by remember { mutableStateOf(if (resumeTargetJob == "待填写") "" else resumeTargetJob) }
            var tempExpectSalary by remember { mutableStateOf(if (resumeExpectSalary == "待填写") "" else resumeExpectSalary) }
            var tempExpectCity by remember { mutableStateOf(if (resumeExpectCity == "待填写") "" else resumeExpectCity) }

            AlertDialog(
                onDismissRequest = { showEditPrefsDialog = false },
                title = { Text("🎯 编辑求职意向", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = tempTargetJob,
                            onValueChange = { tempTargetJob = it },
                            label = { Text("期望职位") },
                            placeholder = { Text("例：全栈工程师 / 算法工程师") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tempExpectSalary,
                            onValueChange = { tempExpectSalary = it },
                            label = { Text("期望薪资") },
                            placeholder = { Text("例：15K - 25K / 月") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tempExpectCity,
                            onValueChange = { tempExpectCity = it },
                            label = { Text("期望城市") },
                            placeholder = { Text("例：深圳 / 杭州 / 上海") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resumeTargetJob.value = tempTargetJob.trim().ifBlank { "待填写" }
                            viewModel.resumeExpectSalary.value = tempExpectSalary.trim().ifBlank { "待填写" }
                            viewModel.resumeExpectCity.value = tempExpectCity.trim().ifBlank { "待填写" }
                            showEditPrefsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4361EE))
                    ) {
                        Text("保存", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditPrefsDialog = false }) { Text("取消") }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 🌟 5. 个人自我评价编辑弹窗
        if (showEditSelfIntroDialog) {
            var tempIntro by remember { mutableStateOf(if (resumeSelfIntro == "待填写") "" else resumeSelfIntro) }

            AlertDialog(
                onDismissRequest = { showEditSelfIntroDialog = false },
                title = { Text("💡 编辑个人优势与评价", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = tempIntro,
                        onValueChange = { tempIntro = it },
                        label = { Text("自我评价与优势描述") },
                        placeholder = { Text("待填写 (填写您的专业背景与个人亮点)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        maxLines = 10
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resumeSelfIntro.value = tempIntro.trim().ifBlank { "待填写" }
                            showEditSelfIntroDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4361EE))
                    ) {
                        Text("保存", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditSelfIntroDialog = false }) { Text("取消") }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 🌟 6. 教育经历编辑弹窗
        if (showEditEduDialog) {
            var tempSchool by remember { mutableStateOf(if (userSchool == "待填写") "" else userSchool) }
            var tempMajor by remember { mutableStateOf(if (userMajor == "待填写") "" else userMajor) }
            var tempDegree by remember { mutableStateOf(if (resumeEducationDegree == "待填写") "" else resumeEducationDegree) }
            var tempHonors by remember { mutableStateOf(if (resumeHonors == "待填写") "" else resumeHonors) }

            AlertDialog(
                onDismissRequest = { showEditEduDialog = false },
                title = { Text("🎓 编辑教育背景", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = tempSchool,
                            onValueChange = { tempSchool = it },
                            label = { Text("就读高校") },
                            placeholder = { Text("如：清华大学 / 浙江大学") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tempMajor,
                            onValueChange = { tempMajor = it },
                            label = { Text("所学专业") },
                            placeholder = { Text("如：计算机科学与技术 / 软件工程") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tempDegree,
                            onValueChange = { tempDegree = it },
                            label = { Text("学历与时间") },
                            placeholder = { Text("例：本科学士 (2022 - 2026)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tempHonors,
                            onValueChange = { tempHonors = it },
                            label = { Text("学业荣誉与奖项") },
                            placeholder = { Text("待填写") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.userSchool.value = tempSchool.trim().ifBlank { "待填写" }
                            viewModel.userMajor.value = tempMajor.trim().ifBlank { "待填写" }
                            viewModel.resumeEducationDegree.value = tempDegree.trim().ifBlank { "待填写" }
                            viewModel.resumeHonors.value = tempHonors.trim().ifBlank { "待填写" }
                            showEditEduDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4361EE))
                    ) {
                        Text("保存", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditEduDialog = false }) { Text("取消") }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 🌟 7. 添加权威考证资质弹窗 (右下角含拍照、上传文件图标栏)
        if (showAddCertDialog) {
            var newCertName by remember { mutableStateOf("") }
            var attachedCertFile by remember { mutableStateOf("") }
            val quickCerts = listOf(
                "信息系统项目管理师 (软考高级)",
                "系统集成项目管理工程师 (软考中级)",
                "注册会计师 CPA",
                "法律职业资格 A 证",
                "大学英语六级 CET-6",
                "一级建造师",
                "证券从业资格"
            )

            AlertDialog(
                onDismissRequest = { showAddCertDialog = false },
                title = { Text("📜 添加权威考证资质", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newCertName,
                            onValueChange = { newCertName = it },
                            label = { Text("证书名称") },
                            placeholder = { Text("输入或点击下方快速推荐") },
                            modifier = Modifier.fillMaxWidth().testTag("input_cert_name"),
                            singleLine = true
                        )

                        // 📸 拍照 & 📁 上传文件功能图标栏 (右下角)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (attachedCertFile.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = attachedCertFile,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "删除附件",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(14.dp).clickable { attachedCertFile = "" }
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "附加证书凭据/原件扫描:",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // 右下角功能图标栏
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 📸 拍照图标
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF4361EE).copy(alpha = 0.12f),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                attachedCertFile = "拍照凭证_IMG_${System.currentTimeMillis() % 10000}.jpg"
                                                if (newCertName.isEmpty()) newCertName = "信息系统项目管理师 (软考高级)"
                                            }
                                            .testTag("cert_camera_btn")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = "拍照上传", tint = Color(0xFF4361EE), modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // 📁 上传文件图标
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF009688).copy(alpha = 0.12f),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                attachedCertFile = "证书核验证据_PDF_${System.currentTimeMillis() % 10000}.pdf"
                                                if (newCertName.isEmpty()) newCertName = "注册会计师 CPA"
                                            }
                                            .testTag("cert_upload_file_btn")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.UploadFile, contentDescription = "上传证书文件", tint = Color(0xFF009688), modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // 🖼️ 相册选取图标
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFE65100).copy(alpha = 0.12f),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                attachedCertFile = "相册凭据_PHOTO_${System.currentTimeMillis() % 10000}.png"
                                            }
                                            .testTag("cert_gallery_btn")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Image, contentDescription = "从相册选取", tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Text("快速推荐证书:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickCerts.forEach { qc ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.clickable { newCertName = qc }
                                ) {
                                    Text(
                                        text = qc,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newCertName.isNotBlank()) {
                                viewModel.addResumeCert(newCertName)
                                showAddCertDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4361EE))
                    ) {
                        Text("添加", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCertDialog = false }) { Text("取消") }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 🌟 7.5 添加所获竞赛证书弹窗 (右下角含拍照、上传文件图标栏)
        if (showAddContestDialog) {
            var newContestName by remember { mutableStateOf("") }
            var attachedContestFile by remember { mutableStateOf("") }
            val quickContests = listOf(
                "第十四届「挑战杯」中国大学生创业计划竞赛 全国金奖",
                "中国国际大学生创新大赛 (原互联网+) 省级一等奖",
                "全国大学生数学建模竞赛 (MCM/ICM) 国家二等奖",
                "中国高校计算机大赛 (团体程序设计天梯赛) 全国一等奖",
                "蓝桥杯全国软件和信息技术专业人才大赛 全国一等奖",
                "全国大学生机器人大赛 (RoboMaster) 一等奖",
                "全国大学生电子商务「三创赛」 省级特等奖"
            )

            AlertDialog(
                onDismissRequest = { showAddContestDialog = false },
                title = { Text("🥇 添加所获竞赛证书", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newContestName,
                            onValueChange = { newContestName = it },
                            label = { Text("竞赛名称与获奖等级") },
                            placeholder = { Text("例：挑战杯中国大学生创业计划竞赛 全国金奖") },
                            modifier = Modifier.fillMaxWidth().testTag("input_contest_name"),
                            singleLine = true
                        )

                        // 📸 拍照 & 📁 上传文件功能图标栏 (右下角)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (attachedContestFile.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = attachedContestFile,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "删除附件",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(14.dp).clickable { attachedContestFile = "" }
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "附加获奖证书拍照/文件:",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // 右下角功能图标栏
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 📸 拍照图标
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF4361EE).copy(alpha = 0.12f),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                attachedContestFile = "竞赛证书拍照_IMG_${System.currentTimeMillis() % 10000}.jpg"
                                                if (newContestName.isEmpty()) newContestName = "第十四届「挑战杯」中国大学生创业计划竞赛 全国金奖"
                                            }
                                            .testTag("contest_camera_btn")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = "拍照上传", tint = Color(0xFF4361EE), modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // 📁 上传文件图标
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF009688).copy(alpha = 0.12f),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                attachedContestFile = "获奖公函及证书_PDF_${System.currentTimeMillis() % 10000}.pdf"
                                                if (newContestName.isEmpty()) newContestName = "中国国际大学生创新大赛 省级一等奖"
                                            }
                                            .testTag("contest_upload_file_btn")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.UploadFile, contentDescription = "上传竞赛证书文件", tint = Color(0xFF009688), modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // 🖼️ 相册选取图标
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFE65100).copy(alpha = 0.12f),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                attachedContestFile = "相册获奖照片_PHOTO_${System.currentTimeMillis() % 10000}.png"
                                            }
                                            .testTag("contest_gallery_btn")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Image, contentDescription = "从相册选取", tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Text("快速推荐竞赛荣誉:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickContests.forEach { qc ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.clickable { newContestName = qc }
                                ) {
                                    Text(
                                        text = qc,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newContestName.isNotBlank()) {
                                viewModel.addContestAward(newContestName)
                                showAddContestDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4361EE)),
                        modifier = Modifier.testTag("save_contest_submit_btn")
                    ) {
                        Text("添加", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddContestDialog = false }) { Text("取消") }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 🌟 8. 添加专业技能标签弹窗
        if (showAddSkillDialog) {
            var newSkillName by remember { mutableStateOf("") }
            val quickSkills = listOf(
                "Kotlin", "Jetpack Compose", "Java", "Python", "Spring Boot", "MySQL", "Room", "Docker", "Git敏捷协作", "高并发架构", "无人机遥测"
            )

            AlertDialog(
                onDismissRequest = { showAddSkillDialog = false },
                title = { Text("🛠️ 添加专业技能标签", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newSkillName,
                            onValueChange = { newSkillName = it },
                            label = { Text("技能标签名称") },
                            placeholder = { Text("例：TypeScript / Vue / 机器学习") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text("快速推荐技能:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickSkills.forEach { qs ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.clickable { newSkillName = qs }
                                ) {
                                    Text(
                                        text = qs,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newSkillName.isNotBlank()) {
                                viewModel.addResumeSkill(newSkillName)
                                showAddSkillDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4361EE))
                    ) {
                        Text("添加", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSkillDialog = false }) { Text("取消") }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 求职状态选择弹窗
        if (showStatusDialog) {
            val statusList = listOf(
                "离校实习 · 随时到岗",
                "在职 · 考虑好机会",
                "在校生 · 寻找寒暑假实习",
                "应届毕业生 · 积极求职",
                "暂不找工作 · 专注考证"
            )
            AlertDialog(
                onDismissRequest = { showStatusDialog = false },
                title = { Text("选择当前求职状态", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        statusList.forEach { st ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (resumeJobStatus == st) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.resumeJobStatus.value = st
                                        showStatusDialog = false
                                    }
                            ) {
                                Text(
                                    text = st,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (resumeJobStatus == st) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showStatusDialog = false }) { Text("关闭") }
                }
            )
        }

        // 导出成功提示弹窗
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                icon = { Text("📄", fontSize = 28.sp) },
                title = { Text("简历导出就绪", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "您的个人求职简历${if (userSchool != "待填写") "（含${userSchool}学籍与官方考证资质核验）" else "（含官方学籍与考证资质核验）"}已自动封装为高清 PDF 格式，支持直接分享投递到 HR 邮箱或招聘平台！",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showExportDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4361EE))
                    ) {
                        Text("知道了", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
}

// =========================================================
// 🕵️ 详细个人资料与求学认证页面 (Professional Detailed Profile Screen)
// =========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailedProfileScreen(
    viewModel: CareerViewModel,
    onBack: () -> Unit
) {
    val userRealName by viewModel.userRealName.collectAsState()
    val userNickname by viewModel.userNickname.collectAsState()
    val userPhone by viewModel.userPhone.collectAsState()
    val userGender by viewModel.userGender.collectAsState()
    val userSchool by viewModel.userSchool.collectAsState()
    val userGrade by viewModel.userGrade.collectAsState()
    val userMajor by viewModel.userMajor.collectAsState()
    val boundWeChat by viewModel.boundWeChat.collectAsState()

    var tempRealName by remember { mutableStateOf(if (userRealName == "待填写") "" else userRealName) }
    var tempNickname by remember { mutableStateOf(if (userNickname == "待填写") "" else userNickname) }
    var tempPhone by remember { mutableStateOf(if (userPhone == "待填写") "" else userPhone) }
    var tempGender by remember { mutableStateOf(if (userGender == "待填写") "" else userGender) }
    var tempSchool by remember { mutableStateOf(if (userSchool == "待填写") "" else userSchool) }
    var tempGrade by remember { mutableStateOf(if (userGrade == "待填写") "" else userGrade) }
    var tempMajor by remember { mutableStateOf(if (userMajor == "待填写") "" else userMajor) }
    var tempWeChat by remember { mutableStateOf(if (boundWeChat == "待填写") "" else boundWeChat) }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "编辑可信科创档案",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 头像和数字身份证卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .background(Color(0xFF4CC9F0), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tempNickname.isBlank() || tempNickname == "待填写") "待" else tempNickname.firstOrNull()?.toString() ?: "待",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "搭伙圈数字化校园凭证 (可信)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (tempPhone.isBlank() || tempPhone == "待填写") "UID: 待绑定手机生成 (平台加密保护)" else "UID: ${tempPhone.hashCode().coerceAtLeast(100000)} (平台加密保护)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. 💡 基础社交信息 Card
            Text(
                text = "💡 基础个人与联系方式",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = tempNickname,
                        onValueChange = { tempNickname = it },
                        label = { Text("搭伙圈昵称", fontWeight = FontWeight.Bold) },
                        placeholder = { Text("待填写") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_nickname"),
                        singleLine = true,
                        leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )

                    OutlinedTextField(
                        value = tempRealName,
                        onValueChange = { tempRealName = it },
                        label = { Text("真实姓名 (仅实名比对用)", fontWeight = FontWeight.Bold) },
                        placeholder = { Text("待填写") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_realname"),
                        singleLine = true,
                        leadingIcon = { Icon(imageVector = Icons.Default.AccountBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )

                    // 性别单选 Chips
                    Column {
                        Text(
                            text = "性别",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("男", "女").forEach { genderOption ->
                                val isSelected = tempGender == genderOption
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { tempGender = genderOption },
                                    label = { Text(genderOption, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = tempPhone,
                        onValueChange = { tempPhone = it },
                        label = { Text("手机号", fontWeight = FontWeight.Bold) },
                        placeholder = { Text("待填写") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_phone"),
                        singleLine = true,
                        leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )
                }
            }

            // 3. 🎓 高校求学背景
            Text(
                text = "🎓 教育经历",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = tempSchool,
                        onValueChange = { tempSchool = it },
                        label = { Text("就读高校", fontWeight = FontWeight.Bold) },
                        placeholder = { Text("如：清华大学 / 华中科技大学") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_school"),
                        singleLine = true,
                        leadingIcon = { Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tempMajor,
                            onValueChange = { tempMajor = it },
                            label = { Text("所学专业", fontWeight = FontWeight.Bold) },
                            placeholder = { Text("所学专业 (待填写)") },
                            modifier = Modifier.weight(1.2f).testTag("edit_major"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tempGrade,
                            onValueChange = { tempGrade = it },
                            label = { Text("年级", fontWeight = FontWeight.Bold) },
                            placeholder = { Text("大三 / 研一") },
                            modifier = Modifier.weight(0.8f).testTag("edit_grade"),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. 保存并生效
            Button(
                onClick = {
                    // 保存草稿数据到全局 ViewModel StateFlows
                    viewModel.userRealName.value = tempRealName.trim().ifBlank { "待填写" }
                    viewModel.userNickname.value = tempNickname.trim().ifBlank { "待填写" }
                    viewModel.userPhone.value = tempPhone.trim().ifBlank { "待填写" }
                    viewModel.userGender.value = tempGender.trim().ifBlank { "待填写" }
                    viewModel.userSchool.value = tempSchool.trim().ifBlank { "待填写" }
                    viewModel.userGrade.value = tempGrade.trim().ifBlank { "待填写" }
                    viewModel.userMajor.value = tempMajor.trim().ifBlank { "待填写" }
                    viewModel.boundWeChat.value = tempWeChat.trim().ifBlank { "待填写" }
                    
                    // 当关键信息填写后，设定为已完善
                    val isCompleted = tempRealName.isNotBlank() || tempSchool.isNotBlank() || tempPhone.isNotBlank()
                    viewModel.isProfileCompleted.value = isCompleted
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_profile_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "保存可信档案并更新名片",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// =========================================================
// 📄 AI 极客简历智能优化专区 (AI Tech Resume Optimizer)
// =========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeOptimizationScreen(
    viewModel: CareerViewModel,
    onBack: () -> Unit
) {
    val userNickname by viewModel.userNickname.collectAsState()
    val userSchool by viewModel.userSchool.collectAsState()
    val userMajor by viewModel.userMajor.collectAsState()
    val userGrade by viewModel.userGrade.collectAsState()
    val userMbti by viewModel.userMbti.collectAsState()
    val isProfileCompleted by viewModel.isProfileCompleted.collectAsState()

    // 交互状态
    var diagnosticScore by remember { mutableStateOf(75) }
    var isDiagnosing by remember { mutableStateOf(false) }
    var hasDiagnosed by remember { mutableStateOf(false) }
    
    var selectedTargetJob by remember { mutableStateOf("后端科创极客") }
    
    var isPolishing by remember { mutableStateOf(false) }
    var hasPolished by remember { mutableStateOf(false) }

    // 推荐岗位关键词
    val recommendedKeywords = remember(selectedTargetJob) {
        when (selectedTargetJob) {
            "后端科创极客" -> listOf("Kotlin协程并发", "Spring Boot微服务", "科创项目实战", "高可用高防设计")
            "无人机遥测专家" -> listOf("RTK厘米级定位", "MAVLink协议通讯", "DJI SDK控制", "飞控算法实盘调试")
            "科创产品经理" -> listOf("商业BP书写", "原型高保真交互", "PRD规范编写", "全生命周期研发管理")
            else -> listOf("Python全栈开发", "高频数据清洗", "Git高效协同", "技术栈深度融合")
        }
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI 极客简历智能优化专区",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 顶部当前个人学籍档案凭证简报
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📄",
                            fontSize = 22.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (userNickname == "待填写") "正在优化：新用户的求职简历" else "正在优化：$userNickname 的求职简历",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (userSchool == "待填写" && userMajor == "待填写") "学籍背景：待填写 (建议先完善个人信息)" else "学籍背景：$userSchool · $userMajor ($userGrade)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. 📊 AI 智能诊断诊断书 (AI Diagnostic Panel)
            Text(
                text = "📊 简历科创竞争力诊断",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "当前简历质量诊断分",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$diagnosticScore",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (diagnosticScore < 80) Color(0xFFFFB703) else Color(0xFF4CAF50)
                                )
                                Text(
                                    text = " / 100",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                isDiagnosing = true
                                hasDiagnosed = false
                            },
                            enabled = !isDiagnosing,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isDiagnosing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("诊断中...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("开始 AI 一键诊断", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isDiagnosing) {
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(1200)
                            diagnosticScore = 96
                            isDiagnosing = false
                            hasDiagnosed = true
                        }
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                    }

                    if (hasDiagnosed) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "🎯 AI 专家优化建议 (已生成):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = "1. 建议在简历头部智能加装【科创精英徽章】，在简历中增加高信誉项目标识，可使网申通过率提升 40%！",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = Color(0xFF1B5E20)
                                )
                                Text(
                                    text = "2. 您的专业是 $userMajor，建议在项目描述中将“和同学组队做程序”重构为“基于 Git 敏捷模型主导多角色联合协同”，含金量大幅跃升。",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "💡 点击按钮诊断简历，AI 将根据您的学术背景、MBTI 性格特质及当前意向职业，深度诊断潜在亮点与缺口！",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. 🎯 目标岗位关键词注入 (Target Job Chips)
            Text(
                text = "⚙️ 设定目标岗位与大厂关键词注入",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "当前期望优化方向",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("后端科创极客", "无人机遥测专家", "科创产品经理").forEach { job ->
                            val isSelected = selectedTargetJob == job
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .clickable { selectedTargetJob = job }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = job,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "💡 智能注入大厂简历核心高频词:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        recommendedKeywords.forEach { word ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = word,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 4. ✨ 智能重构对比栏 (Before vs After Comparison)
            Text(
                text = "✨ 智能重构描述对比",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFEBEE))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "❌ 原版学生腔描述 (优化前)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "“在学校里和同学一起做过几个无人机的小应用和一些 Python 脚本。平时没怎么做过大型项目，喜欢在网上学习，希望找个极客团队带我一起搭伙比赛。”",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = Color(0xFFB71C1C)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (hasPolished) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (hasPolished) "✨ AI 极客精修版描述 (优化后)" else "⏳ 精修描述 (等待重构)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasPolished) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (hasPolished) {
                                "“【学术背景】具备良好的高校学术成长路径与系统的理论素养背书。\n【技术核心】主导多角色敏捷协作，攻克无人机遥测、高频数据包清洗与高可用 Spring / Kotlin 协同核心，具备出色的团队大厂作战协同能力与 MBTI (${userMbti ?: "ENFP"}) 创意领袖直觉。”"
                            } else {
                                "“点击下方「一键 AI 极客精修」按钮，系统将根据您的学校、专业 ($userMajor) 及选定岗位方向，自动融合科创实践，为您量身定制大厂标准重构段落。”"
                            },
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = if (hasPolished) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    if (isPolishing) {
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(1500)
                            isPolishing = false
                            hasPolished = true
                        }
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF4CAF50))
                    }

                    if (!hasPolished) {
                        Button(
                            onClick = { isPolishing = true },
                            modifier = Modifier.fillMaxWidth().testTag("ai_polish_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("一键 AI 极客精修", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.isProfileCompleted.value = true
                                onBack()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("adopt_polish_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007200))
                        ) {
                            Text("一键采纳精修文案并更新名片 ✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobRecruitmentTabScreen(viewModel: CareerViewModel) {
    val jobsList by viewModel.filteredRecruitingJobs.collectAsState()
    val selectedJobIndustry by viewModel.selectedJobIndustry.collectAsState()
    val selectedJobCity by viewModel.selectedJobCity.collectAsState()
    val jobSearchQuery by viewModel.jobSearchQuery.collectAsState()
    val isProfileCompleted by viewModel.isProfileCompleted.collectAsState()
    val recruitersList by viewModel.recruiters.collectAsState()

    val searchMode by viewModel.searchMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val occupationsList by viewModel.occupationsList.collectAsState()
    val certificatesList by viewModel.certificatesList.collectAsState()

    val recentJobSearches by viewModel.recentJobSearches.collectAsState()
    val recentCertSearches by viewModel.recentCertSearches.collectAsState()

    val selectedOccupationId by viewModel.selectedOccupationId.collectAsState()
    val selectedCertificateId by viewModel.selectedCertificateId.collectAsState()

    val relatedCerts by viewModel.relatedCertificates.collectAsState()
    val relatedOccs by viewModel.relatedOccupations.collectAsState()

    val isAiMatchingLoading by viewModel.isAiMatchingLoading.collectAsState()
    val isBackendSearching by viewModel.isBackendSearching.collectAsState()
    val aiOccupationMatches by viewModel.aiOccupationMatches.collectAsState()
    val aiCertificateMatches by viewModel.aiCertificateMatches.collectAsState()

    val aiSearchResult by viewModel.aiSearchResult.collectAsState()
    val isAiSearchLoading by viewModel.isAiSearchLoading.collectAsState()

    LaunchedEffect(searchQuery, searchMode, certificatesList.size, occupationsList.size) {
        val trimmed = searchQuery.trim()
        if (trimmed.isNotEmpty() && certificatesList.isEmpty() && occupationsList.isEmpty()) {
            viewModel.triggerAiSearchForNotFound(trimmed, searchMode)
        }
    }

    var showApplyDialog by remember { mutableStateOf(false) }
    var applyDialogMessage by remember { mutableStateOf("") }
    var applyDialogTitle by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val primaryColor = MaterialTheme.colorScheme.primary

    var activeSearchTarget by remember { mutableStateOf(0) } // 0: 搜在招职位, 1: 查岗配证书(searchMode 0), 2: 持证找职业(searchMode 1)
    var isPolicyExpanded by remember { mutableStateOf(false) }

    val currentQueryText = if (activeSearchTarget == 0) jobSearchQuery else searchQuery

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. 顶部渐变横幅 + 复合智能搜索与模式切换卡片 + 热门探索
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.8f)),
                                startY = 0f,
                                endY = size.height
                            )
                        )
                    }
                    .padding(top = 10.dp, bottom = 12.dp, start = 12.dp, end = 12.dp)
            ) {
                Column {
                    // 顶部标题与重置按钮
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "证道职途",
                                    fontSize = 19.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "CertPass",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "名企持证直聘 · 考证职业智能双向检索",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.resetAndReloadDatabase() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🧹", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "重置缓存",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 🌟 复合输入组件 (自适应手机屏幕宽度，无双层冗余搜索框)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            // 上半部：搜索文本输入行
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (activeSearchTarget == 0) MaterialTheme.colorScheme.primary
                                           else if (activeSearchTarget == 1) Color(0xFF6C63FF)
                                           else Color(0xFF009688),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .wrapContentHeight()
                                ) {
                                    if (currentQueryText.isEmpty()) {
                                        Text(
                                            text = if (activeSearchTarget == 0) "搜索在招职位、名企、城市、证书要求..."
                                                   else if (activeSearchTarget == 1) "输入岗位关键词（如：律师、软件工程、会计、教师）"
                                                   else "输入持有证书（如：法考、CPA、教资、软考、一建）",
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                            lineHeight = 17.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = currentQueryText,
                                        onValueChange = {
                                            if (activeSearchTarget == 0) {
                                                viewModel.jobSearchQuery.value = it
                                            } else {
                                                viewModel.searchQuery.value = it
                                                if (it.isNotBlank()) viewModel.addSearchHistory(it)
                                            }
                                        },
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = {
                                            focusManager.clearFocus()
                                            if (activeSearchTarget != 0 && searchQuery.isNotBlank()) {
                                                viewModel.addSearchHistory(searchQuery)
                                            }
                                        }),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                    )
                                }
                                if (currentQueryText.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            if (activeSearchTarget == 0) {
                                                viewModel.jobSearchQuery.value = ""
                                            } else {
                                                viewModel.searchQuery.value = ""
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "清除",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // 下半部：三合一模式切换胶囊组 + 状态指示
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // 胶囊切换组 (自适应紧凑排版，360dp 手机不挤压)
                                Row(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    // 💼 搜在招 胶囊
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (activeSearchTarget == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                activeSearchTarget = 0
                                                viewModel.searchQuery.value = ""
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("💼", fontSize = 10.sp)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "搜在招",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (activeSearchTarget == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // 🎯 配证书 胶囊
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (activeSearchTarget == 1) Color(0xFF6C63FF) else Color.Transparent,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                activeSearchTarget = 1
                                                viewModel.searchMode.value = 0
                                                viewModel.clearDetails()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🎯", fontSize = 10.sp)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "配证书",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (activeSearchTarget == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // 🔍 找职业 胶囊
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (activeSearchTarget == 2) Color(0xFF009688) else Color.Transparent,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                activeSearchTarget = 2
                                                viewModel.searchMode.value = 1
                                                viewModel.clearDetails()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🔍", fontSize = 10.sp)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "找职业",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (activeSearchTarget == 2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // 右侧状态指示
                                if (isBackendSearching) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(11.dp),
                                            strokeWidth = 1.8.dp,
                                            color = if (activeSearchTarget == 1) Color(0xFF6C63FF) else Color(0xFF009688)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "检索中...",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else {
                                    Text(
                                        text = if (activeSearchTarget == 0) "持证直聘"
                                               else if (activeSearchTarget == 1) "门槛排查"
                                               else "拓维图谱",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(end = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 热门探索
                    val hotTags = if (activeSearchTarget == 0) {
                        listOf("🔥 算法工程师", "🔥 全栈开发", "🔥 注册会计师", "🔥 架构师", "🔥 法考法务", "🔥 项目经理", "🔥 建造师")
                    } else if (activeSearchTarget == 1) {
                        listOf("🔥 律师", "🔥 消防工程师", "🔥 软件工程师", "🔥 注册会计师", "🔥 高中教师", "🔥 执业药师")
                    } else {
                        listOf("📜 法律职业资格", "📜 注册会计师CPA", "📜 教师资格证", "📜 软考高项", "📜 一级建造师", "📜 证券从业")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "热门:",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(hotTags) { tag ->
                                val cleanTerm = tag.substringAfter(" ").trim()
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.22f),
                                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (activeSearchTarget == 0) {
                                                viewModel.jobSearchQuery.value = cleanTerm
                                            } else {
                                                viewModel.searchQuery.value = cleanTerm
                                                viewModel.addSearchHistory(cleanTerm)
                                            }
                                        }
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. 📜 国家职业资格分类政策释义 (横向可滑动组件)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📜", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "国家职业资格分类政策释义",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "左右滑动查看 ↔",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEE2E2),
                            border = BorderStroke(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                            modifier = Modifier.width(280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🚨 准入类：无证不得执业",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "关系公共安全与公众利益，国家依法依规实行从业准入，必须先取得证书方可独立执业（如法考、CPA、教资、医师等）。",
                                    fontSize = 10.5.sp,
                                    lineHeight = 15.sp,
                                    color = Color(0xFF7F1D1D)
                                )
                            }
                        }
                    }
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE0F2FE),
                            border = BorderStroke(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                            modifier = Modifier.width(280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "💡 水平评价类：衡量技术水平",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "非强制执业准入，用于评价专业技能素养，供用人单位聘任、定级定薪与职称评定参考（如软考、CATTI等）。",
                                    fontSize = 10.5.sp,
                                    lineHeight = 15.sp,
                                    color = Color(0xFF075985)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. 找职业模式且未输入时，展示岗位类别标签栏
        if (activeSearchTarget == 2 && searchQuery.isEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    items(viewModel.categories) { cat ->
                        val isSelected = cat == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setCategory(cat) },
                            label = { Text(cat, fontWeight = FontWeight.Bold, fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            }
        }

        // 4. 内容区域
        if (activeSearchTarget == 0 || (searchQuery.isEmpty() && activeSearchTarget != 0)) {
            // --- 名企持证直聘专区与筛选栏 ---
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
                    // 标题与统计及重置筛选按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🔥 名企硬性持证直聘专区",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${jobsList.size}个在招",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        val isFilterActive = jobSearchQuery.isNotBlank() || selectedJobCity != "全国" || selectedJobIndustry != "全部"
                        if (isFilterActive) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { viewModel.resetJobFilters() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🔄", fontSize = 10.sp)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "重置筛选",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 📍 城市所在地快捷筛选栏
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📍 城市:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val cities = listOf("全国", "北京", "上海", "深圳", "广州", "杭州", "成都", "武汉", "南京", "长沙")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(cities) { city ->
                                val isSelected = selectedJobCity == city
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectedJobCity.value = city },
                                    label = {
                                        Text(
                                            text = city,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = if (isSelected) null else BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // 💼 岗位行业分类筛选 Chip 栏
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💼 行业:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val industries = listOf("全部", "互联网/IT", "金融/证券", "财会/咨询", "教育/文传", "制造/工程", "医药/健康", "文旅/新兴", "绿色/能源")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(industries) { ind ->
                                val isSelected = selectedJobIndustry == ind
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectedJobIndustry.value = ind },
                                    label = {
                                        Text(
                                            text = ind,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = if (isSelected) null else BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 职位列表项
            if (jobsList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("🔍", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "未找到与条件匹配的在招岗位",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "可尝试清空城市筛选、行业分类或输入其他关键词",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.resetJobFilters() },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("重置所有筛选条件")
                            }
                        }
                    }
                }
            } else {
                items(jobsList, key = { it.id }) { job ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color(job.avatarColor), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = job.company.take(2),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = job.title,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = job.industry,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Text(
                                            text = job.company,
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "•",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                        Text(
                                            text = job.location,
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = job.salary,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFE65100)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.6.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = job.description,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (job.mandatoryCerts.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "🔒 硬性准入:",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD90429)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(job.mandatoryCerts) { cert ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFFF0F3),
                                                border = BorderStroke(0.8.dp, Color(0xFFFFB3C1))
                                            ) {
                                                Text(
                                                    text = cert,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFC9184A),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (job.preferredCerts.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "⭐ 优先加分:",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF023E8A)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(job.preferredCerts) { cert ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFE0F7FA),
                                                border = BorderStroke(0.8.dp, Color(0xFF80DEEA))
                                            ) {
                                                Text(
                                                    text = cert,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF0077B6),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (!isProfileCompleted) {
                                            applyDialogTitle = "⚠️ 个人信息待完善"
                                            applyDialogMessage = "为了向企业 HR 展示真实靠谱的高校科创背景，建议您先前往「个人主页」完善基础学籍与专业档案再行投递！"
                                            showApplyDialog = true
                                        } else {
                                            val recruiter = viewModel.recruiters.value.find { it.id == job.recruiterId }
                                                ?: viewModel.recruiters.value.first()
                                            viewModel.startChatWith(recruiter)
                                            viewModel.currentTab.value = 2 // 求职信使
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("💬 投递简历并直连HR", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- 智能双向检索结果 (配证书 / 找职业) ---
            if (activeSearchTarget == 1) { // 🎯 配证书
                val hasCertMatches = certificatesList.isNotEmpty()
                val hasOccMatches = occupationsList.isNotEmpty()

                if (!hasCertMatches && !hasOccMatches) {
                    item {
                        AiNotFoundOutputView(
                            searchQuery = searchQuery,
                            searchMode = 0,
                            isLoading = isAiSearchLoading,
                            result = aiSearchResult,
                            onRetry = { viewModel.triggerAiSearchForNotFound(searchQuery, 0, forceRefresh = true) }
                        )
                    }
                } else if (hasCertMatches) {
                    items(certificatesList, key = { it.id }) { cert ->
                        val isExpanded = cert.id == selectedCertificateId
                        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                            CertificateCard(
                                certificate = cert,
                                isExpanded = isExpanded,
                                relatedOccs = if (isExpanded) relatedOccs else emptyList(),
                                aiMatches = if (isExpanded) aiCertificateMatches[cert.id] else null,
                                isAiLoading = isAiMatchingLoading && isExpanded,
                                onTriggerAi = { viewModel.triggerAiMatchForCertificate(cert, forceRefresh = true) },
                                onClick = {
                                    if (isExpanded) viewModel.selectCertificate(null)
                                    else viewModel.selectCertificate(cert.id, cert)
                                }
                            )
                        }
                    }
                } else {
                    items(occupationsList, key = { it.id }) { occ ->
                        val isExpanded = occ.id == selectedOccupationId
                        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                            OccupationCard(
                                occupation = occ,
                                isExpanded = isExpanded,
                                relatedCerts = if (isExpanded) relatedCerts else emptyList(),
                                aiMatches = if (isExpanded) aiOccupationMatches[occ.id] else null,
                                isAiLoading = isAiMatchingLoading && isExpanded,
                                onTriggerAi = { viewModel.triggerAiMatchForOccupation(occ, forceRefresh = true) },
                                onClick = {
                                    if (isExpanded) viewModel.selectOccupation(null)
                                    else viewModel.selectOccupation(occ.id, occ)
                                }
                            )
                        }
                    }
                }
            } else { // 🔍 找职业 (activeSearchTarget == 2)
                val hasOccMatches = occupationsList.isNotEmpty()
                val hasCertMatches = certificatesList.isNotEmpty()

                if (!hasOccMatches && !hasCertMatches) {
                    item {
                        AiNotFoundOutputView(
                            searchQuery = searchQuery,
                            searchMode = 1,
                            isLoading = isAiSearchLoading,
                            result = aiSearchResult,
                            onRetry = { viewModel.triggerAiSearchForNotFound(searchQuery, 1, forceRefresh = true) }
                        )
                    }
                } else if (hasOccMatches) {
                    items(occupationsList, key = { it.id }) { occ ->
                        val isExpanded = occ.id == selectedOccupationId
                        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                            OccupationCard(
                                occupation = occ,
                                isExpanded = isExpanded,
                                relatedCerts = if (isExpanded) relatedCerts else emptyList(),
                                aiMatches = if (isExpanded) aiOccupationMatches[occ.id] else null,
                                isAiLoading = isAiMatchingLoading && isExpanded,
                                onTriggerAi = { viewModel.triggerAiMatchForOccupation(occ, forceRefresh = true) },
                                onClick = {
                                    if (isExpanded) viewModel.selectOccupation(null)
                                    else viewModel.selectOccupation(occ.id, occ)
                                }
                            )
                        }
                    }
                } else {
                    items(certificatesList, key = { it.id }) { cert ->
                        val isExpanded = cert.id == selectedCertificateId
                        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                            CertificateCard(
                                certificate = cert,
                                isExpanded = isExpanded,
                                relatedOccs = if (isExpanded) relatedOccs else emptyList(),
                                aiMatches = if (isExpanded) aiCertificateMatches[cert.id] else null,
                                isAiLoading = isAiMatchingLoading && isExpanded,
                                onTriggerAi = { viewModel.triggerAiMatchForCertificate(cert, forceRefresh = true) },
                                onClick = {
                                    if (isExpanded) viewModel.selectCertificate(null)
                                    else viewModel.selectCertificate(cert.id, cert)
                                }
                            )
                        }
                    }
                }
            }
        }
    }


    if (showApplyDialog) {
        AlertDialog(
            onDismissRequest = { showApplyDialog = false },
            title = { Text(applyDialogTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = { Text(applyDialogMessage, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        showApplyDialog = false
                        if (applyDialogTitle.contains("⚠️")) {
                            viewModel.currentTab.value = 3 // 跳转到个人主页
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (applyDialogTitle.contains("⚠️")) "去完善" else "确定", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                if (applyDialogTitle.contains("⚠️")) {
                    TextButton(onClick = { showApplyDialog = false }) {
                        Text("取消", fontSize = 12.sp)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}



data class MatchingResult(
    val score: Int,
    val level: String,
    val levelColor: Color,
    val desc: String
)






