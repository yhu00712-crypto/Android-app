package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.StaticDataStore
import com.example.data.model.*
import com.example.data.skills.CampusToCareerSkill
import com.example.data.skills.CareerPlannerSkill
import com.example.ui.theme.AnimeSkinTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull

// API 连通性测试状态
sealed class ApiTestState {
    object Idle : ApiTestState()
    object Testing : ApiTestState()
    data class Success(val message: String) : ApiTestState()
    data class Error(val message: String) : ApiTestState()
}

// 聊天消息实体
data class ChatMessage(
    val id: String,
    val sender: String, // "RECRUITER" or "USER"
    val content: String,
    val timestamp: String,
    val isRead: Boolean
)

// 模拟面试官/HR人设
data class Recruiter(
    val id: String,
    val name: String,
    val company: String,
    val avatarColor: Long,
    val targetOccupationId: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val hasUnread: Boolean
)

// 大学生生涯互助 (组队与咨询) 帖子实体
data class CoCreatePost(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorMajor: String,
    val authorAvatarColor: Long,
    val type: String, // "比赛组队" or "实习咨询"
    val title: String,
    val content: String,
    val targetPlatform: String, // e.g. "挑战杯国赛", "数学建模美赛", "字节跳动运营内推", "四大会计所审计咨询"
    val relatedCertTags: List<String>, // 相关关联证书标签
    val neededRoles: String, // 招募角色/咨询方向
    val memberCount: Int,
    val memberMax: Int,
    val timestamp: String,
    val isJoined: Boolean = false
)

// 招聘岗位实体
data class RecruitingJob(
    val id: String,
    val title: String,
    val company: String,
    val salary: String,
    val location: String,
    val avatarColor: Long,
    val mandatoryCerts: List<String>,
    val preferredCerts: List<String>,
    val description: String,
    val recruiterId: String,
    val industry: String = "互联网/IT"
)

// 个人简历工作/项目经历实体
data class ResumeWorkExp(
    val id: String,
    val company: String,
    val role: String,
    val period: String,
    val description: String
)

class CareerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val dao = database.careerCertDao()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // 清理旧表数据并重置写入最新、高精度的真实法定对应关系
            dao.clearRelations()
            dao.clearOccupations()
            dao.clearCertificates()
            dao.insertCertificates(StaticDataStore.getCertificates())
            dao.insertOccupations(StaticDataStore.getOccupations())
            dao.insertRelations(StaticDataStore.getRelations())
        }
    }

    // --- DeepSeek / AI 大模型 API 配置管理 ---
    private val aiPrefs = application.getSharedPreferences("ai_settings_prefs", Context.MODE_PRIVATE)

    val aiProvider = MutableStateFlow(aiPrefs.getString("ai_provider", "DEEPSEEK") ?: "DEEPSEEK")
    val deepSeekApiKey: MutableStateFlow<String> = MutableStateFlow(
        aiPrefs.getString("deepseek_api_key", "")?.ifBlank {
            com.example.data.network.ApiConfigHelper.getDeepSeekKey()
        } ?: com.example.data.network.ApiConfigHelper.getDeepSeekKey()
    )
    val deepSeekBaseUrl = MutableStateFlow(aiPrefs.getString("deepseek_base_url", "https://api.deepseek.com") ?: "https://api.deepseek.com")
    val deepSeekModel = MutableStateFlow(aiPrefs.getString("deepseek_model", "deepseek-chat") ?: "deepseek-chat")

    // 控制 API 设置界面调出 (默认 false，不遮挡主界面，需要时可在设置或聊天中主动调出)
    val showApiConfigDialog = MutableStateFlow(false)
    val apiTestState = MutableStateFlow<ApiTestState>(ApiTestState.Idle)

    fun openApiConfigDialog() {
        apiTestState.value = ApiTestState.Idle
        showApiConfigDialog.value = true
    }

    fun closeApiConfigDialog() {
        showApiConfigDialog.value = false
    }

    fun saveDeepSeekConfig(apiKey: String, baseUrl: String, model: String, provider: String = "DEEPSEEK") {
        val cleanKey = apiKey.trim()
        val cleanUrl = if (baseUrl.isBlank()) "https://api.deepseek.com" else baseUrl.trim().trimEnd('/')
        val cleanModel = if (model.isBlank()) "deepseek-chat" else model.trim()

        deepSeekApiKey.value = cleanKey
        deepSeekBaseUrl.value = cleanUrl
        deepSeekModel.value = cleanModel
        aiProvider.value = provider

        aiPrefs.edit()
            .putString("deepseek_api_key", cleanKey)
            .putString("deepseek_base_url", cleanUrl)
            .putString("deepseek_model", cleanModel)
            .putString("ai_provider", provider)
            .apply()

        showApiConfigDialog.value = false
    }

    fun testDeepSeekConnection(apiKey: String, baseUrl: String, model: String) {
        val cleanKey = apiKey.trim()
        val cleanUrl = if (baseUrl.isBlank()) "https://api.deepseek.com" else baseUrl.trim().trimEnd('/')
        val cleanModel = if (model.isBlank()) "deepseek-chat" else model.trim()

        if (cleanKey.isEmpty()) {
            apiTestState.value = ApiTestState.Error("API Key 不能为空，请输入您的 DeepSeek 密钥")
            return
        }

        apiTestState.value = ApiTestState.Testing

        viewModelScope.launch(Dispatchers.IO) {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val jsonPayload = """
                {
                    "model": "$cleanModel",
                    "messages": [
                        {"role": "user", "content": "请只回复两个字：OK"}
                    ],
                    "max_tokens": 10
                }
            """.trimIndent()

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = okhttp3.RequestBody.Companion.create(mediaType, jsonPayload)
            val fullUrl = if (cleanUrl.endsWith("/chat/completions")) cleanUrl else "$cleanUrl/chat/completions"

            val request = okhttp3.Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        apiTestState.value = ApiTestState.Success("连接测试成功！DeepSeek API 验证通过 🚀")
                    } else {
                        apiTestState.value = ApiTestState.Error("HTTP ${response.code}: $bodyString")
                    }
                }
            } catch (e: Exception) {
                apiTestState.value = ApiTestState.Error("连接失败: ${e.message ?: "网络超时"}")
            }
        }
    }

    // --- 🏢 用户身份体系 (企业招聘端 vs 个人求职端) ---
    private val rolePrefs = application.getSharedPreferences("user_role_prefs", Context.MODE_PRIVATE)

    val userRoleMode: MutableStateFlow<UserRoleMode> = MutableStateFlow(
        when (rolePrefs.getString("saved_user_role", null)) {
            "ENTERPRISE" -> UserRoleMode.ENTERPRISE
            "PERSONAL" -> UserRoleMode.PERSONAL
            else -> UserRoleMode.UNSELECTED
        }
    )

    // 控制身份选择弹窗显示 (首次进入或主动切换时弹出)
    val showRoleSelectionDialog = MutableStateFlow(
        userRoleMode.value == UserRoleMode.UNSELECTED
    )

    fun selectRole(role: UserRoleMode, rememberChoice: Boolean = true) {
        userRoleMode.value = role
        showRoleSelectionDialog.value = false
        if (rememberChoice && role != UserRoleMode.UNSELECTED) {
            rolePrefs.edit().putString("saved_user_role", role.name).apply()
        }
    }

    fun openRoleSelection() {
        showRoleSelectionDialog.value = true
    }

    fun switchRole() {
        val nextRole = if (userRoleMode.value == UserRoleMode.ENTERPRISE) UserRoleMode.PERSONAL else UserRoleMode.ENTERPRISE
        selectRole(nextRole, rememberChoice = true)
    }

    // --- 🏢 企业端状态管理 ---
    val enterpriseCurrentTab = MutableStateFlow(0) // 0:工作台, 1:找人才, 2:职位管理, 3:沟通, 4:企业中心

    val enterpriseProfile = MutableStateFlow(
        EnterpriseCompanyProfile(
            companyName = "星澜数智科技 (深圳) 有限公司 · 云计算与智能产业集群",
            shortName = "星澜智科",
            industry = "互联网 / 基础软件 / 人工智能",
            scale = "10000人以上",
            financeStage = "已上市",
            address = "深圳市南山区粤海街道高新南十二路88号星澜智谷研发中心A座",
            creditCode = "91440300MA5H8X9Q2L",
            isVerified = true,
            recruiterName = "林思远",
            recruiterTitle = "资深技术招聘专家 / HRBP",
            recruiterWorkNo = "XL-HR-88206",
            slogan = "数智赋能未来，直聘持证专业菁英",
            welfareTags = listOf("六险一金", "持证高额技能津贴", "全额公积金", "双休弹性打卡", "免费早晚餐", "租房安居补贴", "资深AI架构师导师制")
        )
    )

    fun updateRecruiterProfile(
        name: String,
        title: String,
        workNo: String,
        department: String = "",
        phone: String = "",
        email: String = "",
        welcomeMsg: String = ""
    ) {
        val current = enterpriseProfile.value
        enterpriseProfile.value = current.copy(
            recruiterName = name.ifBlank { current.recruiterName },
            recruiterTitle = title.ifBlank { current.recruiterTitle },
            recruiterWorkNo = workNo.ifBlank { current.recruiterWorkNo },
            recruiterDepartment = if (department.isNotBlank()) department else current.recruiterDepartment,
            recruiterPhone = if (phone.isNotBlank()) phone else current.recruiterPhone,
            recruiterEmail = if (email.isNotBlank()) email else current.recruiterEmail,
            recruiterWelcomeMsg = if (welcomeMsg.isNotBlank()) welcomeMsg else current.recruiterWelcomeMsg
        )
    }

    fun updateCompanyProfile(
        companyName: String,
        shortName: String,
        industry: String,
        scale: String,
        financeStage: String,
        address: String,
        creditCode: String,
        slogan: String,
        welfareTags: List<String>
    ) {
        val current = enterpriseProfile.value
        enterpriseProfile.value = current.copy(
            companyName = companyName.ifBlank { current.companyName },
            shortName = shortName.ifBlank { current.shortName },
            industry = industry.ifBlank { current.industry },
            scale = scale.ifBlank { current.scale },
            financeStage = financeStage.ifBlank { current.financeStage },
            address = address.ifBlank { current.address },
            creditCode = creditCode.ifBlank { current.creditCode },
            slogan = slogan.ifBlank { current.slogan },
            welfareTags = if (welfareTags.isNotEmpty()) welfareTags else current.welfareTags
        )
    }

    // 企业端人才库
    val enterpriseCandidates = MutableStateFlow(
        listOf(
            EnterpriseCandidate(
                id = "CAND_ZCH",
                name = "张楚航",
                avatarColor = 0xFF1E88E5,
                targetPosition = "Android 研发架构师 / 云原生全栈工程师",
                expectedSalary = "18k-28k",
                school = "华中科技大学",
                degree = "软件工程 · 本科在读",
                graduationYear = "2026 届应届",
                jobStatus = "离校实习 · 随时到岗",
                matchScore = 98,
                mandatoryCerts = listOf("信息系统项目管理师 (软考高级)", "法律职业资格 A 证", "软件设计师 (软考中级)"),
                skillTags = listOf("Kotlin", "Jetpack Compose", "Room / SQLite", "Spring Boot", "高并发微服务", "系统架构设计"),
                highlights = "软考高级+法考A证双持证；第十四届挑战杯全国金奖；曾在头部数智云中台实习，主导微服务重构与Room本地数据库优化，高并发吞吐提升35%。",
                workOrProjectSummary = "拥有端到端全链路研发架构经验，带领5人团队研发「基于知识图谱的职场考证双向检索系统」获省级一等奖。",
                isBookmarked = false,
                isInvited = false
            ),
            EnterpriseCandidate(
                id = "CAND_LL",
                name = "李林",
                avatarColor = 0xFF7B1FA2,
                targetPosition = "高级财务合规总监 / 投行IPO分析师",
                expectedSalary = "22k-35k",
                school = "上海交通大学",
                degree = "会计学 · 硕士",
                graduationYear = "2025 届应届",
                jobStatus = "在职看机会 · 1个月内到岗",
                matchScore = 96,
                mandatoryCerts = listOf("注册会计师 CPA (专业阶段全科)", "证券从业资格", "基金从业资格"),
                skillTags = listOf("CPA全科", "财务尽职调查", "IPO合规", "合并报表审计", "税务筹划"),
                highlights = "CPA一次性全科通过；四大会计师事务所核心项目实习；主导两家科技拟IPO企业底稿尽调与内控合规审计。",
                workOrProjectSummary = "精通中国会计准则与国际IFRS，具备深厚资本市场财务建模与数据分析实操功底。",
                isBookmarked = false,
                isInvited = false
            ),
            EnterpriseCandidate(
                id = "CAND_CH",
                name = "陈浩",
                avatarColor = 0xFF00897B,
                targetPosition = "云原生 / K8s 基础设施研发专家",
                expectedSalary = "25k-40k",
                school = "浙江大学",
                degree = "计算机科学与技术 · 本科",
                graduationYear = "2024 届",
                jobStatus = "已离职 · 随时到岗",
                matchScore = 95,
                mandatoryCerts = listOf("CKA (Kubernetes认证管理员)", "系统架构设计师 (软考高级)", "华为认证 ICT 专家 (HCIE Cloud)"),
                skillTags = listOf("Kubernetes / Docker", "Golang", "Service Mesh", "高可用容灾", "Linux内核调优"),
                highlights = "持CKA与HCIE顶级双认证；拥有独角兽企业核心容器集群运维与Operator自动化开发实战；主导异地多活灾备建设。",
                workOrProjectSummary = "深入掌握K8s调度机制与Cobra客户端开发，开源社区贡献者。",
                isBookmarked = false,
                isInvited = false
            ),
            EnterpriseCandidate(
                id = "CAND_ZRX",
                name = "赵若曦",
                avatarColor = 0xFFD81B60,
                targetPosition = "涉外数据合规总监 / 跨国法律顾问",
                expectedSalary = "20k-32k",
                school = "中国政法大学",
                degree = "民商法学 · 硕士",
                graduationYear = "2025 届应届",
                jobStatus = "离校实习 · 随时到岗",
                matchScore = 99,
                mandatoryCerts = listOf("国家统一法律职业资格证书 (法考A证)", "国际隐私合规认证 (CIPP/E)", "英语专业八级 TEM-8"),
                skillTags = listOf("法考A证", "GDPR数据出境合规", "涉外合同审查", "知识产权争议解决", "商事仲裁"),
                highlights = "法考客观题230+分首考通过；红圈律所资本市场与跨境数据合规实习经验；流利英文谈判与双语合同草拟。",
                workOrProjectSummary = "参与头部互联网平台海外出海反垄断与隐私保护安全风险评估体系搭建。",
                isBookmarked = false,
                isInvited = false
            ),
            EnterpriseCandidate(
                id = "CAND_ZWB",
                name = "周文博",
                avatarColor = 0xFFE65100,
                targetPosition = "总造价工程师 / 一级项目总指挥",
                expectedSalary = "24k-38k",
                school = "同济大学",
                degree = "土木工程与工程管理 · 本科",
                graduationYear = "2023 届",
                jobStatus = "在职看机会 · 2周内到岗",
                matchScore = 94,
                mandatoryCerts = listOf("一级造价工程师", "一级建造师 (建筑工程)", "PMP 项目管理专业人士"),
                skillTags = listOf("一造/一建双证", "BIM 5D全生命周期", "大型总承包EPC", "成本全流程精细管控", "招投标清标"),
                highlights = "持住建部一建+一造双硬性准入证书；曾主导两座40万平超高层综合体全阶段概预算编制与结算审核，核减工程款超3000万。",
                workOrProjectSummary = "深谙广联达与BIM计量计价软件，具备特级资质施工国企核心项目管理经验。",
                isBookmarked = false,
                isInvited = false
            ),
            EnterpriseCandidate(
                id = "CAND_SYH",
                name = "孙艺涵",
                avatarColor = 0xFF5E35B1,
                targetPosition = "国际研学首席双语导师 / 重点高中英语骨干",
                expectedSalary = "15k-25k",
                school = "北京师范大学",
                degree = "英语语言文学 · 硕士",
                graduationYear = "2026 届应届",
                jobStatus = "毕业求职 · 随时到岗",
                matchScore = 97,
                mandatoryCerts = listOf("高级中学教师资格证 (英语)", "英语专业八级 (TEM-8 优秀)", "CATTI 二级笔译"),
                skillTags = listOf("高级中学教资", "剑桥少儿考官资质", "跨文化沟通", "分层教学课程研发", "国际双语研学"),
                highlights = "北师大硕士；教育部高级中学教师资格；获全国大学生英语演讲比赛特等奖；全国翻译专业资格认证。",
                workOrProjectSummary = "曾主编面向知名国际学校的定制研学课案，教学满意度达99.5%。",
                isBookmarked = false,
                isInvited = false
            )
        )
    )

    // 企业端发布的职位列表
    val enterpriseJobs = MutableStateFlow(
        listOf(
            EnterpriseJobPosting(
                id = "E_JOB_1",
                title = "Android 研发架构师 (云终端)",
                department = "云与智慧产业事业群 · 终端平台部",
                salary = "25k-45k",
                location = "深圳 · 南山",
                headCount = 3,
                mandatoryCerts = listOf("软件设计师 (软考中级)", "信息系统项目管理师 (软考高级)"),
                preferredCerts = listOf("大学英语六级 (CET-6)", "软考系统架构设计师"),
                experienceReq = "应届硕博 / 1-3年",
                educationReq = "本科及以上",
                jobDescription = "负责核心云终端 Android 客户端基础架构演进，参与 Compose UI 组件库落地与音视频实时低延迟网络传输底层优化；推动质量工程与稳定性治理。",
                applicantCount = 28,
                viewCount = 480,
                isOnline = true,
                postedTime = "2小时前"
            ),
            EnterpriseJobPosting(
                id = "E_JOB_2",
                title = "涉外法务合规经理 (海外业务)",
                department = "法务综合部 · 跨国合规组",
                salary = "20k-35k",
                location = "北京 · 海淀",
                headCount = 2,
                mandatoryCerts = listOf("国家统一法律职业资格证书 (法考A证)"),
                preferredCerts = listOf("国际隐私合规认证 (CIPP/E)", "英语专业八级 TEM-8"),
                experienceReq = "应届优秀 / 1-3年",
                educationReq = "硕士及以上",
                jobDescription = "负责出海游戏及云业务的数据安全合规论证，起草并审核海外跨境商事法律文书；对接境外反垄断与消费者权益法律监管合规。",
                applicantCount = 35,
                viewCount = 620,
                isOnline = true,
                postedTime = "昨天"
            ),
            EnterpriseJobPosting(
                id = "E_JOB_3",
                title = "资本市场财务总监助理 (投融资)",
                department = "企业发展事业群 · 战略财务部",
                salary = "22k-38k",
                location = "上海 · 徐汇",
                headCount = 2,
                mandatoryCerts = listOf("注册会计师 CPA (专业阶段全科)"),
                preferredCerts = listOf("特许金融分析师 CFA", "证券从业资格"),
                experienceReq = "1-3年 / 优秀应届生",
                educationReq = "本科及以上",
                jobDescription = "参与被投企业财务尽职调查与投后运营跟踪，主导合并财务报表复核与资本市场估值建模；协助推进并购重组项目底稿申报。",
                applicantCount = 19,
                viewCount = 310,
                isOnline = true,
                postedTime = "3天前"
            ),
            EnterpriseJobPosting(
                id = "E_JOB_4",
                title = "云原生容器基础设施专家",
                department = "星澜智云基础设施与AI架构部",
                salary = "30k-50k",
                location = "深圳 · 南山",
                headCount = 4,
                mandatoryCerts = listOf("CKA (Kubernetes认证管理员)", "系统架构设计师 (软考高级)"),
                preferredCerts = listOf("华为认证 HCIE Cloud"),
                experienceReq = "3-5年",
                educationReq = "本科及以上",
                jobDescription = "负责百万级容器调度平台稳定性运维与智能化自治系统研发，深入分析内核调度瓶颈，建设高可用跨云多活控制平面。",
                applicantCount = 14,
                viewCount = 275,
                isOnline = true,
                postedTime = "4天前"
            )
        )
    )

    // 企业端候选人沟通列表
    val enterpriseConversations = MutableStateFlow(
        listOf(
            CandidateConversation(
                id = "CONV_ZCH",
                candidateId = "CAND_ZCH",
                candidateName = "张楚航",
                candidateSchool = "华中科技大学 · 软件工程",
                candidateCertTag = "软考高级 + 法考A证",
                targetPosition = "Android 研发架构师",
                avatarColor = 0xFF1E88E5,
                lastMessage = "林老师您好！我对云终端 Compose 架构重构非常感兴趣，已发在线简历。",
                lastMessageTime = "10:24",
                hasUnread = true,
                unreadCount = 2
            ),
            CandidateConversation(
                id = "CONV_LL",
                candidateId = "CAND_LL",
                candidateName = "李林",
                candidateSchool = "上海交通大学 · 会计学硕士",
                candidateCertTag = "CPA 专业阶段全科",
                targetPosition = "高级财务合规总监",
                avatarColor = 0xFF7B1FA2,
                lastMessage = "感谢贵司对我的关注，我随时方便进行线上飞书专业复试！",
                lastMessageTime = "09:40",
                hasUnread = true,
                unreadCount = 1
            ),
            CandidateConversation(
                id = "CONV_ZRX",
                candidateId = "CAND_ZRX",
                candidateName = "赵若曦",
                candidateSchool = "中国政法大学 · 民商法学",
                candidateCertTag = "法考A证 + CIPP/E",
                targetPosition = "涉外法务合规经理",
                avatarColor = 0xFFD81B60,
                lastMessage = "我已经提交了涉外数据出境合规的调研方案简报，请查阅附件。",
                lastMessageTime = "昨天",
                hasUnread = false,
                unreadCount = 0
            ),
            CandidateConversation(
                id = "CONV_CH",
                candidateId = "CAND_CH",
                candidateName = "陈浩",
                candidateSchool = "浙江大学 · 计算机科学",
                candidateCertTag = "CKA + 软考高级",
                targetPosition = "云原生容器基础设施专家",
                avatarColor = 0xFF00897B,
                lastMessage = "关于跨可用区容灾切换机制，我们可以约个技术二面详聊底层实现。",
                lastMessageTime = "前天",
                hasUnread = false,
                unreadCount = 0
            )
        )
    )

    // 企业端面试邀约记录
    val interviewInvites = MutableStateFlow(
        listOf(
            InterviewInvite(
                id = "INV_1",
                candidateId = "CAND_ZCH",
                candidateName = "张楚航",
                positionTitle = "Android 研发架构师 (云终端)",
                interviewTime = "2026-10-15 14:30",
                interviewType = "线上视频面试 (AI智联会议)",
                locationOrLink = "https://meeting.starstream.ai/room/88920194",
                notes = "请准备15分钟架构设计作品分享及法定软考证书原件核验",
                status = "候选人已接受"
            ),
            InterviewInvite(
                id = "INV_2",
                candidateId = "CAND_LL",
                candidateName = "李林",
                positionTitle = "资本市场财务总监助理",
                interviewTime = "2026-10-16 10:00",
                interviewType = "线上视频面试 (AI智联会议)",
                locationOrLink = "https://meeting.starstream.ai/room/99301824",
                notes = "重点考察财务底稿尽调逻辑与CPA会计准则实操分析",
                status = "待候选人确认"
            )
        )
    )

    // 企业端搜索与筛选状态
    val enterpriseSearchTalentQuery = MutableStateFlow("")
    val enterpriseSelectedCertFilter = MutableStateFlow("全部")
    val activeEnterpriseCandidate = MutableStateFlow<EnterpriseCandidate?>(null)

    // 企业端与候选人的对话消息流
    val enterpriseChatMessages = MutableStateFlow<Map<String, List<ChatMessage>>>(
        mapOf(
            "CAND_ZCH" to listOf(
                ChatMessage("m1", "RECRUITER", "楚航同学你好！看到你持有软考高级与法考A证，并且在知名云原生实验室实习过，背景非常出色！", "10:15", true),
                ChatMessage("m2", "USER", "林老师您好！我对咱们云终端的 Android 架构演进与 Compose 深度调优非常向往！", "10:20", true),
                ChatMessage("m3", "RECRUITER", "我们组正在推进新一代全栈云终端，非常看重高并发与本地数据一致性保障。何时方便进行技术面试？", "10:22", true),
                ChatMessage("m4", "USER", "我已提交了在线简历，周四或周五下午都非常方便，随时可以接入会议！", "10:24", true)
            ),
            "CAND_LL" to listOf(
                ChatMessage("m1", "RECRUITER", "李林同学你好，注册会计师 CPA 全科在校内通过非常不易，业务底子很硬！", "09:30", true),
                ChatMessage("m2", "USER", "感谢林老师认可！我在四大会计所参与过科创板IPO审计，对合并报表与税务合规很有实操经验。", "09:35", true),
                ChatMessage("m3", "USER", "感谢贵司对我的关注，我随时方便进行线上飞书专业复试！", "09:40", true)
            )
        )
    )

    fun postEnterpriseJob(
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
    ) {
        val newJob = EnterpriseJobPosting(
            id = "E_JOB_${System.currentTimeMillis()}",
            title = title.trim(),
            department = department.trim().ifEmpty { "技术与业务研发中心" },
            salary = salary.trim().ifEmpty { "20k-35k" },
            location = location.trim().ifEmpty { "深圳" },
            headCount = if (headCount <= 0) 1 else headCount,
            mandatoryCerts = mandatoryCerts,
            preferredCerts = preferredCerts,
            experienceReq = experienceReq.ifEmpty { "经验不限 / 优秀应届" },
            educationReq = educationReq.ifEmpty { "本科及以上" },
            jobDescription = jobDescription.trim().ifEmpty { "负责核心业务系统架构研发，推动持证专业规范落地。" },
            applicantCount = 0,
            viewCount = 1,
            isOnline = true,
            postedTime = "刚刚"
        )
        val current = enterpriseJobs.value.toMutableList()
        current.add(0, newJob)
        enterpriseJobs.value = current
    }

    fun toggleJobStatus(jobId: String) {
        val current = enterpriseJobs.value.toMutableList()
        val index = current.indexOfFirst { it.id == jobId }
        if (index >= 0) {
            val old = current[index]
            current[index] = old.copy(isOnline = !old.isOnline)
            enterpriseJobs.value = current
        }
    }

    fun bookmarkCandidate(candidateId: String) {
        val current = enterpriseCandidates.value.toMutableList()
        val index = current.indexOfFirst { it.id == candidateId }
        if (index >= 0) {
            val old = current[index]
            current[index] = old.copy(isBookmarked = !old.isBookmarked)
            enterpriseCandidates.value = current
        }
    }

    fun sendInterviewInvite(
        candidateId: String,
        positionTitle: String,
        time: String,
        type: String,
        linkOrLoc: String,
        notes: String
    ) {
        val candidate = enterpriseCandidates.value.find { it.id == candidateId }
        val invite = InterviewInvite(
            id = "INV_${System.currentTimeMillis()}",
            candidateId = candidateId,
            candidateName = candidate?.name ?: "候选人",
            positionTitle = positionTitle,
            interviewTime = time,
            interviewType = type,
            locationOrLink = linkOrLoc,
            notes = notes,
            status = "待候选人确认"
        )
        val current = interviewInvites.value.toMutableList()
        current.add(0, invite)
        interviewInvites.value = current

        val candList = enterpriseCandidates.value.toMutableList()
        val cIdx = candList.indexOfFirst { it.id == candidateId }
        if (cIdx >= 0) {
            candList[cIdx] = candList[cIdx].copy(isInvited = true)
            enterpriseCandidates.value = candList
        }

        sendEnterpriseMessage(candidateId, "【面试邀请函】诚邀您参与「$positionTitle」专业面试。\n时间：$time\n形式：$type\n地点/链接：$linkOrLoc\n备注：$notes")
    }

    fun sendEnterpriseMessage(candidateId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val msgMap = enterpriseChatMessages.value.toMutableMap()
        val list = (msgMap[candidateId] ?: emptyList()).toMutableList()
        list.add(
            ChatMessage(
                id = "msg_${System.currentTimeMillis()}",
                sender = "RECRUITER",
                content = trimmed,
                timestamp = "刚刚",
                isRead = true
            )
        )
        msgMap[candidateId] = list
        enterpriseChatMessages.value = msgMap

        val convList = enterpriseConversations.value.toMutableList()
        val cIdx = convList.indexOfFirst { it.candidateId == candidateId }
        if (cIdx >= 0) {
            val oldConv = convList[cIdx]
            convList[cIdx] = oldConv.copy(lastMessage = trimmed, lastMessageTime = "刚刚")
            enterpriseConversations.value = convList
        } else {
            val cand = enterpriseCandidates.value.find { it.id == candidateId }
            if (cand != null) {
                convList.add(
                    0,
                    CandidateConversation(
                        id = "CONV_${System.currentTimeMillis()}",
                        candidateId = cand.id,
                        candidateName = cand.name,
                        candidateSchool = cand.school,
                        candidateCertTag = cand.mandatoryCerts.firstOrNull() ?: "持证人才",
                        targetPosition = cand.targetPosition,
                        avatarColor = cand.avatarColor,
                        lastMessage = trimmed,
                        lastMessageTime = "刚刚",
                        hasUnread = false,
                        unreadCount = 0
                    )
                )
                enterpriseConversations.value = convList
            }
        }
    }

    // --- UI 导航状态 ---
    val currentTab = MutableStateFlow(0) // 0: 招聘求职, 1: 搭伙圈, 2: 求职信使, 3: 个人主页
    val showOnlineResumeScreen = MutableStateFlow(false)
    val resumeStartInEditMode = MutableStateFlow(false)

    fun navigateToOnlineResume(editMode: Boolean = true) {
        currentTab.value = 3 // 跳转至个人主页 Tab
        showOnlineResumeScreen.value = true
        resumeStartInEditMode.value = editMode
    }

    // --- 个人信息完善状态 (默认未完善，点击可完善) ---
    val isProfileCompleted = MutableStateFlow(false)

    // --- 真实个人资料状态 (可进行完整编辑与保存) ---
    val userRealName = MutableStateFlow("张楚航")
    val userNickname = MutableStateFlow("楚航学长")
    val userPhone = MutableStateFlow("13888889999")
    val userEmail = MutableStateFlow("zhangchuhang@hust.edu.cn")
    val userGender = MutableStateFlow("男")
    val userSchool = MutableStateFlow("华中科技大学")
    val userGrade = MutableStateFlow("大三")
    val userMajor = MutableStateFlow("软件工程")
    val boundWeChat = MutableStateFlow("ch_zhang_99")

    // --- 个人在线求职简历数据 (标准履历中心) ---
    val resumeTargetJob = MutableStateFlow("软件开发工程师 / 全栈工程师")
    val resumeExpectSalary = MutableStateFlow("15K - 25K / 月")
    val resumeExpectCity = MutableStateFlow("杭州 / 深圳 / 上海 / 北京")
    val resumeJobStatus = MutableStateFlow("离校实习 · 随时到岗")
    val resumeEducationDegree = MutableStateFlow("本科学士 (2022 - 2026)")
    val resumeSelfIntro = MutableStateFlow(
        "华中科技大学软件工程在读，具备扎实的计算机科学基础与多端全栈落地实战经验。熟练掌握 Kotlin、Java、Android 与微服务架构，注重代码工程质量与高可用设计。在国家级/省级高校科创赛事中主导核心架构研发并获一等奖，具备出色的自驱力、业务问题拆解能力与跨学科团队协同领导力。"
    )
    val resumeWorkExperiences = MutableStateFlow(
        listOf(
            ResumeWorkExp(
                id = "exp_1",
                company = "星澜数智科技 (深圳) 有限公司",
                role = "移动端/后端研发实习生",
                period = "2025.06 - 2025.10",
                description = "参与核心业务中台微服务接口研发与性能调优，基于 Kotlin 与 Spring Boot 重构高并发任务分发模块，吞吐提升 35%；主导移动端本地 Room 数据库缓存与网络拦截层优化，网络异常重试成功率达 99.8%。"
            ),
            ResumeWorkExp(
                id = "exp_2",
                company = "高校国家重点实验室 · 科创项目组",
                role = "搭伙科创团队负责人 / 全栈架构师",
                period = "2024.09 - 2025.05",
                description = "主导「基于知识图谱的职场考证智能双向检索系统」研发，带领 5 人跨学科团队斩获挑战杯省级一等奖；负责全链路数据库建模、Supabase 云端高并发索引及 Android Jetpack Compose 端到端交互交付。"
            )
        )
    )
    val resumeSkillTags = MutableStateFlow(
        listOf("Kotlin / Java", "Jetpack Compose", "Android SDK", "Spring Boot", "MySQL / Room", "Git敏捷协作", "高并发微服务", "系统架构设计")
    )
    val resumeCertifications = MutableStateFlow(
        listOf("信息系统项目管理师 (软考高级)", "大学英语六级 CET-6 (580分)", "全国计算机等级考试四级", "法律职业资格 A 证 (备考通过)")
    )
    val resumeContestAwards = MutableStateFlow(
        listOf(
            "第十四届「挑战杯」中国大学生创业计划竞赛 全国金奖",
            "中国国际大学生创新大赛 (原互联网+) 省级一等奖",
            "全国大学生数学建模竞赛 (MCM/ICM) 国家二等奖",
            "中国高校计算机大赛 (团体程序设计天梯赛) 全国一等奖"
        )
    )
    val resumeHonors = MutableStateFlow("专业前 5% · 连续两年获国家励志奖学金 · 优秀共青团干部 · 挑战杯省级一等奖")

    fun addWorkExperience(exp: ResumeWorkExp) {
        val current = resumeWorkExperiences.value.toMutableList()
        current.add(0, exp)
        resumeWorkExperiences.value = current
    }

    fun updateWorkExperience(exp: ResumeWorkExp) {
        val current = resumeWorkExperiences.value.toMutableList()
        val index = current.indexOfFirst { it.id == exp.id }
        if (index >= 0) {
            current[index] = exp
            resumeWorkExperiences.value = current
        }
    }

    fun deleteWorkExperience(id: String) {
        val current = resumeWorkExperiences.value.toMutableList()
        current.removeAll { it.id == id }
        resumeWorkExperiences.value = current
    }

    fun addResumeCert(cert: String) {
        val trimmed = cert.trim()
        if (trimmed.isEmpty()) return
        val current = resumeCertifications.value.toMutableList()
        if (!current.contains(trimmed)) {
            current.add(trimmed)
            resumeCertifications.value = current
        }
    }

    fun deleteResumeCert(cert: String) {
        val current = resumeCertifications.value.toMutableList()
        current.remove(cert)
        resumeCertifications.value = current
    }

    fun addResumeSkill(skill: String) {
        val trimmed = skill.trim()
        if (trimmed.isEmpty()) return
        val current = resumeSkillTags.value.toMutableList()
        if (!current.contains(trimmed)) {
            current.add(trimmed)
            resumeSkillTags.value = current
        }
    }

    fun deleteResumeSkill(skill: String) {
        val current = resumeSkillTags.value.toMutableList()
        current.remove(skill)
        resumeSkillTags.value = current
    }

    fun addContestAward(award: String) {
        val trimmed = award.trim()
        if (trimmed.isEmpty()) return
        val current = resumeContestAwards.value.toMutableList()
        if (!current.contains(trimmed)) {
            current.add(trimmed)
            resumeContestAwards.value = current
        }
    }

    fun deleteContestAward(award: String) {
        val current = resumeContestAwards.value.toMutableList()
        current.remove(award)
        resumeContestAwards.value = current
    }

    // --- 🌟 动漫联名限定皮肤与二次元属性系统 (Anime Collab Limited Skin) ---
    val selectedAnimeSkin = MutableStateFlow(AnimeSkinTheme.SAKURA_STARLIGHT)
    val isDarkMode = MutableStateFlow(false) // 默认日间模式 (白底黑字)
    val animeBattlePower = MutableStateFlow(98500) // 初始职场战斗力 / BP
    val animeAwakeningLevel = MutableStateFlow(99) // 觉醒等级 Lv.99

    fun setAnimeSkin(skin: AnimeSkinTheme) {
        selectedAnimeSkin.value = skin
    }

    fun toggleDarkMode(enabled: Boolean) {
        isDarkMode.value = enabled
    }

    // --- MBTI 职场人格认证状态 (默认未认证 null，可选 16 型) ---
    val userMbti = MutableStateFlow<String?>(null)

    // --- 双向检索状态与历史记录 ---
    val searchMode = MutableStateFlow(0) // 0: 配证书(查岗位), 1: 找职业(持证书)
    val searchQuery = MutableStateFlow("") // 默认清空无残留
    val selectedCategory = MutableStateFlow("低门槛·高成长")
    val recentJobSearches = MutableStateFlow(listOf("律师", "消防工程师", "软件工程师", "注册会计师", "高中教师"))
    val recentCertSearches = MutableStateFlow(listOf("法律职业资格", "注册会计师CPA", "教师资格证", "软考高项", "一级建造师"))

    fun addSearchHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        if (searchMode.value == 0) { // 配证书 -> 输入岗位关键词搜索
            val list = recentJobSearches.value.toMutableList()
            list.remove(trimmed)
            list.add(0, trimmed)
            recentJobSearches.value = list.take(8)
        } else { // 找职业 -> 输入持有证书搜索
            val list = recentCertSearches.value.toMutableList()
            list.remove(trimmed)
            list.add(0, trimmed)
            recentCertSearches.value = list.take(8)
        }
    }

    // 后端 Supabase 数据库检索中状态
    val isBackendSearching = MutableStateFlow(false)

    // 数据源 Flow
    val categories = listOf("低门槛·高成长", "法律/财务", "技术开发", "教育/语言", "综合行政/工程健康")

    // 动态搜索的职业列表 (端到端直连后端 Supabase 数据库检索，支持离线无缝降级)
    val occupationsList: StateFlow<List<Occupation>> = searchQuery
        .onEach { clearDetails() }
        .debounce(150)
        .flatMapLatest { rawQuery ->
            val query = rawQuery.trim()
            if (query.isEmpty()) {
                flowOf(emptyList()) // 界面默认彻底清空所有岗位
            } else {
                flow {
                    isBackendSearching.value = true
                    try {
                        // 1. 发起到后端 Supabase 数据库的实时检索
                        val backendResults = com.example.data.network.SupabaseClient.searchOccupations(query)
                        if (backendResults.isNotEmpty()) {
                            emit(backendResults)
                        } else {
                            // 2. 若后端未匹配，降级检索本地 Room 数据库
                            val cleanTerm = when {
                                query.endsWith("员") && query.length > 2 -> query.removeSuffix("员")
                                query.endsWith("师") && query.length > 2 -> query.removeSuffix("师")
                                query.endsWith("工") && query.length > 2 -> query.removeSuffix("工")
                                query.endsWith("官") && query.length > 2 -> query.removeSuffix("官")
                                else -> query
                            }
                            emit(dao.searchOccupations(cleanTerm).first())
                        }
                    } catch (e: Exception) {
                        emit(dao.searchOccupations(query).first())
                    } finally {
                        isBackendSearching.value = false
                    }
                }
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 动态搜索的证书列表 (端到端直连后端 Supabase 数据库检索)
    val certificatesList: StateFlow<List<Certificate>> = searchQuery
        .onEach { clearDetails() }
        .debounce(150)
        .flatMapLatest { rawQuery ->
            val query = rawQuery.trim()
            if (query.isEmpty()) {
                flowOf(emptyList()) // 界面默认彻底清空所有证书
            } else {
                flow {
                    isBackendSearching.value = true
                    try {
                        // 1. 发起到后端 Supabase 证书库的实时检索
                        val backendResults = com.example.data.network.SupabaseClient.searchCertificates(query)
                        if (backendResults.isNotEmpty()) {
                            emit(backendResults)
                        } else {
                            val cleanTerm = when {
                                query.endsWith("证") && query.length > 2 -> query.removeSuffix("证")
                                query.endsWith("证书") && query.length > 3 -> query.removeSuffix("证书")
                                else -> query
                            }
                            emit(dao.searchCertificates(cleanTerm).first())
                        }
                    } catch (e: Exception) {
                        emit(dao.searchCertificates(query).first())
                    } finally {
                        isBackendSearching.value = false
                    }
                }
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 选中的岗位详情关联证书 (即时同步清除，杜绝卡片残留)
    private val _selectedOccupationId = MutableStateFlow<String?>(null)
    val selectedOccupationId: StateFlow<String?> = _selectedOccupationId.asStateFlow()

    private val _relatedCertificates = MutableStateFlow<List<CertificateWithRelationInfo>>(emptyList())
    val relatedCertificates: StateFlow<List<CertificateWithRelationInfo>> = _relatedCertificates.asStateFlow()

    // 选中的证书详情关联岗位 (即时同步清除，杜绝卡片残留)
    private val _selectedCertificateId = MutableStateFlow<String?>(null)
    val selectedCertificateId: StateFlow<String?> = _selectedCertificateId.asStateFlow()

    private val _relatedOccupations = MutableStateFlow<List<OccupationWithRelationInfo>>(emptyList())
    val relatedOccupations: StateFlow<List<OccupationWithRelationInfo>> = _relatedOccupations.asStateFlow()

    private var loadRelatedJob: kotlinx.coroutines.Job? = null

    // --- AI 实时推导与 Supabase 云端缓存状态 ---
    val isAiMatchingLoading = MutableStateFlow(false)
    val aiOccupationMatches = MutableStateFlow<Map<String, List<com.example.data.network.AiMatchedItem>>>(emptyMap())
    val aiCertificateMatches = MutableStateFlow<Map<String, List<com.example.data.network.AiMatchedItem>>>(emptyMap())

    // --- 搜索未匹配职业时的 AI 结构化匹配数据与状态 ---
    val aiSearchResult = MutableStateFlow<com.example.data.network.AiSearchOutput?>(null)
    val isAiSearchLoading = MutableStateFlow(false)

    fun triggerAiSearchForNotFound(query: String, mode: Int, forceRefresh: Boolean = false) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        if (!forceRefresh && aiSearchResult.value?.query == trimmed && aiSearchResult.value?.mode == mode) {
            return
        }
        viewModelScope.launch {
            isAiSearchLoading.value = true
            try {
                val result = com.example.data.network.GeminiMatchingService.matchUnknownOccupation(trimmed, mode)
                aiSearchResult.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isAiSearchLoading.value = false
            }
        }
    }

    fun triggerAiMatchForOccupation(occupation: Occupation, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh && aiOccupationMatches.value.containsKey(occupation.id)) return@launch
            isAiMatchingLoading.value = true
            try {
                // 1. 尝试从 Supabase 缓存中快速读取
                val cached = if (!forceRefresh) {
                    com.example.data.network.SupabaseClient.getCachedRelationsForOccupation(occupation.name)
                } else emptyList()

                if (cached.isNotEmpty()) {
                    aiOccupationMatches.update { it + (occupation.id to cached) }
                } else {
                    // 2. 调用 Gemini 智能模型推导匹配
                    val aiResults = com.example.data.network.GeminiMatchingService.matchCertificatesForOccupation(
                        occupation.name,
                        occupation.description
                    )
                    aiOccupationMatches.update { it + (occupation.id to aiResults) }
                    // 3. 异步沉淀回 Supabase 数据库
                    com.example.data.network.SupabaseClient.saveAiResultsToSupabase(
                        targetName = occupation.name,
                        results = aiResults,
                        isOccupationToCert = true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isAiMatchingLoading.value = false
            }
        }
    }

    fun triggerAiMatchForCertificate(certificate: Certificate, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh && aiCertificateMatches.value.containsKey(certificate.id)) return@launch
            isAiMatchingLoading.value = true
            try {
                // 1. 尝试从 Supabase 缓存中快速读取
                val cached = if (!forceRefresh) {
                    com.example.data.network.SupabaseClient.getCachedRelationsForCertificate(certificate.name)
                } else emptyList()

                if (cached.isNotEmpty()) {
                    aiCertificateMatches.update { it + (certificate.id to cached) }
                } else {
                    // 2. 调用 Gemini 智能模型反向拓宽岗位
                    val aiResults = com.example.data.network.GeminiMatchingService.matchOccupationsForCertificate(
                        certificate.name,
                        certificate.description
                    )
                    aiCertificateMatches.update { it + (certificate.id to aiResults) }
                    // 3. 异步沉淀回 Supabase 数据库
                    com.example.data.network.SupabaseClient.saveAiResultsToSupabase(
                        targetName = certificate.name,
                        results = aiResults,
                        isOccupationToCert = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isAiMatchingLoading.value = false
            }
        }
    }

    // --- 模拟对话与已读未读消息状态 (AI导师置顶) ---
    val recruiters = MutableStateFlow(
        listOf(
            Recruiter("AGENT_RESUME_POLISH", "AI 简历润色专家 🔮", "搭伙圈 AI 实验室 (置顶导师)", 0xFFE07A5F, "OCC_SOFTWARE_DEV", "少侠！零经验？没实习？别慌，把你在校的杂事写给我，看我用 STAR 法则一键变废为宝！", "刚刚", true),
            Recruiter("TUTOR_AI", "AI 生涯规划师 🧭", "国家智慧教育平台 (置顶导师)", 0xFFFF7096, "OCC_PM_AST", "你好！我是你的专属职业生涯规划师，你想探索哪个职业与考证方向？", "刚刚", true),
            Recruiter("HR_BYTEDANCE", "张敏 (HR主管)", "字节跳动合规组", 0xFF6C63FF, "OCC_LEGAL_ADV", "同学你好，你的法考A证确实是硬性前置加分项，何时能面试？", "10:30 AM", true),
            Recruiter("TECH_HUAWEI", "李国栋 (高级架构师)", "华为移动研发部", 0xFF00B4D8, "OCC_SOFTWARE_DEV", "看到你有软考中级证书，底层基础很扎实，我们聊聊？", "昨天", true),
            Recruiter("HR_GOLDMAN", "Catherine (招聘总监)", "高盛亚洲研究所", 0xFF9B5DE5, "OCC_SECURITIES_AST", "你好，证券研究助理岗位需要同时持有证券与基金从业证书。", "2天前", false),
            Recruiter("HR_ALIBABA", "王承 (技术招聘专家)", "阿里达摩院", 0xFFFF9900, "OCC_SOFTWARE_DEV", "同学，你的大模型提示工程与数据工程背景很对口我们组！", "昨天", false),
            Recruiter("HR_QIANXIN", "陈工 (攻防实验室负责人)", "奇安信安全科技", 0xFF2A9D8F, "OCC_SOFTWARE_DEV", "你好，看到你持有 CISP 注册信息安全资质，具备实战经验吗？", "3天前", false),
            Recruiter("HR_ICBC", "周经理 (金融科技校招主管)", "中国工商银行总行", 0xFFC92A2A, "OCC_FINANCIAL_PLAN", "同学你好，工行总行科技金融管培生正在筛选首轮名单。", "5小时前", true),
            Recruiter("HR_CITIC", "陆总 (投资银行部董事总经理)", "中信证券投行部", 0xFF1864AB, "OCC_SECURITIES_AST", "持有法考或CPA在投行底稿尽调中优势极大，简历已通过初筛！", "前天", false),
            Recruiter("HR_MCKINSEY", "Sarah (大中华区校招总监)", "麦肯锡咨询", 0xFF1098AD, "OCC_FINANCIAL_PLAN", "Hi！我们很看重你的数据建模逻辑与跨学科领导力。", "4天前", false),
            Recruiter("HR_XDF", "刘老师 (国际教学主管)", "新东方教育集团", 0xFF7048E8, "OCC_TEACHER_SEC", "同学你好，高级中学教资是硬门槛，你的试讲表现准备如何？", "昨天", false),
            Recruiter("HR_MANGO", "柯导 (内容生态制作人)", "湖南广电芒果TV", 0xFFFF6B6B, "OCC_PM_AST", "新一季头部S级综艺急需网生脑洞策划，期待你的文创作品集！", "2天前", false),
            Recruiter("HR_CSCEC", "赵总工 (海外重大工程总指挥)", "中国建筑第八工程局", 0xFF0CA678, "OCC_SOFTWARE_DEV", "我们一带一路海外标杆项目需要持二建以上青年工程师！", "昨天", false),
            Recruiter("HR_BYD", "孙工 (动力电控研发组长)", "比亚迪汽车工程院", 0xFF3B5BDB, "OCC_SOFTWARE_DEV", "新一代刀片电驱动平台需要硬核电气背景，何时方便技术二面？", "3小时前", true),
            Recruiter("HR_SINOPHARM", "郑主管 (临床研究合规负责人)", "国药集团致君制药", 0xFF20C997, "OCC_LEGAL_ADV", "临床试验GCP规范监查需要执业药师资质，你的背景非常契合！", "昨天", false),
            Recruiter("HR_TAIKANG", "韩总监 (心身关怀业务总监)", "泰康大健康生活中心", 0xFFF08C00, "OCC_PM_AST", "你好，高端养老社区心身辅导岗位很看重专业心理咨询资质。", "4天前", false),
            Recruiter("HR_DJI", "肖队长 (低空应用交付组长)", "大疆创新行业部", 0xFF495057, "OCC_SOFTWARE_DEV", "持 CAAC 执照进行三维正射与激光点云建模是我们的标准动作！", "昨天", false),
            Recruiter("HR_CYTS", "杜部长 (国际研学中心首席导师)", "中青旅国际研学", 0xFF008080, "OCC_TEACHER_SEC", "涉外高定路线需要扎实的导游与外语双证，诚邀加入！", "3天前", false),
            Recruiter("HR_SPIC", "林研究员 (绿能与双碳投资部)", "国家电投绿能投资", 0xFF2B8A3E, "OCC_FINANCIAL_PLAN", "ESG披露评级与碳资产盘查是未来风口，欢迎来所里详聊！", "昨天", false)
        )
    )

    val activeRecruiter = MutableStateFlow<Recruiter?>(null)

    val selectedJobIndustry = MutableStateFlow("全部")

    val recruitingJobs = MutableStateFlow(
        listOf(
            // 1. 互联网/IT科技行业
            RecruitingJob(
                id = "JOB_BYTEDANCE",
                title = "法务合规专员 (出海方向)",
                company = "字节跳动合规组",
                salary = "15k-25k",
                location = "北京",
                avatarColor = 0xFF6C63FF,
                mandatoryCerts = listOf("国家法律职业资格 (法考A证)"),
                preferredCerts = listOf("大学英语六级 (CET6)", "涉外合规师"),
                description = "负责出海及多国地区合规保障，起草涉外隐私与数据安全法律文书，与跨国监管高效协同对接。",
                recruiterId = "HR_BYTEDANCE",
                industry = "互联网/IT"
            ),
            RecruitingJob(
                id = "JOB_HUAWEI",
                title = "Android 研发工程师",
                company = "华为终端BG",
                salary = "18k-30k",
                location = "深圳",
                avatarColor = 0xFF00B4D8,
                mandatoryCerts = listOf("软件设计师 (软考中级)"),
                preferredCerts = listOf("大学英语六级 (CET6)", "高频算法设计师"),
                description = "负责HarmonyOS/Android系统框架核心模块及跨终端互联SDK开发，参与极客级并发优化及Compose自研组件编写。",
                recruiterId = "TECH_HUAWEI",
                industry = "互联网/IT"
            ),
            RecruitingJob(
                id = "JOB_STARSTREAM",
                title = "产品经理助理 (科创项目)",
                company = "星澜科创实验室",
                salary = "12k-20k",
                location = "深圳",
                avatarColor = 0xFFFF7096,
                mandatoryCerts = listOf("项目管理专业人士 (PMP)"),
                preferredCerts = listOf("大学英语六级 (CET6)", "软件设计师 (软考中级)"),
                description = "主导移动端创新产品原型交互，进行高保真PRD编写及全生命周期研发敏捷管理，擅长MBTI性格创意直觉。",
                recruiterId = "TUTOR_AI",
                industry = "互联网/IT"
            ),
            RecruitingJob(
                id = "JOB_ALIBABA",
                title = "大模型数据工程与评测专家",
                company = "阿里巴巴达摩院",
                salary = "22k-38k",
                location = "杭州",
                avatarColor = 0xFFFF9900,
                mandatoryCerts = listOf("软件设计师 (软考中级)"),
                preferredCerts = listOf("CDA 数据分析师 (Level 1)", "大学英语六级 (CET6)"),
                description = "负责通义多模态大模型评测基准构建、高质量行业语料微调与对齐评估，主导多任务自动化 Benchmark 流水线开发。",
                recruiterId = "HR_ALIBABA",
                industry = "互联网/IT"
            ),
            RecruitingJob(
                id = "JOB_QIANXIN",
                title = "网络安全渗透测试工程师",
                company = "奇安信安全科技",
                salary = "14k-24k",
                location = "北京",
                avatarColor = 0xFF2A9D8F,
                mandatoryCerts = listOf("注册信息安全专业人员 (CISP)"),
                preferredCerts = listOf("程序员 (软考初级)", "大学英语四级 (CET-4)"),
                description = "负责攻防对抗红蓝实战演练、企业内网脆弱性评估及APT攻击溯源分析，保障关基系统重保业务安全稳定运行。",
                recruiterId = "HR_QIANXIN",
                industry = "互联网/IT"
            ),

            // 2. 金融银行与投资证券
            RecruitingJob(
                id = "JOB_GOLDMAN",
                title = "证券研究助理 (分析师方向)",
                company = "高盛亚洲研究所",
                salary = "20k-35k",
                location = "上海",
                avatarColor = 0xFF9B5DE5,
                mandatoryCerts = listOf("证券从业资格证", "基金从业资格证"),
                preferredCerts = listOf("注册会计师 (CPA)", "大学英语六级 (CET-6)"),
                description = "协助首席行业分析师，收集、整理宏观数据及行业报表，进行行业深度调研及分析，起草中英文投资建议报告。",
                recruiterId = "HR_GOLDMAN",
                industry = "金融/证券"
            ),
            RecruitingJob(
                id = "JOB_ICBC",
                title = "总行科技金融管培生",
                company = "中国工商银行总行",
                salary = "13k-20k",
                location = "北京",
                avatarColor = 0xFFC92A2A,
                mandatoryCerts = listOf("银行业专业人员职业资格证"),
                preferredCerts = listOf("基金从业资格证", "CDA 数据分析师 (Level 1)"),
                description = "负责总行科创型小微企业专项信贷产品风控建模、绿色供应链金融资产尽调，参与新一代智慧银行金融科技场景落地。",
                recruiterId = "HR_ICBC",
                industry = "金融/证券"
            ),
            RecruitingJob(
                id = "JOB_CITIC",
                title = "投行分析师助理 (IBD)",
                company = "中信证券投资银行部",
                salary = "18k-28k",
                location = "深圳",
                avatarColor = 0xFF1864AB,
                mandatoryCerts = listOf("证券从业资格证"),
                preferredCerts = listOf("注册会计师 (CPA)", "法律职业资格证书 (A证)"),
                description = "参与A股IPO、主板并购重组及企业债发行承销工作，负责行业与对标公司尽职调查、招股说明书工作底稿编制。",
                recruiterId = "HR_CITIC",
                industry = "金融/证券"
            ),

            // 3. 财会审计与战略咨询
            RecruitingJob(
                id = "JOB_PWC",
                title = "初级审计顾问",
                company = "普华永道 (PwC) 审计部",
                salary = "10k-16k",
                location = "广州",
                avatarColor = 0xFFE07A5F,
                mandatoryCerts = listOf("基金从业资格证"),
                preferredCerts = listOf("注册会计师 (CPA)", "大学英语六级 (CET-6)"),
                description = "执行大型集团企业财务报表审计及内部控制合规评估，分析重大财务勾稽关系，输出严密审计工作底稿。",
                recruiterId = "HR_GOLDMAN",
                industry = "财会/咨询"
            ),
            RecruitingJob(
                id = "JOB_MCKINSEY",
                title = "商业战略分析师 (全职)",
                company = "麦肯锡咨询 (McKinsey)",
                salary = "25k-42k",
                location = "上海",
                avatarColor = 0xFF1098AD,
                mandatoryCerts = listOf("项目管理专业人士 (PMP)"),
                preferredCerts = listOf("CDA 数据分析师 (Level 1)", "英语专业八级 (TEM-8)"),
                description = "服务全球500强及大型国央企高层战略转型，进行定性市场访谈与定量财务回归建模，输出顶级商业战略洞见。",
                recruiterId = "HR_MCKINSEY",
                industry = "财会/咨询"
            ),

            // 4. 教育培训与文化传媒
            RecruitingJob(
                id = "JOB_XDF",
                title = "双语国际学科教学导师",
                company = "新东方教育科技集团",
                salary = "12k-18k",
                location = "武汉",
                avatarColor = 0xFF7048E8,
                mandatoryCerts = listOf("高级中学教师资格证", "普通话水平测试 (二级甲等)"),
                preferredCerts = listOf("大学英语六级 (CET-6)", "英语专业八级 (TEM-8)"),
                description = "负责国际IB/AP/A-Level课程体系双语授课，制定学员个性化学术提分方案，进行课堂互动与深度答疑。",
                recruiterId = "HR_XDF",
                industry = "教育/文传"
            ),
            RecruitingJob(
                id = "JOB_MANGO",
                title = "影视编导与IP文创衍生策划",
                company = "湖南广电芒果TV文创中心",
                salary = "11k-18k",
                location = "长沙",
                avatarColor = 0xFFFF6B6B,
                mandatoryCerts = listOf("普通话水平测试 (一级乙等)"),
                preferredCerts = listOf("数字资产IP文创衍生品策划师", "大学英语四级 (CET-4)"),
                description = "负责头部S级综艺节目前期脚本统筹、短视频宣发矩阵创意剪辑与衍生潮玩文创周边方案孵化。",
                recruiterId = "HR_MANGO",
                industry = "教育/文传"
            ),

            // 5. 高端制造与建筑工程
            RecruitingJob(
                id = "JOB_CSCEC",
                title = "海外工程建造管理工程师",
                company = "中国建筑第八工程局",
                salary = "15k-25k",
                location = "南京",
                avatarColor = 0xFF0CA678,
                mandatoryCerts = listOf("二级建造师 (二建)"),
                preferredCerts = listOf("一级建造师 (一建)", "大学英语六级 (CET-6)"),
                description = "参与大型枢纽机场及超高层地标总承包施工管理，主导现场BIM协同建模、质量安全验收与多工种劳务协调调度。",
                recruiterId = "HR_CSCEC",
                industry = "制造/工程"
            ),
            RecruitingJob(
                id = "JOB_BYD",
                title = "智能电动车动力电控工程师",
                company = "比亚迪汽车工程研究院",
                salary = "16k-28k",
                location = "深圳",
                avatarColor = 0xFF3B5BDB,
                mandatoryCerts = listOf("注册电气工程师"),
                preferredCerts = listOf("软件设计师 (软考中级)", "大学英语六级 (CET-6)"),
                description = "负责新能源乘用车高压电控驱动系统软件开发与CAN总线协议栈测试，参与极寒高温整车标定实验。",
                recruiterId = "HR_BYD",
                industry = "制造/工程"
            ),

            // 6. 生物医药与医疗健康
            RecruitingJob(
                id = "JOB_SINOPHARM",
                title = "临床试验监查员 (CRA) / 质控专员",
                company = "国药集团致君制药",
                salary = "13k-22k",
                location = "成都",
                avatarColor = 0xFF20C997,
                mandatoryCerts = listOf("执业药师 (药学/中药学)"),
                preferredCerts = listOf("大学英语六级 (CET-6)", "注册安全评价师"),
                description = "协同三甲医院开展抗肿瘤创新药II/III期临床试验监查，监督GCP规范执行，核实CRF原始数据与药物不良反应记录。",
                recruiterId = "HR_SINOPHARM",
                industry = "医药/健康"
            ),
            RecruitingJob(
                id = "JOB_TAIKANG",
                title = "心身健康顾问与心理干预督导",
                company = "泰康大健康生活中心",
                salary = "10k-17k",
                location = "杭州",
                avatarColor = 0xFFF08C00,
                mandatoryCerts = listOf("心理咨询专业技能证书"),
                preferredCerts = listOf("普通话水平测试 (二级甲等)", "企业人力资源管理师 (四级)"),
                description = "负责高端养老社区居民与高净值客群的心理健康体检评估、认知症早筛干预与家庭情感抚慰支持体系搭建。",
                recruiterId = "HR_TAIKANG",
                industry = "医药/健康"
            ),

            // 7. 现代文旅与低空新兴
            RecruitingJob(
                id = "JOB_DJI",
                title = "无人机遥感航测与巡检工程师",
                company = "大疆创新行业应用部",
                salary = "14k-23k",
                location = "深圳",
                avatarColor = 0xFF495057,
                mandatoryCerts = listOf("民用无人机操控员执照 (CAAC)"),
                preferredCerts = listOf("注册安全评价师", "大学英语四级 (CET-4)"),
                description = "负责电力巡线、应急测绘及林业巡防等工业级无人机航线规划、LiDAR激光点云数据处理与三维正射影像建模。",
                recruiterId = "HR_DJI",
                industry = "文旅/新兴"
            ),
            RecruitingJob(
                id = "JOB_CYTS",
                title = "涉外高端定制研学领队",
                company = "中青旅国际研学事业部",
                salary = "9k-15k",
                location = "北京",
                avatarColor = 0xFF008080,
                mandatoryCerts = listOf("全国导游资格证书", "普通话水平测试 (二级甲等)"),
                preferredCerts = listOf("全国翻译专业资格证书 (三级口译/笔译)", "大学英语六级 (CET-6)"),
                description = "负责国内外名校研学营与联合国青少年峰会路线带团策划，进行双语文化场馆专业讲解与安全突发应急演练把控。",
                recruiterId = "HR_CYTS",
                industry = "文旅/新兴"
            ),

            // 8. 绿色双碳与新能源
            RecruitingJob(
                id = "JOB_SPIC",
                title = "碳资产管理与ESG评级分析师",
                company = "国家电投绿色能源投资部",
                salary = "15k-26k",
                location = "上海",
                avatarColor = 0xFF2B8A3E,
                mandatoryCerts = listOf("碳中和绿色ESG高级评估员"),
                preferredCerts = listOf("初级会计职称", "大学英语六级 (CET-6)"),
                description = "负责企业范围一二三温室气体排放盘查与碳配额交易履约核算，撰写符合国际ISSB标准的ESG可持续发展年报。",
                recruiterId = "HR_SPIC",
                industry = "绿色/能源"
            )
        )
    )

    val jobSearchQuery = MutableStateFlow("")
    val selectedJobCity = MutableStateFlow("全国")

    fun resetJobFilters() {
        jobSearchQuery.value = ""
        selectedJobCity.value = "全国"
        selectedJobIndustry.value = "全部"
    }

    val filteredRecruitingJobs: StateFlow<List<RecruitingJob>> = combine(
        recruitingJobs,
        selectedJobIndustry,
        selectedJobCity,
        jobSearchQuery
    ) { jobs, industry, city, query ->
        jobs.filter { job ->
            val matchIndustry = (industry == "全部" || job.industry == industry)
            val matchCity = (city == "全国" || job.location.contains(city) || city.contains(job.location))
            val matchQuery = if (query.isBlank()) true else {
                val q = query.trim()
                job.title.contains(q, ignoreCase = true) ||
                job.company.contains(q, ignoreCase = true) ||
                job.location.contains(q, ignoreCase = true) ||
                job.description.contains(q, ignoreCase = true) ||
                job.mandatoryCerts.any { it.contains(q, ignoreCase = true) } ||
                job.preferredCerts.any { it.contains(q, ignoreCase = true) } ||
                job.industry.contains(q, ignoreCase = true)
            }
            matchIndustry && matchCity && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 针对每个 recruiter 的消息列表
    private val _chatMessagesMap = MutableStateFlow<Map<String, List<ChatMessage>>>(
        mapOf(
            "HR_BYTEDANCE" to listOf(
                ChatMessage("1", "RECRUITER", "同学你好，我们看到了你在高校平台提交的求职意向。", "10:25 AM", true),
                ChatMessage("2", "USER", "您好！我对贵司跨境电商及出海合规岗非常感兴趣，我刚通过了国家法律职业资格 A 证考试。", "10:28 AM", true),
                ChatMessage("3", "RECRUITER", "同学你好，你的法考A证确实是硬性前置加分项，何时能面试？", "10:30 AM", false)
            ),
            "TECH_HUAWEI" to listOf(
                ChatMessage("1", "RECRUITER", "你好，我是华为 Android 研发组的李国栋。看到你的简历写了掌握 Jetpack Compose 和 Room 数据库。", "09:15 AM", true),
                ChatMessage("2", "USER", "是的李老师，我独立开发过本地化双向检索应用，并且通过了国家软考中级设计师认证。", "09:18 AM", true),
                ChatMessage("3", "RECRUITER", "看到你有软考中级证书，底层基础很扎实，我们聊聊？", "09:20 AM", false)
            ),
            "HR_GOLDMAN" to listOf(
                ChatMessage("1", "USER", "您好，请问非财会专业的本科生有机会申请证券研究助理岗吗？", "前天", true),
                ChatMessage("2", "RECRUITER", "你好，证券研究助理岗位需要同时持有证券与基金从业证书。如果有 CPA 更是重磅加分项。", "前天", true)
            ),
            "TUTOR_AI" to listOf(
                ChatMessage("1", "RECRUITER", "你好！我是你的专属职业生涯规划师，你想探索哪个方向？我们可以从 150 个行业岗位中，分析属于你的双向证书通关路线图！", "刚刚", false)
            ),
            "AGENT_RESUME_POLISH" to listOf(
                ChatMessage("RESUME_1", "RECRUITER", "少侠你好！我是你的专属 AI 简历润色教练 🔮。\n\n很多同学觉得自己大一到大三除了当咸鱼，就是做社团跑腿搬砖、画海报排版，或者期末写写小计算器、小大作业。其实这些经历稍加重构就是闪闪发光的 BD/增长运营/研发经历！\n\n你可以直接把你的在校杂事大白话发给我（例如：我在社团拉了 500 块赞助、期末写了一个二手商品查询的代码、帮班里排版了期末路演 PPT），看我用大厂最爱的 STAR 法则帮它们强力润色，秒变高大上职场黑话！快发来试试吧！", "刚刚", false)
            ),
            "HR_ALIBABA" to listOf(
                ChatMessage("ALI_1", "RECRUITER", "你好同学！我是阿里达摩院技术招聘专家王承。看到你关注了大模型数据工程岗位，目前组内正进行百亿级语料对齐基准开发，有软考或数据分析背景优先安排二面！", "昨天", false)
            ),
            "HR_QIANXIN" to listOf(
                ChatMessage("QX_1", "RECRUITER", "你好，我是奇安信安全科技攻防实验室负责人陈工。CISP 或网络工程师认证是国家攻防蓝队和渗透测试的强背书，欢迎来聊聊实战演练经验！", "3天前", false)
            ),
            "HR_ICBC" to listOf(
                ChatMessage("ICBC_1", "RECRUITER", "同学你好，这里是中国工商银行总行金融科技招聘组。今年重点招收具备银行业专业资质与数字化建模能力的新星，简历已入池，期待你的加入！", "5小时前", true)
            ),
            "HR_CITIC" to listOf(
                ChatMessage("CITIC_1", "RECRUITER", "你好，我是中信证券投行部陆总。投行底稿尽调与申报材料制作对复合型背景非常青睐，法考A证或CPA能让你在入职第一天脱颖而出！", "前天", false)
            ),
            "HR_MCKINSEY" to listOf(
                ChatMessage("MCK_1", "RECRUITER", "Hi! Sarah here from McKinsey campus recruiting. 我们的商业战略咨询注重严密的问题拆解结构与定量逻辑，期待与你的交流！", "4天前", false)
            ),
            "HR_XDF" to listOf(
                ChatMessage("XDF_1", "RECRUITER", "同学你好！我是新东方国际教育教学主管刘老师。双语国际课程授课要求过硬的高级中学教资与英语水平，我们非常期待你的试讲展现！", "昨天", false)
            ),
            "HR_MANGO" to listOf(
                ChatMessage("MGO_1", "RECRUITER", "哈喽同学！我是芒果TV内容制作柯导。我们目前正为新一季S级青年创新节目物色网生新锐，如果你有数字文创或导演策划经验，请速投作品集！", "2天前", false)
            ),
            "HR_CSCEC" to listOf(
                ChatMessage("CSCEC_1", "RECRUITER", "你好，中建八局海外事业部赵总工。我们承接了多个海外机场高铁地标，持有二级建造师及以上的小伙子能快速担任标段执行负责人，欢迎详谈！", "昨天", false)
            ),
            "HR_BYD" to listOf(
                ChatMessage("BYD_1", "RECRUITER", "同学你好，比亚迪汽车工程研究院孙工。新一代刀片电驱动系统正在大规模招募电气系统人才，有注册电气资质或扎实单片机软考底子优先，聊聊？", "3小时前", true)
            ),
            "HR_SINOPHARM" to listOf(
                ChatMessage("SINO_1", "RECRUITER", "你好！国药集团致君制药临床监查合规组郑主管。创新药进入多中心临床试验离不开严谨的GCP质控与执业药师专业复核，期待你的简历！", "昨天", false)
            ),
            "HR_TAIKANG" to listOf(
                ChatMessage("TK_1", "RECRUITER", "同学你好，泰康大健康心身关怀业务韩总监。高端医养结合社区急需兼具温度与专业执业技能的心理咨询人才，你的专业资质令人眼前一亮！", "4天前", false)
            ),
            "HR_DJI" to listOf(
                ChatMessage("DJI_1", "RECRUITER", "你好！大疆创新行业应用部肖队长。低空经济正处于高速爆发期，具备 CAAC 执照与航测建模能力的工程师是真正的稀缺人才，期待并肩作战！", "昨天", false)
            ),
            "HR_CYTS" to listOf(
                ChatMessage("CYTS_1", "RECRUITER", "你好，中青旅国际研学事业部杜部长。我们的联合国峰会与名校营项目需要具备导游双证与外语能力的卓越带队导师，欢迎加入！", "3天前", false)
            ),
            "HR_SPIC" to listOf(
                ChatMessage("SPIC_1", "RECRUITER", "同学你好！国家电投绿色双碳投资部林研究员。国内外企业ESG评级与碳市场履约正迎来万亿蓝海，有绿能ESG证书是极佳的敲门砖！", "昨天", false)
            )
        )
    )

    val currentChatMessages: StateFlow<List<ChatMessage>> = activeRecruiter
        .flatMapLatest { recruiter ->
            if (recruiter == null) flowOf(emptyList())
            else _chatMessagesMap.map { messagesMap -> messagesMap[recruiter.id] ?: emptyList() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 职业树路线选择 ---
    val selectedTreeOccupationId = MutableStateFlow("OCC_LEGAL_ADV")
    val selectedTreeOccupation: StateFlow<Occupation?> = selectedTreeOccupationId
        .map { id ->
            com.example.data.database.StaticDataStore.getOccupations().firstOrNull { it.id == id }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val treeRequiredCerts: StateFlow<List<CertificateWithRelationInfo>> = selectedTreeOccupationId
        .flatMapLatest { id ->
            dao.getCertificatesForOccupation(id)
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 大学生生涯互助 (组队与咨询) 状态 ---
    val selectedCoCreateType = MutableStateFlow("全部") // "全部", "比赛组队", "实习咨询"
    val coCreateSearchQuery = MutableStateFlow("")

    val coCreatePosts = MutableStateFlow(
        listOf(
            CoCreatePost(
                id = "POST_1",
                authorId = "PEER_LIN",
                authorName = "林逸",
                authorMajor = "计算机学院 · 软件工程大三",
                authorAvatarColor = 0xFF4CAF50,
                type = "比赛组队",
                title = "「挑战杯国赛」急招跨界商业BP/文案队友！",
                content = "我们团队目前正在研发一款‘智能低碳绿色环保追踪系统’。系统硬件和算法开发已完成90%（已点亮软考中级证书、5G网络调测等技术栈）。目前极度缺乏一名擅长撰写商业计划书、进行PPT路演答辩的商科同学（最好对碳中和绿色ESG有一定了解）。欢迎随时小窗！",
                targetPlatform = "全国大学生“挑战杯”创业大赛",
                relatedCertTags = listOf("软件设计师 (软考中级)", "碳中和绿色ESG高级评估员"),
                neededRoles = "招募：商业BP撰写 / 答辩路演 (商科优先)",
                memberCount = 3,
                memberMax = 4,
                timestamp = "10分钟前"
            ),
            CoCreatePost(
                id = "POST_2",
                authorId = "PEER_WANG",
                authorName = "王舒雅",
                authorMajor = "商学院 · 会计学硕士研一",
                authorAvatarColor = 0xFFE91E63,
                type = "实习咨询",
                title = "CPA 已过专业阶段 4 门，在线解答四大会计所面试/实习避坑",
                content = "刚拿到普华永道（PwC）审计部门的暑期实习 Offer。回馈高校社区，在这里为想冲四大审计、内控或大厂财务分析助理的师弟师妹们解答咨询。关于‘注册会计师 (CPA)’备考规划、审计助理岗位实操、第一份金融实习怎么投递，欢迎向我提问！",
                targetPlatform = "四大会计师事务所审计咨询",
                relatedCertTags = listOf("注册会计师 (CPA)", "企业人力资源管理师 (四级)"),
                neededRoles = "咨询方向：四大审计实习面经 / CPA备考路线",
                memberCount = 12,
                memberMax = 99,
                timestamp = "45分钟前"
            ),
            CoCreatePost(
                id = "POST_3",
                authorId = "PEER_CHEN",
                authorName = "陈航",
                authorMajor = "数学与统计学院大二",
                authorAvatarColor = 0xFF00BCD4,
                type = "比赛组队",
                title = "【数学建模美赛】招募 Python 算法/会抓数的数据分析队友",
                content = "美赛组队冲刺，我和另一位队友擅长论文撰写和排版。目前急切寻找一位对Python建模熟悉、会进行复杂数据清洗和商业建模的队友（最好考过CDA数据分析师或软考初级，对数据极其敏感）。我们一起冲刺一等奖！",
                targetPlatform = "美国大学生数学建模竞赛 (MCM/ICM)",
                relatedCertTags = listOf("CDA 数据分析师 (Level 1)", "程序员 (软考初级)"),
                neededRoles = "招募：Python数据建模 / 特征工程开发",
                memberCount = 2,
                memberMax = 3,
                timestamp = "2小时前"
            ),
            CoCreatePost(
                id = "POST_4",
                authorId = "PEER_ZHANG",
                authorName = "张雨彤",
                authorMajor = "外国语学院 · 英语翻译大四",
                authorAvatarColor = 0xFFFF9800,
                type = "实习咨询",
                title = "CATTI二级口译已点亮！分享涉外随同/出海选品翻译实习经验",
                content = "已成功通过CATTI二级口笔译。大四在一家大型出海跨境电商企业做翻译兼出海选品合规助理。想了解翻译专业资格证的复习方法、外企/涉外商务助理岗位的面试流程、以及英语专业八级备考重点的，随时联系我沟通！",
                targetPlatform = "涉外翻译与跨境电商选品咨询",
                relatedCertTags = listOf("全国翻译专业资格证书 (二级口译/笔译)", "英语专业八级 (TEM-8)"),
                neededRoles = "咨询方向：涉外商务翻译 / 跨境电商助理面试",
                memberCount = 8,
                memberMax = 99,
                timestamp = "3小时前"
            ),
            CoCreatePost(
                id = "POST_5",
                authorId = "PEER_LIU",
                authorName = "刘杰",
                authorMajor = "自动化与电气工程学院大三",
                authorAvatarColor = 0xFF9C27B0,
                type = "比赛组队",
                title = "「挑战杯」无人机智能环保巡检，缺测绘/林业绿化专业队友",
                content = "我们团队利用智能无人机巡检算法进行绿化评估。我已考取 CAAC 无人机操控员驾驶执照（飞手技能点满），团队目前开发了基础飞控，急需一名对林业巡检标准、景观绿化测绘或智慧植被覆盖分析熟悉的同学，提供业务方案支持！",
                targetPlatform = "全国大学生“挑战杯”科创比赛",
                relatedCertTags = listOf("民用无人驾驶航空器操控员执照 (CAAC 无人机驾驶证)", "二级建造师 (二建)"),
                neededRoles = "招募：景观绿化测绘方案 / 智慧林业背景同学",
                memberCount = 2,
                memberMax = 4,
                timestamp = "5小时前"
            ),
            CoCreatePost(
                id = "POST_6",
                authorId = "PEER_XU",
                authorName = "徐菲菲",
                authorMajor = "法学院 · 知识产权法大四",
                authorAvatarColor = 0xFFE040FB,
                type = "实习咨询",
                title = "法学学姐已过法考和专利代理师，解答法务助理/专利代理面经",
                content = "大四已成功点亮法考A证与专利代理师资格证书。目前在一家红圈律所知识产权部门实习。对于法本/非法本同学如何平衡法考与专利代理师备考、大厂法务助理/知识产权合规官的校招要求，可以提供一对一真实经验分享。",
                targetPlatform = "红圈法务与专利代理实务咨询",
                relatedCertTags = listOf("法律职业资格证书 (A证)", "专利代理师资格证书"),
                neededRoles = "咨询方向：知识产权合规官 / 法务助理实习要求",
                memberCount = 15,
                memberMax = 99,
                timestamp = "1天前"
            )
        )
    )

    // 过滤后的互助帖子列表
    val filteredCoCreatePosts: StateFlow<List<CoCreatePost>> = combine(
        coCreatePosts,
        selectedCoCreateType,
        coCreateSearchQuery
    ) { posts, type, query ->
        posts.filter { post ->
            val matchType = type == "全部" || post.type == type
            val matchQuery = query.isEmpty() ||
                    post.title.contains(query, ignoreCase = true) ||
                    post.content.contains(query, ignoreCase = true) ||
                    post.targetPlatform.contains(query, ignoreCase = true) ||
                    post.authorMajor.contains(query, ignoreCase = true) ||
                    post.relatedCertTags.any { it.contains(query, ignoreCase = true) }
            matchType && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 互助交互动作 ---
    fun toggleJoinPost(postId: String) {
        coCreatePosts.update { list ->
            list.map {
                if (it.id == postId) {
                    val joined = !it.isJoined
                    val newCount = if (joined) it.memberCount + 1 else it.memberCount - 1
                    it.copy(isJoined = joined, memberCount = newCount)
                } else it
            }
        }

        // 发送方获得消息和提示
        val updatedPost = coCreatePosts.value.firstOrNull { it.id == postId } ?: return
        val peerId = updatedPost.authorId
        
        if (updatedPost.isJoined) {
            // 用户点击了申请加入，在发帖人的沟通栏闪烁消息红点提示！
            val list = recruiters.value
            val exists = list.any { it.id == peerId }

            if (!exists) {
                val newRecruiter = Recruiter(
                    id = peerId,
                    name = "${updatedPost.authorName} (${updatedPost.type}发起人)",
                    company = updatedPost.authorMajor,
                    avatarColor = updatedPost.authorAvatarColor,
                    targetOccupationId = "OCC_LEGAL_ADV",
                    lastMessage = "已收到您的申请，简历匹配中...",
                    lastMessageTime = "刚刚",
                    hasUnread = true // 🔴 亮起红点！
                )
                recruiters.update { current -> current + newRecruiter }
            } else {
                recruiters.update { current ->
                    current.map {
                        if (it.id == peerId) {
                            it.copy(
                                lastMessage = "已收到您的入队申请，简历评估中...",
                                lastMessageTime = "刚刚",
                                hasUnread = true // 🔴 亮起红点！
                            )
                        } else it
                    }
                }
            }

            // 自动注入两人之间极具防诈警示和仪式感的消息对话
            _chatMessagesMap.update { currentMap ->
                val existingMsgs = currentMap[peerId] ?: emptyList()
                val newMsgs = existingMsgs + listOf(
                    ChatMessage("SYS_${System.currentTimeMillis()}_1", "USER", "【入队意向申请书】我已点击「申请加入」您的合作项目「${updatedPost.title}」，并同步共享了我的数字化考证与技能主页档案，请审阅！", "刚刚", true),
                    ChatMessage("SYS_${System.currentTimeMillis()}_2", "RECRUITER", "哈喽同学！我是「${updatedPost.title}」的发起人 ${updatedPost.authorName}。我已经收到你的申请通知啦！🎉 平台已通过安全的【信使信箱】将你的简历推送给我。在审核结束前，请千万不要泄露微信或手机号，谨防外部招生诈骗。我们直接在这里沟通就好，期待合作！🌟", "刚刚", false)
                )
                currentMap.toMutableMap().apply { put(peerId, newMsgs) }
            }
        } else {
            // 取消加入
            _chatMessagesMap.update { currentMap ->
                val existingMsgs = currentMap[peerId] ?: emptyList()
                val newMsgs = existingMsgs + listOf(
                    ChatMessage("SYS_${System.currentTimeMillis()}_3", "USER", "【撤回申请】我已撤回该合作项目的申请。", "刚刚", true)
                )
                currentMap.toMutableMap().apply { put(peerId, newMsgs) }
            }
        }
    }

    fun contactAuthor(post: CoCreatePost) {
        // 1. 检查此 Author 是否已经在 Recruiters 列表里
        val peerId = post.authorId
        val list = recruiters.value
        val exists = list.any { it.id == peerId }

        if (!exists) {
            val newRecruiter = Recruiter(
                id = peerId,
                name = "${post.authorName} (${post.type}发起人)",
                company = post.authorMajor,
                avatarColor = post.authorAvatarColor,
                targetOccupationId = "OCC_LEGAL_ADV", // 默认
                lastMessage = "你好！我看到了你在‘生涯互助’板块发布的关于「${post.title}」的帖子，想和你交流一下！",
                lastMessageTime = "刚刚",
                hasUnread = false
            )
            recruiters.update { current -> current + newRecruiter }
            
            // 初始化对话记录
            _chatMessagesMap.update { currentMap ->
                val initMsg = listOf(
                    ChatMessage("INIT_1", "RECRUITER", "同学你好！我是「${post.title}」的发起人 ${post.authorName}。很高兴在生涯互助板块看到你对我的帖子感兴趣！", "刚刚", true),
                    ChatMessage("INIT_2", "USER", "你好！我对你发布的这篇「${post.title}」很感兴趣。我看到上面关联了【${post.relatedCertTags.joinToString("、")}】。我想进一步了解细节或加入合作！", "刚刚", true)
                )
                currentMap.toMutableMap().apply { put(peerId, initMsg) }
            }
        }

        // 2. 开启聊天并将 Tab 设为 2 ("求职信使"的索引)
        val selectedRecruiter = recruiters.value.first { it.id == peerId }
        startChatWith(selectedRecruiter)
        currentTab.value = 2 // 切换至求职信使
    }

    fun addCoCreatePost(
        type: String,
        title: String,
        platform: String,
        content: String,
        neededRoles: String,
        certTags: List<String>
    ) {
        val newPost = CoCreatePost(
            id = "POST_" + System.currentTimeMillis().toString(),
            authorId = "PEER_ME",
            authorName = "我 (当前用户)",
            authorMajor = "高校注册会员 · 证书通关者",
            authorAvatarColor = 0xFF6C63FF,
            type = type,
            title = title,
            content = content,
            targetPlatform = platform,
            relatedCertTags = certTags,
            neededRoles = neededRoles,
            memberCount = 1,
            memberMax = if (type == "比赛组队") 4 else 99,
            timestamp = "刚刚"
        )
        coCreatePosts.update { current -> listOf(newPost) + current }
    }

    // --- 交互方法 ---

    fun setCategory(category: String) {
        selectedCategory.value = category
        searchQuery.value = category
        clearDetails()
    }

    fun selectOccupation(id: String?, occupation: Occupation? = null) {
        loadRelatedJob?.cancel()
        _relatedCertificates.value = emptyList() // 立即同步清除，防止旧卡片数据残留！
        _selectedOccupationId.value = id
        _selectedCertificateId.value = null
        if (id != null) {
            loadRelatedJob = viewModelScope.launch(Dispatchers.IO) {
                // 1. 优先从后端 Supabase 数据库拉取该职业关联的法定证书
                val backendCerts = if (occupation != null) {
                    com.example.data.network.SupabaseClient.getCertificatesForOccupation(occupation)
                } else emptyList()

                if (backendCerts.isNotEmpty() && _selectedOccupationId.value == id) {
                    _relatedCertificates.value = backendCerts
                } else {
                    // 2. 降级从本地 Room 数据库获取
                    dao.getCertificatesForOccupation(id).collect { certs ->
                        if (_selectedOccupationId.value == id) {
                            _relatedCertificates.value = certs
                        }
                    }
                }
            }
            if (occupation != null) {
                triggerAiMatchForOccupation(occupation)
            }
        }
    }

    fun selectCertificate(id: String?, certificate: Certificate? = null) {
        loadRelatedJob?.cancel()
        _relatedOccupations.value = emptyList() // 立即同步清除，防止旧卡片数据残留！
        _selectedCertificateId.value = id
        _selectedOccupationId.value = null
        if (id != null) {
            loadRelatedJob = viewModelScope.launch(Dispatchers.IO) {
                dao.getOccupationsForCertificate(id).collect { occs ->
                    if (_selectedCertificateId.value == id) {
                        _relatedOccupations.value = occs
                    }
                }
            }
            if (certificate != null) {
                triggerAiMatchForCertificate(certificate)
            }
        }
    }

    fun clearDetails() {
        loadRelatedJob?.cancel()
        _selectedOccupationId.value = null
        _selectedCertificateId.value = null
        _relatedCertificates.value = emptyList()
        _relatedOccupations.value = emptyList()
        if (searchQuery.value.trim().isEmpty()) {
            aiSearchResult.value = null
        }
    }

    /**
     * 实时深度清理本地数据残留与重置预览界面
     */
    fun resetAndReloadDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            clearDetails()
            searchQuery.value = ""
            aiOccupationMatches.value = emptyMap()
            aiCertificateMatches.value = emptyMap()
            dao.clearRelations()
            dao.clearOccupations()
            dao.clearCertificates()
            dao.insertCertificates(StaticDataStore.getCertificates())
            dao.insertOccupations(StaticDataStore.getOccupations())
            dao.insertRelations(StaticDataStore.getRelations())
        }
    }

    // 开启聊天
    fun startChatWith(recruiter: Recruiter) {
        activeRecruiter.value = recruiter
        // 标记该 Recruiter 最后一项为已读
        recruiters.update { list ->
            list.map {
                if (it.id == recruiter.id) it.copy(hasUnread = false) else it
            }
        }
        // 将属于该 recruiter 的消息全部标记为已读
        _chatMessagesMap.update { map ->
            val list = map[recruiter.id] ?: emptyList()
            val updatedList = list.map { it.copy(isRead = true) }
            map.toMutableMap().apply { put(recruiter.id, updatedList) }
        }
    }

    fun closeChat() {
        activeRecruiter.value = null
    }

    private fun getMatchedJobsJsonForPlanning(): String {
        return try {
            val selectedOcc = selectedTreeOccupation.value
            val occs = if (selectedOcc != null) listOf(selectedOcc) else StaticDataStore.getOccupations().take(2)
            val arr = org.json.JSONArray()
            for (occ in occs) {
                val obj = org.json.JSONObject()
                obj.put("occupation_title", occ.name)
                obj.put("category", occ.category)
                obj.put("salary_range", occ.salaryExpectation)
                obj.put("description", occ.description)
                val certsArr = org.json.JSONArray()
                treeRequiredCerts.value.forEach { c ->
                    val isMandatory = c.requirementType.equals("MANDATORY", ignoreCase = true)
                    certsArr.put("${c.certificate.name}(${if (isMandatory) "法定红线准入" else "加分优选"})")
                }
                obj.put("required_or_preferred_certs", certsArr)
                arr.put(obj)
            }
            arr.toString()
        } catch (e: Exception) {
            "[]"
        }
    }

    // 发送新消息并触发模拟回复
    fun sendMessage(text: String) {
        val recruiter = activeRecruiter.value ?: return
        if (text.trim().isEmpty()) return

        val userMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            sender = "USER",
            content = text,
            timestamp = "刚刚",
            isRead = true // 用户发的消息默认是已读
        )

        // 1. 添加用户消息
        _chatMessagesMap.update { map ->
            val list = map[recruiter.id]?.toMutableList() ?: mutableListOf()
            list.add(userMsg)
            map.toMutableMap().apply { put(recruiter.id, list) }
        }

        // 2. 更新最新消息文本
        recruiters.update { list ->
            list.map {
                if (it.id == recruiter.id) it.copy(lastMessage = text, lastMessageTime = "刚刚") else it
            }
        }

        // 3. 延迟触发模拟面试官/大模型 Agent 回复
        viewModelScope.launch {
            val replyText = if (recruiter.id == "TUTOR_AI" || recruiter.id == "AGENT_RESUME_POLISH") {
                val systemInstruction = if (recruiter.id == "TUTOR_AI") {
                    val certsList = resumeCertifications.value.joinToString(", ")
                    CareerPlannerSkill.buildSystemPrompt(
                        userMajor = userMajor.value,
                        userEducation = "${userGrade.value}（本科在读 · ${userSchool.value}）",
                        userCerts = if (certsList.isBlank()) "暂无专业准入证书（待规划）" else certsList,
                        userGoal = if (text.isBlank()) resumeTargetJob.value else text,
                        matchedJobsJson = getMatchedJobsJsonForPlanning()
                    )
                } else {
                    CampusToCareerSkill.SYSTEM_PROMPT
                }
                callDeepSeekApi(systemInstruction, text)
            } else {
                kotlinx.coroutines.delay(1200)
                generateRecruiterReply(recruiter.id, text)
            }

            val recruiterMsg = ChatMessage(
                id = (System.currentTimeMillis() + 1).toString(),
                sender = "RECRUITER",
                content = replyText,
                timestamp = "刚刚",
                isRead = false // 新来的消息未读，直到用户在这个聊天界面
            )

            _chatMessagesMap.update { map ->
                val list = map[recruiter.id]?.toMutableList() ?: mutableListOf()
                list.add(recruiterMsg)
                map.toMutableMap().apply { put(recruiter.id, list) }
            }

            // 如果用户当前仍在这个聊天界面，自动将新来的消息设为已读
            if (activeRecruiter.value?.id == recruiter.id) {
                _chatMessagesMap.update { map ->
                    val list = map[recruiter.id] ?: emptyList()
                    val updatedList = list.map { it.copy(isRead = true) }
                    map.toMutableMap().apply { put(recruiter.id, updatedList) }
                }
            } else {
                // 如果用户切出了该界面，将招聘人列表标红
                recruiters.update { list ->
                    list.map {
                        if (it.id == recruiter.id) it.copy(hasUnread = true, lastMessage = replyText, lastMessageTime = "刚刚") else it
                    }
                }
            }
        }
    }

    private suspend fun callDeepSeekApi(systemInstruction: String, prompt: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val apiKey = deepSeekApiKey.value.trim()
        val baseUrl = deepSeekBaseUrl.value.trim().trimEnd('/')
        val model = deepSeekModel.value.trim()

        if (apiKey.isEmpty()) {
            return@withContext "⚠️ 【DeepSeek 未配置 API Key】\n\n您尚未配置自己的 DeepSeek API 密钥，请点击界面右上角【⚙️ API 设置】输入您的 Key 即可实时对话！\n\n为你提供本地专家参考回复：\n\n" + getLocalFallbackReply(systemInstruction, prompt)
        }

        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val escapedPrompt = escapeJson(prompt)
        val escapedSystem = escapeJson(systemInstruction)

        val jsonPayload = """
            {
                "model": "$model",
                "messages": [
                    {"role": "system", "content": "$escapedSystem"},
                    {"role": "user", "content": "$escapedPrompt"}
                ],
                "temperature": 0.7,
                "stream": false
            }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = okhttp3.RequestBody.Companion.create(mediaType, jsonPayload)
        val fullUrl = if (baseUrl.endsWith("/chat/completions")) baseUrl else "$baseUrl/chat/completions"

        val request = okhttp3.Request.Builder()
            .url(fullUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext "⚠️ 【DeepSeek API 请求失败 ${response.code}】\n\n$bodyString\n\n请检查 API Key 余额或代理地址。为您调取本地专家兜底：\n\n" +
                        getLocalFallbackReply(systemInstruction, prompt)
                }
                val content = parseOpenAiChatResponse(bodyString)
                if (content.isNullOrBlank()) {
                    "⚠️ 【DeepSeek 解析失败】无法提取内容。为您切换本地智能备选：\n\n" +
                        getLocalFallbackReply(systemInstruction, prompt)
                } else {
                    content
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "⚠️ 【DeepSeek 连接异常】${e.message}\n请检查网络连接或 API Base URL。为您调取本地专家回复：\n\n" +
                getLocalFallbackReply(systemInstruction, prompt)
        }
    }

    private fun parseOpenAiChatResponse(jsonString: String): String? {
        try {
            val root = org.json.JSONObject(jsonString)
            val choices = root.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.optJSONObject("message") ?: return null
            val reasoning = message.optString("reasoning_content", "")
            val content = message.optString("content", "")
            return if (reasoning.isNotBlank() && content.isNotBlank()) {
                "🧠 【DeepSeek-R1 深度思考】\n$reasoning\n\n━━━━━━━━━━━━━━\n\n$content"
            } else if (content.isNotBlank()) {
                content
            } else if (reasoning.isNotBlank()) {
                reasoning
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private suspend fun callGeminiApi(systemInstruction: String, prompt: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // 如果 API Key 未配置，优雅降级到本地离线智能模板
            return@withContext getLocalFallbackReply(systemInstruction, prompt)
        }

        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val escapedPrompt = escapeJson(prompt)
        val escapedSystem = escapeJson(systemInstruction)

        val jsonPayload = """
            {
                "contents": [
                    {"parts": [{"text": $escapedPrompt}]}
                ],
                "systemInstruction": {
                    "parts": [{"text": $escapedSystem}]
                },
                "generationConfig": {
                    "temperature": 0.7,
                    "maxOutputTokens": 1000
                }
            }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = okhttp3.RequestBody.Companion.create(mediaType, jsonPayload)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = okhttp3.Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "🤖 [安全网关提示] 信号暂时微弱。已自动为您连通备用星链路线：\n\n" + 
                        getLocalFallbackReply(systemInstruction, prompt)
                }
                val bodyString = response.body?.string() ?: ""
                parseGeminiResponse(bodyString) ?: ("🤖 [安全网关提示] 回响解析异常。为您切换到急救包：\n\n" + 
                    getLocalFallbackReply(systemInstruction, prompt))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "🤖 [安全网关提示] 网络连接有波动。为您自动调取本地专家离线应答：\n\n" + 
                getLocalFallbackReply(systemInstruction, prompt)
        }
    }

    private fun escapeJson(text: String): String {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun parseGeminiResponse(jsonString: String): String? {
        try {
            val textKey = "\"text\":"
            val index = jsonString.indexOf(textKey)
            if (index == -1) return null
            
            val startQuote = jsonString.indexOf("\"", index + textKey.length)
            if (startQuote == -1) return null
            
            var endQuote = -1
            var escaped = false
            for (i in (startQuote + 1) until jsonString.length) {
                val c = jsonString[i]
                if (escaped) {
                    escaped = false
                    continue
                }
                if (c == '\\') {
                    escaped = true
                } else if (c == '"') {
                    endQuote = i
                    break
                }
            }
            if (endQuote == -1) return null
            
            val rawText = jsonString.substring(startQuote + 1, endQuote)
            return rawText
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\t", "\t")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getLocalFallbackReply(systemInstruction: String, prompt: String): String {
        if (systemInstruction.contains("在校经历转职场语言专家") || systemInstruction.contains("简历润色")) {
            return when {
                prompt.contains("赞助") || prompt.contains("外联") || prompt.contains("跑腿") || prompt.contains("商家") -> {
                    "### 🎯 职场能力转化亮点\n" +
                    "- **核心能力提取**：商务谈判能力、对外沟通、跨圈层资源整合能力\n" +
                    "- **价值分析**：拉赞助不是简单要钱或跑腿，而是向商户决策人展现商业曝光价值并促成签约，是业务闭环中的核心 BD (商业拓展) 动作。\n\n" +
                    "### ✍️ 优化后话术推荐\n" +
                    "> **原表达**：${prompt}\n" +
                    "> **职场化表达**：主导校级千人科创峰会外部赞助拓展，梳理周边 20 家目标商户画像并交付定制化路演方案；独立完成商务谈判，成功达成核心商户独家赞助，赞助款项 100% 履约交付，实现校园品牌曝光触达 2000+ 人次。\n\n" +
                    "### 💡 建议在面试中补充的细节\n" +
                    "- 补充被商户拒绝后的异议处理方式，以及你如何通过量化人流数据打消赞助商疑虑。"
                }
                prompt.contains("PPT") || prompt.contains("做ppt") || prompt.contains("ppt") || prompt.contains("策划") || prompt.contains("文案") -> {
                    "### 🎯 职场能力转化亮点\n" +
                    "- **核心能力提取**：高管级信息提炼能力、复杂逻辑可视化表达、项目交付把控\n" +
                    "- **价值分析**：做 PPT 与写策划绝不是打字排版，而是将多维度散碎资料抽象为具有决策指导意义的商业/竞赛纲领，直接决定评委对项目含金量的首要印象。\n\n" +
                    "### ✍️ 优化后话术推荐\n" +
                    "> **原表达**：${prompt}\n" +
                    "> **职场化表达**：作为核心骨干主导国家级竞赛商业路演方案构建，基于商业画布模型抽象 50+ 页复杂行业调研，重构为 20 页高逻辑性汇报 PPT；实现关键数据可视化图表化呈现，支撑团队在终审答辩中夺得高分评级。\n\n" +
                    "### 💡 建议在面试中补充的细节\n" +
                    "- 面试中可重点强调你如何设计 PPT 演讲故事线（Storyline），以及如何用数据支撑团队的核心论点。"
                }
                prompt.contains("组织") || prompt.contains("活动") || prompt.contains("签到") || prompt.contains("社团") -> {
                    "### 🎯 职场能力转化亮点\n" +
                    "- **核心能力提取**：项目统筹（PMO）、跨部门敏捷协作、突发风险应急控制\n" +
                    "- **价值分析**：组织校园活动就是一次完整的小型敏捷项目管理，涵盖人力排班、场地物料、突发预案与全周期落地把控。\n\n" +
                    "### ✍️ 优化后话术推荐\n" +
                    "> **原表达**：${prompt}\n" +
                    "> **职场化表达**：统筹规划 300+ 人规模高校科技创客沙龙全流程运营，制定甘特图倒排工期与 5 类突发预警方案；协调现场 15 人志愿者工作流，实现嘉宾零延误、活动准点率 100%，获评院系年度标杆项目。\n\n" +
                    "### 💡 建议在面试中补充的细节\n" +
                    "- 面试时分享一次现场突发状况（如设备故障或时间超时），说明你是如何在 5 分钟内协调解决的。"
                }
                else -> {
                    "### 🎯 职场能力转化亮点\n" +
                    "- **核心能力提取**：业务流程标准化、高韧性执行力、关键指标达成意识\n" +
                    "- **价值分析**：任何在校基础事务只要梳理出方法论与可复用流程，在大厂 HR 眼中都是极具潜力的初阶业务沉淀。\n\n" +
                    "### ✍️ 优化后话术推荐\n" +
                    "> **原表达**：${prompt}\n" +
                    "> **职场化表达**：负责项目基础数据流与协作节点管理，搭建轻量化进度追踪台账，消除跨组信息同步壁垒；将日常处理周期缩减 30%，保障整体里程碑节点高质量准时交付。\n\n" +
                    "### 💡 建议在面试中补充的细节\n" +
                    "- 强调你在这段经历中主动优化的流程或自建的模板工具，展现你超越同龄人的自主驱动力与职场素养。"
                }
            }
        } else {
            // TUTOR_AI (职业规划师) 兜底：输出严格符合 Skill 的结构化 JSON
            return """
            {
              "career_summary": "基于当前背景与目标【${prompt.ifBlank { "综合求职" }}】，您的工科专业底座良好，但在核心准入资质与行业实战方面仍有提升空间，具备优秀的冲刺潜力。",
              "match_score": 85,
              "advantages": [
                "目标岗位与专业方向具有良好的契合度，学习与工程领悟力强",
                "具备基础知识储备与求职方向定位"
              ],
              "gaps": [
                "缺少目标岗位核心法定红线或高含金量准入证书",
                "实战项目量化成果较少，面试深度追问容易暴露短板"
              ],
              "short_term_roadmap": {
                "timeline": "1-3个月（准备冲刺期）",
                "action_items": [
                  "集中精力攻克对应行业的核心门槛证书（如软考/法考/财会）",
                  "系统梳理在校项目与课题成果，提炼出可量化的核心业务亮点"
                ]
              },
              "mid_term_roadmap": {
                "timeline": "6-12个月（稳固提升期）",
                "action_items": [
                  "锁定秋招/春招提前批，进行全真模拟笔试与面试冲刺",
                  "参与1-2个高质量行业实践或企业级项目，夯实履历深度"
                ]
              },
              "recommended_certificates": [
                {
                  "cert_name": "信息系统项目管理师(软考高级)",
                  "reason": "国企/大厂校招顶级加分项，评定技术职称的核心依据",
                  "priority": "HIGH"
                },
                {
                  "cert_name": "法律职业资格证书(A证)",
                  "reason": "公考法务/监察/国企合规极高含金量准入门槛",
                  "priority": "MEDIUM"
                }
              ]
            }
            """.trimIndent()
        }
    }

    private fun generateRecruiterReply(recruiterId: String, userMessage: String): String {
        return when (recruiterId) {
            "HR_BYTEDANCE" -> {
                if (userMessage.contains("面试")) "好的！下周二下午 3:00 我们在飞书进行合规组的一面，请提前准备好您的法律职业资格证书电子件。"
                else "法务合规岗非常注重对出海业务风险的敏感度。你能谈谈你在英文合同审核和知识产权确权方面的实际经验吗？"
            }
            "TECH_HUAWEI" -> {
                if (userMessage.contains("证书") || userMessage.contains("软考")) "软考中级代表你具备扎实的软件设计师理论。我们组内目前重点开发 Jetpack Compose 的复杂高精交互，你对 M3 规范熟悉吗？"
                else "非常不错的方向！在移动端研发中，合理的本地持久化方案（如 Room）对性能调优至关重要。我们可以约个时间聊聊底层原理吗？"
            }
            "TUTOR_AI" -> {
                "根据你的职业偏好，我推荐你通过‘双向检索’定位本专业或感兴趣岗位的【法定门槛证书】。例如，若想从事金融或法律，必须先点亮准入证，这是绝不妥协的行业红线！"
            }
            else -> "同学你好，你的求职诉求我已经收到。关于我们岗位的专业证书加分项 and 技能要求，你可以查看主页的‘职业树路线’，了解完整的考证通关图谱！"
        }
    }
}
