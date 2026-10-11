package com.example.ui.enterprise

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DeepSeekApiDialog
import com.example.ui.viewmodel.CareerViewModel

/**
 * 🏢 企业端主入口容器 (EnterpriseMainScreen)
 * 包含企业工作台、证书人才库、职位管理、候选人沟通、企业中心五大模块
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterpriseMainScreen(
    viewModel: CareerViewModel,
    modifier: Modifier = Modifier
) {
    val enterpriseTab by viewModel.enterpriseCurrentTab.collectAsState()
    val enterpriseProfile by viewModel.enterpriseProfile.collectAsState()
    val conversations by viewModel.enterpriseConversations.collectAsState()
    val totalUnread = conversations.count { it.hasUnread }

    BackHandler(enabled = enterpriseTab != 0) {
        viewModel.enterpriseCurrentTab.value = 0
    }

    var openPostJobInManagement by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = enterpriseProfile.shortName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "企业认证",
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "企业招聘端",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    // 一键切换为个人求职者端
                    FilledTonalButton(
                        onClick = { viewModel.switchRole() },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("switch_to_personal_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "切换为个人端",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("求职端", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // API 接口配置入口 (由原齿轮图标改成专门的 API 接口按钮)
                    OutlinedButton(
                        onClick = { viewModel.openApiConfigDialog() },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("enterprise_api_interface_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Api,
                            contentDescription = "API接口",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("API接口", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = enterpriseTab == 0,
                    onClick = { viewModel.enterpriseCurrentTab.value = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "工作台") },
                    label = { Text("工作台", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = enterpriseTab == 1,
                    onClick = { viewModel.enterpriseCurrentTab.value = 1 },
                    icon = { Icon(Icons.Default.Radar, contentDescription = "找人才") },
                    label = { Text("找人才", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = enterpriseTab == 2,
                    onClick = { viewModel.enterpriseCurrentTab.value = 2 },
                    icon = { Icon(Icons.Default.WorkOutline, contentDescription = "职位管理") },
                    label = { Text("职位管理", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = enterpriseTab == 3,
                    onClick = { viewModel.enterpriseCurrentTab.value = 3 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (totalUnread > 0) {
                                    Badge { Text("$totalUnread") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Forum, contentDescription = "沟通")
                        }
                    },
                    label = { Text("沟通", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = enterpriseTab == 4,
                    onClick = { viewModel.enterpriseCurrentTab.value = 4 },
                    icon = { Icon(Icons.Default.Business, contentDescription = "企业中心") },
                    label = { Text("企业中心", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (enterpriseTab) {
                0 -> EnterpriseWorkbenchScreen(
                    viewModel = viewModel,
                    onNavigateToTab = { tabIndex -> viewModel.enterpriseCurrentTab.value = tabIndex },
                    onOpenPostJobDialog = {
                        viewModel.enterpriseCurrentTab.value = 2
                        openPostJobInManagement = true
                    }
                )
                1 -> EnterpriseTalentScreen(
                    viewModel = viewModel,
                    onNavigateToChatWithCandidate = { candidateId ->
                        viewModel.enterpriseCurrentTab.value = 3
                    }
                )
                2 -> EnterpriseJobManagementScreen(
                    viewModel = viewModel,
                    showPostJobDialogDirectly = openPostJobInManagement,
                    onResetPostJobDialogFlag = { openPostJobInManagement = false }
                )
                3 -> EnterpriseChatScreen(
                    viewModel = viewModel
                )
                4 -> EnterpriseProfileScreen(
                    viewModel = viewModel
                )
            }

            // 全局 DeepSeek / 大模型 API 配置弹窗
            val showApiConfigDialog by viewModel.showApiConfigDialog.collectAsState()
            if (showApiConfigDialog) {
                DeepSeekApiDialog(viewModel = viewModel)
            }
        }
    }
}
