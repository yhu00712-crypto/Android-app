package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.ApiTestState
import com.example.ui.viewmodel.CareerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepSeekApiDialog(
    viewModel: CareerViewModel
) {
    val currentApiKey by viewModel.deepSeekApiKey.collectAsState()
    val currentBaseUrl by viewModel.deepSeekBaseUrl.collectAsState()
    val currentModel by viewModel.deepSeekModel.collectAsState()
    val testState by viewModel.apiTestState.collectAsState()

    var inputApiKey by remember { mutableStateOf(currentApiKey) }
    var inputBaseUrl by remember { mutableStateOf(currentBaseUrl) }
    var inputModel by remember { mutableStateOf(currentModel) }
    var isKeyVisible by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = { viewModel.closeApiConfigDialog() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 顶部标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E88E5).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🐳", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DeepSeek API 接口配置",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "赋能简历润色专家与生涯规划师",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.closeApiConfigDialog() }) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // 可滚动内容区域
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // DeepSeek API Key 输入
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DeepSeek API Key *",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = {
                                    clipboardManager.getText()?.text?.let { clipText ->
                                        inputApiKey = clipText.trim()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("一键粘贴", fontSize = 11.sp)
                            }
                        }

                        OutlinedTextField(
                            value = inputApiKey,
                            onValueChange = { inputApiKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("sk-xxxxxxxxxxxxxxxxxxxxxxxx") },
                            singleLine = true,
                            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                    Icon(
                                        if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isKeyVisible) "隐藏" else "显示"
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💡 密钥仅安全存储于您当前手机本地，绝不上传第三方服务器。",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // API Base URL
                    Column {
                        Text(
                            text = "API 接口地址 (Base URL)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = inputBaseUrl,
                            onValueChange = { inputBaseUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("https://api.deepseek.com") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SuggestionChip(
                                onClick = { inputBaseUrl = "https://api.deepseek.com" },
                                label = { Text("官方默认", fontSize = 10.sp) }
                            )
                            SuggestionChip(
                                onClick = { inputBaseUrl = "https://api.siliconflow.cn/v1" },
                                label = { Text("硅基流动", fontSize = 10.sp) }
                            )
                            SuggestionChip(
                                onClick = { inputBaseUrl = "https://openrouter.ai/api/v1" },
                                label = { Text("OpenRouter", fontSize = 10.sp) }
                            )
                        }
                    }

                    // 模型选择
                    Column {
                        Text(
                            text = "选择 DeepSeek 模型版本",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { inputModel = "deepseek-chat" },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (inputModel == "deepseek-chat")
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = if (inputModel == "deepseek-chat")
                                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                else null
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⚡ V3", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("通用极速", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text("deepseek-chat", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("适合简历润色/快速对话", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { inputModel = "deepseek-reasoner" },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (inputModel == "deepseek-reasoner")
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = if (inputModel == "deepseek-reasoner")
                                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                else null
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🧠 R1", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("深度思考", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text("deepseek-reasoner", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("含CoT思考链/规划极强", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // 连通性测试按钮与反馈展示
                    Column {
                        OutlinedButton(
                            onClick = {
                                viewModel.testDeepSeekConnection(
                                    apiKey = inputApiKey,
                                    baseUrl = inputBaseUrl,
                                    model = inputModel
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = testState !is ApiTestState.Testing
                        ) {
                            if (testState is ApiTestState.Testing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("正在测试连接 DeepSeek API...", fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("🔍 测试 API 连通性", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // 测试结果提示卡片
                        when (val state = testState) {
                            is ApiTestState.Success -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(state.message, color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            is ApiTestState.Error -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(state.message, color = Color(0xFFC62828), fontSize = 11.sp)
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // 底部确认与取消按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.closeApiConfigDialog() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("稍后再说")
                    }

                    Button(
                        onClick = {
                            viewModel.saveDeepSeekConfig(
                                apiKey = inputApiKey,
                                baseUrl = inputBaseUrl,
                                model = inputModel,
                                provider = "DEEPSEEK"
                            )
                        },
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("保存并立即启用", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
