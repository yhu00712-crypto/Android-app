package com.example.ui.enterprise

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.EnterpriseCandidate
import com.example.ui.viewmodel.CareerViewModel

/**
 * 🏢 企业端 TAB 1：证书人才库 (持证直聘人才雷达)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterpriseTalentScreen(
    viewModel: CareerViewModel,
    onNavigateToChatWithCandidate: (String) -> Unit = {}
) {
    val candidates by viewModel.enterpriseCandidates.collectAsState()
    val searchQuery by viewModel.enterpriseSearchTalentQuery.collectAsState()
    val selectedCertFilter by viewModel.enterpriseSelectedCertFilter.collectAsState()

    var candidateForDetail by remember { mutableStateOf<EnterpriseCandidate?>(null) }
    var candidateForInterview by remember { mutableStateOf<EnterpriseCandidate?>(null) }

    val certFilterOptions = listOf(
        "全部",
        "软考高级/系统架构",
        "注册会计师 CPA",
        "法律职业资格 A证",
        "CKA 云原生",
        "一级建造/造价师",
        "PMP 项目管理",
        "高级教师资格证"
    )

    val filteredCandidates = remember(candidates, searchQuery, selectedCertFilter) {
        candidates.filter { cand ->
            val matchSearch = searchQuery.isBlank() ||
                    cand.name.contains(searchQuery, ignoreCase = true) ||
                    cand.school.contains(searchQuery, ignoreCase = true) ||
                    cand.targetPosition.contains(searchQuery, ignoreCase = true) ||
                    cand.mandatoryCerts.any { it.contains(searchQuery, ignoreCase = true) } ||
                    cand.skillTags.any { it.contains(searchQuery, ignoreCase = true) }

            val matchFilter = when (selectedCertFilter) {
                "全部" -> true
                "软考高级/系统架构" -> cand.mandatoryCerts.any { it.contains("软考") || it.contains("架构") }
                "注册会计师 CPA" -> cand.mandatoryCerts.any { it.contains("CPA") || it.contains("会计") }
                "法律职业资格 A证" -> cand.mandatoryCerts.any { it.contains("法考") || it.contains("法律") }
                "CKA 云原生" -> cand.mandatoryCerts.any { it.contains("CKA") || it.contains("Kubernetes") }
                "一级建造/造价师" -> cand.mandatoryCerts.any { it.contains("建造") || it.contains("造价") }
                "PMP 项目管理" -> cand.mandatoryCerts.any { it.contains("PMP") || it.contains("项目管理") }
                "高级教师资格证" -> cand.mandatoryCerts.any { it.contains("教师") }
                else -> true
            }
            matchSearch && matchFilter
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 搜索框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.enterpriseSearchTalentQuery.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_talent_input"),
                    placeholder = { Text("搜索姓名、院校、国家证书、技能关键词...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.enterpriseSearchTalentQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "清空")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 证书筛选横向胶囊
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(certFilterOptions) { filter ->
                        val isSelected = selectedCertFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.enterpriseSelectedCertFilter.value = filter },
                            label = { Text(filter, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "共发现 ${filteredCandidates.size} 位符合国家资格认证的人才",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "按证书契合度优先排序",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (filteredCandidates.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "未检索到匹配的持证人才",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "可尝试清空搜索关键词或更换证书筛选类别",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredCandidates, key = { it.id }) { candidate ->
                    CandidateFullCard(
                        candidate = candidate,
                        onChatClick = {
                            viewModel.activeEnterpriseCandidate.value = candidate
                            onNavigateToChatWithCandidate(candidate.id)
                        },
                        onInviteClick = {
                            candidateForInterview = candidate
                        },
                        onBookmarkClick = {
                            viewModel.bookmarkCandidate(candidate.id)
                        },
                        onDetailClick = {
                            candidateForDetail = candidate
                        }
                    )
                }
            }
        }
    }

    // 候选人简历详情弹窗
    candidateForDetail?.let { candidate ->
        CandidateDetailDialog(
            candidate = candidate,
            onDismiss = { candidateForDetail = null },
            onStartChat = {
                candidateForDetail = null
                viewModel.activeEnterpriseCandidate.value = candidate
                onNavigateToChatWithCandidate(candidate.id)
            },
            onInvite = {
                candidateForDetail = null
                candidateForInterview = candidate
            }
        )
    }

    // 发送面试邀请弹窗
    candidateForInterview?.let { candidate ->
        InterviewInviteDialog(
            candidate = candidate,
            onDismiss = { candidateForInterview = null },
            onConfirmInvite = { pos, time, type, link, notes ->
                viewModel.sendInterviewInvite(
                    candidateId = candidate.id,
                    positionTitle = pos,
                    time = time,
                    type = type,
                    linkOrLoc = link,
                    notes = notes
                )
                candidateForInterview = null
            }
        )
    }
}

@Composable
private fun CandidateFullCard(
    candidate: EnterpriseCandidate,
    onChatClick: () -> Unit = {},
    onInviteClick: () -> Unit = {},
    onBookmarkClick: () -> Unit = {},
    onDetailClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onDetailClick)
            .testTag("candidate_card_${candidate.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // 顶部：姓名、求职意向与匹配度评分
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(candidate.avatarColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = candidate.name.take(1),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = candidate.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = candidate.jobStatus,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${candidate.school} · ${candidate.degree} · ${candidate.graduationYear}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 契合度徽章
                Surface(
                    color = Color(0xFF1565C0).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1565C0).copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${candidate.matchScore}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1565C0)
                        )
                        Text(
                            text = "人岗契合",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = Color(0xFF1565C0)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 期望岗位与期望薪资
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "期望：${candidate.targetPosition}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = candidate.expectedSalary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 🏅 国家权威证书直展 (核心亮点)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                candidate.mandatoryCerts.forEach { cert ->
                    Surface(
                        color = Color(0xFFFFF9C4).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏅", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cert,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8D6E63)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "国家认证·已核验",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 亮点摘要 (STAR 法则)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = candidate.highlights,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 技能标签
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                candidate.skillTags.forEach { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = tag,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Spacer(modifier = Modifier.height(12.dp))

            // 底部操作栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 收藏按钮
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (candidate.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "收藏候选人",
                        tint = if (candidate.isBookmarked) Color(0xFFFFA000) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onInviteClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("邀约面试", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onChatClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1565C0)
                        )
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("立即开聊", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 候选人完整简历画像弹窗
 */
