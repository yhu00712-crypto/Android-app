package com.example.ui.enterprise

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.EnterpriseJobPosting
import com.example.ui.viewmodel.CareerViewModel

/**
 * 🏢 企业端 TAB 2：职位管理 (在招职位/发布新职位)
 */
@Composable
fun EnterpriseJobManagementScreen(
    viewModel: CareerViewModel,
    showPostJobDialogDirectly: Boolean = false,
    onResetPostJobDialogFlag: () -> Unit = {}
) {
    val jobs by viewModel.enterpriseJobs.collectAsState()
    var isPostingJob by remember { mutableStateOf(false) }

    LaunchedEffect(showPostJobDialogDirectly) {
        if (showPostJobDialogDirectly) {
            isPostingJob = true
            onResetPostJobDialogFlag()
        }
    }

    val onlineJobs = jobs.filter { it.isOnline }
    val offlineJobs = jobs.filter { !it.isOnline }

    var selectedTab by remember { mutableStateOf(0) } // 0: 在招中, 1: 已暂停

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isPostingJob = true },
                containerColor = Color(0xFF1565C0),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("post_job_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "发布职位")
                Spacer(modifier = Modifier.width(6.dp))
                Text("发布新职位", fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 顶部统计
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("在招职位", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${onlineJobs.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1565C0))
                    }
                    VerticalDivider(modifier = Modifier.height(30.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("已暂停", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${offlineJobs.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF757575))
                    }
                    VerticalDivider(modifier = Modifier.height(30.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("已收简历", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${jobs.sumOf { it.applicantCount }}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00897B))
                    }
                    VerticalDivider(modifier = Modifier.height(30.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("总曝光", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${jobs.sumOf { it.viewCount }}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE65100))
                    }
                }
            }

            // Tab 切换：在招中 vs 已暂停
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("在招中 (${onlineJobs.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("已暂停 (${offlineJobs.size})", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val currentDisplayJobs = if (selectedTab == 0) onlineJobs else offlineJobs

            if (currentDisplayJobs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.WorkOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedTab == 0) "暂无在招职位，点击右下角发布" else "暂无已暂停职位",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                ) {
                    items(currentDisplayJobs, key = { it.id }) { job ->
                        EnterpriseJobCard(
                            job = job,
                            onToggleStatus = { viewModel.toggleJobStatus(job.id) }
                        )
                    }
                }
            }
        }
    }

    // 发布新职位弹窗
    if (isPostingJob) {
        PostNewJobDialog(
            onDismiss = { isPostingJob = false },
            onConfirmPost = { title, dept, salary, loc, count, mandCerts, prefCerts, exp, edu, desc ->
                viewModel.postEnterpriseJob(
                    title = title,
                    department = dept,
                    salary = salary,
                    location = loc,
                    headCount = count,
                    mandatoryCerts = mandCerts,
                    preferredCerts = prefCerts,
                    experienceReq = exp,
                    educationReq = edu,
                    jobDescription = desc
                )
                isPostingJob = false
            }
        )
    }
}

@Composable
private fun EnterpriseJobCard(
    job: EnterpriseJobPosting,
    onToggleStatus: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 职位名称与薪资
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${job.department} · ${job.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = job.salary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFE65100)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 硬性持证要求
            if (job.mandatoryCerts.isNotEmpty()) {
                Surface(
                    color = Color(0xFFFFF8E1),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.8.dp, Color(0xFFFFB300).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏅 准入持证：", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFB78103))
                        Text(
                            text = job.mandatoryCerts.joinToString("、"),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF795548),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 经验学历与人数
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${job.educationReq} · ${job.experienceReq} · 招${job.headCount}人",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = job.jobDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Spacer(modifier = Modifier.height(10.dp))

            // 底部数据与操作
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF1565C0))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("投递 ${job.applicantCount} 份", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF757575))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("浏览 ${job.viewCount} 次", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                OutlinedButton(
                    onClick = onToggleStatus,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (job.isOnline) Color(0xFFC62828) else Color(0xFF2E7D32)
                    )
                ) {
                    Text(
                        text = if (job.isOnline) "暂停招聘" else "恢复在招",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * ➕ 发布新职位弹窗表单
 */
@Composable
private fun PostNewJobDialog(
    onDismiss: () -> Unit,
    onConfirmPost: (
        title: String,
        department: String,
        salary: String,
        location: String,
        headCount: Int,
        mandatoryCerts: List<String>,
        preferredCerts: List<String>,
        experienceReq: String,
        educationReq: String,
        jobDescription: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("云计算与技术研发中心") }
    var salary by remember { mutableStateOf("20k-35k") }
    var location by remember { mutableStateOf("深圳 · 南山") }
    var headCountStr by remember { mutableStateOf("2") }
    var experienceReq by remember { mutableStateOf("应届生 / 1-3年") }
    var educationReq by remember { mutableStateOf("本科及以上") }
    var jobDescription by remember { mutableStateOf("负责核心业务系统架构演进，参与高并发微服务与端侧体验深度调优，推动国家职业资格持证技术规范落地。") }

    var selectedMandatoryCerts by remember { mutableStateOf(listOf("信息系统项目管理师 (软考高级)")) }
    var customCertInput by remember { mutableStateOf("") }

    val presetCerts = listOf(
        "软件设计师 (软考中级)",
        "系统架构设计师 (软考高级)",
        "信息系统项目管理师 (软考高级)",
        "国家统一法律职业资格 (法考A证)",
        "注册会计师 CPA (全科)",
        "CKA (Kubernetes管理员)",
        "一级建造师",
        "一级造价工程师",
        "PMP 项目管理",
        "高级中学教师资格证"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 顶部标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("发布新职位", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("设置硬性持证门槛，精准直聘名校持证英才", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("职位名称 (例：资深云原生研发架构师)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = salary,
                                onValueChange = { salary = it },
                                label = { Text("薪资范围 (例：25k-40k)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                label = { Text("工作城市") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = department,
                                onValueChange = { department = it },
                                label = { Text("招聘部门") },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = headCountStr,
                                onValueChange = { headCountStr = it },
                                label = { Text("招聘人数") },
                                modifier = Modifier.weight(0.8f),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        Text(
                            text = "🏅 硬性准入考证要求 (本平台核心特色)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "设置后，系统将自动在双向图谱中加权展示，并对持证求职者精准推送：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // 快捷点选证书
                        presetCerts.chunked(2).forEach { pair ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                pair.forEach { cert ->
                                    val isSelected = selectedMandatoryCerts.contains(cert)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedMandatoryCerts = if (isSelected) {
                                                selectedMandatoryCerts - cert
                                            } else {
                                                selectedMandatoryCerts + cert
                                            }
                                        },
                                        label = { Text(cert, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFFFF8E1),
                                            selectedLabelColor = Color(0xFFB78103)
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // 自定义添加证书
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = customCertInput,
                                onValueChange = { customCertInput = it },
                                placeholder = { Text("手动输入其他证书名称") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(onClick = {
                                if (customCertInput.isNotBlank()) {
                                    selectedMandatoryCerts = selectedMandatoryCerts + customCertInput.trim()
                                    customCertInput = ""
                                }
                            }) {
                                Text("添加")
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = jobDescription,
                            onValueChange = { jobDescription = it },
                            label = { Text("岗位职责与任职要求") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirmPost(
                                    title,
                                    department,
                                    salary,
                                    location,
                                    headCountStr.toIntOrNull() ?: 1,
                                    selectedMandatoryCerts,
                                    listOf("大学英语六级 CET-6"),
                                    experienceReq,
                                    educationReq,
                                    jobDescription
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) {
                        Text("立即上线招聘", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
