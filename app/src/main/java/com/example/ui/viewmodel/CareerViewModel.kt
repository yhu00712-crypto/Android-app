package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.StaticDataStore
import com.example.data.model.*
import com.example.ui.theme.AnimeSkinTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull

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
    val recruiterId: String
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
                company = "腾讯科技 (深圳) 有限公司",
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

    // --- 模拟对话与已读未读消息状态 ---
    val recruiters = MutableStateFlow(
        listOf(
            Recruiter("HR_BYTEDANCE", "张敏 (HR主管)", "字节跳动合规组", 0xFF6C63FF, "OCC_LEGAL_ADV", "同学你好，你的法考A证确实是硬性前置加分项，何时能面试？", "10:30 AM", true),
            Recruiter("TECH_HUAWEI", "李国栋 (高级架构师)", "华为移动研发部", 0xFF00B4D8, "OCC_SOFTWARE_DEV", "看到你有软考中级证书，底层基础很扎实，我们聊聊？", "昨天", true),
            Recruiter("HR_GOLDMAN", "Catherine (招聘总监)", "高盛亚洲研究所", 0xFF9B5DE5, "OCC_SECURITIES_AST", "你好，证券研究助理岗位需要同时持有证券与基金从业证书。", "2天前", false),
            Recruiter("TUTOR_AI", "AI 生涯规划师", "国家智慧教育平台", 0xFFFF7096, "OCC_PM_AST", "你好！我是你的专属职业生涯规划师，你想探索哪个方向？", "1分钟前", true),
            Recruiter("AGENT_RESUME_POLISH", "AI 简历润色专家 🔮", "搭伙圈 AI 实验室", 0xFFE07A5F, "OCC_SOFTWARE_DEV", "少侠！零经验？没实习？别慌，把你在校的杂事写给我，看我用 STAR 法则一键变废为宝！", "刚刚", true)
        )
    )

    val activeRecruiter = MutableStateFlow<Recruiter?>(null)

    val recruitingJobs = MutableStateFlow(
        listOf(
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
                recruiterId = "HR_BYTEDANCE"
            ),
            RecruitingJob(
                id = "JOB_HUAWEI",
                title = "Android 研发工程师",
                company = "华为终端BG",
                salary = "18k-30k",
                location = "深圳",
                avatarColor = 0xFF00B4D8,
                mandatoryCerts = listOf("计算机软件资格考试 (软考中级)"),
                preferredCerts = listOf("大学英语六级 (CET6)", "高频算法设计师"),
                description = "负责HarmonyOS/Android系统框架核心模块及跨终端互联SDK开发，参与极客级并发优化及Compose自研组件编写。",
                recruiterId = "TECH_HUAWEI"
            ),
            RecruitingJob(
                id = "JOB_GOLDMAN",
                title = "证券研究助理 (分析师方向)",
                company = "高盛亚洲研究所",
                salary = "20k-35k",
                location = "上海",
                avatarColor = 0xFF9B5DE5,
                mandatoryCerts = listOf("证券从业资格", "基金从业资格"),
                preferredCerts = listOf("注册会计师 (CPA)", "特许金融分析师 (CFA)"),
                description = "协助首席行业分析师，收集、整理宏观数据及行业报表，进行行业深度调研及分析，起草中英文投资建议报告。",
                recruiterId = "HR_GOLDMAN"
            ),
            RecruitingJob(
                id = "JOB_TENCENT",
                title = "产品经理助理 (科创项目)",
                company = "腾讯科创实验室",
                salary = "12k-20k",
                location = "深圳",
                avatarColor = 0xFFFF7096,
                mandatoryCerts = listOf("暂无硬性前置限制"),
                preferredCerts = listOf("大学英语六级 (CET6)", "计算机软件资格考试 (软考中级)"),
                description = "主导移动端创新产品原型交互，进行高保真PRD编写及全生命周期研发敏捷管理，擅长MBTI性格创意直觉。",
                recruiterId = "TUTOR_AI"
            ),
            RecruitingJob(
                id = "JOB_PWC",
                title = "初级审计顾问",
                company = "普华永道 (PwC) 审计部",
                salary = "10k-16k",
                location = "广州",
                avatarColor = 0xFFE07A5F,
                mandatoryCerts = listOf("基金从业资格"),
                preferredCerts = listOf("注册会计师 (CPA)", "大学英语六级 (CET6)"),
                description = "执行大型集团企业财务报表审计及内部控制合规评估，分析重大财务勾稽关系，输出审计工作底稿。",
                recruiterId = "HR_GOLDMAN"
            )
        )
    )

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
                    "你是一位高校大学生生涯规划师，精通各行业证书体系 and 考证路径。用亲切、客观、充满正能量的语气，解答大学生关于选专业、考证加分、考公务员、进入互联网大厂或外企的求职疑惑。回答要结合专业证书的重要门槛性，字数控制在200字以内，排版多用Emoji、空行和序号。"
                } else {
                    "你是一位资深的高校大厂求职指导教练和简历润色Agent。任务是帮助零实习、零经验的大学生，将在校普通的杂事经历（如拉赞助、做志愿者、期末大作业、打字整理、社团搬砖、画画排版、做PPT等），翻译优化为适应职场的、含金量极高且符合大厂高频需求的STAR法则格式经历。你的回答格式必须是：首先用风趣幽默的语言鼓励少侠，然后给出【优化前】和【AI 深度重构后（STAR法则）】的强烈对比，最后给出一小条【求职加分建议】。总长度控制在300字以内，多空行排版。"
                }
                callGeminiApi(systemInstruction, text)
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
        if (systemInstruction.contains("简历润色")) {
            return when {
                prompt.contains("赞助") || prompt.contains("外联") || prompt.contains("钱") -> {
                    "少侠！拉赞助可不只是跑腿，这可是妥妥的 BD (商业拓展) 战绩！🔮\n\n" +
                    "【优化前】\n在学生会外联部去校外找商家拉了500块钱赞助印传单。\n\n" +
                    "【AI 深度重构后（STAR法则）】\n★【商业拓展与项目交付（校园合作方向）】\n" +
                    "• S/T (背景): 针对校级千人科创峰会，负责外部赞助商合作谈判，填补 500 元宣传资金缺口。\n" +
                    "• A (行动): 独立梳理校方周边 20 家商家画像并进行需求对齐，制定定制化校园品牌路演方案；逐一拜访商户决策人展现曝光价值。\n" +
                    "• R (结果): 最终成功达成 1 家核心商户的独家赞助，赞助金 100% 交付，品牌覆盖全校 2000+ 学子。\n\n" +
                    "【💡 求职加分建议】\n不要提“发传单、要钱”，大厂更喜欢听“高频转化、商家路演、多维合作谈判”这样的商业词汇。多把这段话写进你的简历里，绝对加分！🚀"
                }
                prompt.contains("传单") || prompt.contains("印") || prompt.contains("发") || prompt.contains("排版") -> {
                    "少侠！发传单、排版物料绝非打杂，这是经典的 AARRR 增长运营与物料视觉策划！🔮\n\n" +
                    "【优化前】\n在社团帮忙排版并印刷、分发了招新传单。\n\n" +
                    "【AI 深度重构后（STAR法则）】\n★【校园用户增长与视觉物料策划（运营岗方向）】\n" +
                    "• S/T (背景): 为突破科创社团季度招新人数，负责招新视觉物料排版与线下分发，面临转化率提升痛点。\n" +
                    "• A (行动): 优化传单排版视觉体系，提炼核心入群痛点福利；选择在上下课人流量最大的食堂及校门口进行网格化定向分发。\n" +
                    "• R (结果): 传单的扫码入群率达到 12%，一天内为社团引流 150+ 垂直科创兴趣人群，超额完成招新 KPI。\n\n" +
                    "【💡 求职加分建议】\n不要在简历中流露廉价劳动力色彩，要讲“增长路径”、“扫码转化率”、“网格化运营”，高级感立刻扑面而来！✨"
                }
                prompt.contains("作业") || prompt.contains("代码") || prompt.contains("写") || prompt.contains("高数") || prompt.contains("计算器") || prompt.contains("程序") -> {
                    "少侠！课设和作业可是大学生没有实习时，写进简历的唯一“研发经历”！千万要写好！🔮\n\n" +
                    "【优化前】\n期末写过一个计算器/学生大作业代码，拿了90分。\n\n" +
                    "【AI 深度重构后（STAR法则）】\n★【基于数据字典的并发数值计算器（独立项目）】\n" +
                    "• S/T (背景): 针对期末课程设计中，高并发下基础数学解析系统出现的栈溢出及性能耗损，主导优化重构。\n" +
                    "• A (行动): 采用 Python 构建底层数据架构，优化了原本冗余的循环逻辑，算法复杂度由 O(N²) 缩减为 O(N)。\n" +
                    "• R (结果): 最终实现系统在极限运算场景下的数据吞吐耗时缩短 35%，项目获评 90+ 优秀（Top 5%），并作为样板进行班级演示。\n\n" +
                    "【💡 求职加分建议】\n不要写“做作业”，要写“独立研发项目”；把计算器写成“数值计算系统”，写出你的内存/时间复杂度优化，HR 看完直接安排面试！🔥"
                }
                else -> {
                    "少侠！这段经历蕴含着极佳的职场潜质，看我用 STAR 原则帮它镀一层金！🔮\n\n" +
                    "【优化前】\n${prompt}\n\n" +
                    "【AI 深度重构后（STAR法则）】\n★【团队项目交付与核心执行（通用方向）】\n" +
                    "• S/T (背景): 面对无职场经验、资源约束紧迫等挑战，作为核心执行人，面临指标重组与高水准交付的考核压力。\n" +
                    "• A (行动): 系统重构执行策略，优化工作量矩阵，将日常杂务梳理成 3 大结构化执行条目，实现任务进度的可视化跟踪。\n" +
                    "• R (结果): 提前达成预设目标，实现了工作效率的多维跃进，完美切合职场核心抗压和结果导向技能。\n\n" +
                    "•【💡 求职加分建议】\n不管做什么经历，多提“流程重构”、“指标量化”和“高效执行”，你就是 HR 眼里的高潜力人才！💪"
                }
            }
        } else {
            return "少侠！我是你的 AI 生涯规划师。想探索考证路线吗？建议直接回复您的专业或意向岗位，我将利用 Gemini API 为您推算最对口的考证路径与避坑经验。也可以去【双向检索】中直接查查属于你的证书图谱哦！"
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