@Composable
private fun CandidateDetailDialog(
    candidate: EnterpriseCandidate,
    onDismiss: () -> Unit,
    onStartChat: () -> Unit,
    onInvite: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 顶部关闭与标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "候选人在校履历档案",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "名校持证直通",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(candidate.avatarColor)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = candidate.name.take(1),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(candidate.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${candidate.school} · ${candidate.degree}", style = MaterialTheme.typography.bodySmall)
                                    Text("期望：${candidate.targetPosition} (${candidate.expectedSalary})", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100))
                                }
                            }
                        }
                    }

                    item {
                        Text("国家法定考证认证 (硬性准入/优选加分)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        candidate.mandatoryCerts.forEach { cert ->
                            Surface(
                                color = Color(0xFFFFFDE7),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFB300)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🏅", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(cert, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    item {
                        Text("STAR 实战项目与业务沉淀", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = candidate.workOrProjectSummary,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    item {
                        Text("核心竞争力亮点", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = candidate.highlights,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onInvite,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("发起面试邀约", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onStartChat,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) {
                        Text("立即开聊沟通", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 发起面试邀约弹窗
 */
@Composable
private fun InterviewInviteDialog(
    candidate: EnterpriseCandidate,
    onDismiss: () -> Unit,
    onConfirmInvite: (position: String, time: String, type: String, link: String, notes: String) -> Unit
) {
    var positionTitle by remember { mutableStateOf(candidate.targetPosition) }
    var interviewTime by remember { mutableStateOf("2026-10-16 14:30") }
    var interviewType by remember { mutableStateOf("线上视频面试 (AI智联会议)") }
    var linkOrLoc by remember { mutableStateOf("https://meeting.starstream.ai/room/88930198") }
    var notes by remember { mutableStateOf("请提前准备10分钟项目架构作品展示，并备齐相关国家职业资格证书原件电子件。") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "发送面试邀约函",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "受邀人：${candidate.name} (${candidate.school})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = positionTitle,
                    onValueChange = { positionTitle = it },
                    label = { Text("面试职位") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = interviewTime,
                    onValueChange = { interviewTime = it },
                    label = { Text("面试时间") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = interviewType,
                    onValueChange = { interviewType = it },
                    label = { Text("面试形式") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = linkOrLoc,
                    onValueChange = { linkOrLoc = it },
                    label = { Text("会议链接 / 现场地点") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("面试备忘 / 证书核查要求") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            onConfirmInvite(positionTitle, interviewTime, interviewType, linkOrLoc, notes)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) {
                        Text("确认发送邀约", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
