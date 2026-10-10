package com.example.data.model

/**
 * 🏢 企业端数据实体定义
 * 包含：企业候选人、企业发布职位、面试邀约、企业沟通会话等
 */

// 用户角色模式
enum class UserRoleMode {
    UNSELECTED,   // 首次进入 / 未选择
    PERSONAL,     // 个人求职者账户 (查考证·名企直聘·大学生搭伙)
    ENTERPRISE    // 企业招聘官账户 (发布职位·搜持证人才·AI简历初筛·邀约面试)
}

// 候选人实体 (企业端查看的人才画像)
data class EnterpriseCandidate(
    val id: String,
    val name: String,
    val avatarColor: Long,
    val targetPosition: String,
    val expectedSalary: String,
    val school: String,
    val degree: String,
    val graduationYear: String,
    val jobStatus: String, // 例如："离校实习 · 随时到岗"、"在职看机会"
    val matchScore: Int,   // 契合度评分 (e.g. 98)
    val mandatoryCerts: List<String>, // 持有国家硬性准入/关键资格证书
    val skillTags: List<String>,
    val highlights: String, // 核心亮点 (STAR 法则提炼)
    val workOrProjectSummary: String,
    val isBookmarked: Boolean = false,
    val isInvited: Boolean = false,
    val gender: String = "男"
)

// 企业已发布的招聘职位
data class EnterpriseJobPosting(
    val id: String,
    val title: String,
    val department: String,
    val salary: String,
    val location: String,
    val headCount: Int,
    val mandatoryCerts: List<String>,  // 硬性持证准入要求
    val preferredCerts: List<String>,  // 优选证书
    val experienceReq: String,          // 经验要求 (e.g. "应届/在校实习", "1-3年")
    val educationReq: String,           // 学历要求 (e.g. "本科及以上")
    val jobDescription: String,
    val applicantCount: Int,            // 投递简历数
    val viewCount: Int,                 // 浏览量
    val isOnline: Boolean = true,       // 在招中 / 已暂停
    val postedTime: String = "刚刚"
)

// 面试邀约实体
data class InterviewInvite(
    val id: String,
    val candidateId: String,
    val candidateName: String,
    val positionTitle: String,
    val interviewTime: String,     // 例如 "2026-10-15 14:30"
    val interviewType: String,     // "线上视频面试 (AI智联会议/飞书)" or "线下现场面试"
    val locationOrLink: String,
    val notes: String,
    val status: String = "待候选人确认" // "待候选人确认", "已接受", "已完成"
)

// 企业端候选人沟通记录
data class CandidateConversation(
    val id: String,
    val candidateId: String,
    val candidateName: String,
    val candidateSchool: String,
    val candidateCertTag: String,
    val targetPosition: String,
    val avatarColor: Long,
    val lastMessage: String,
    val lastMessageTime: String,
    val hasUnread: Boolean = false,
    val unreadCount: Int = 0
)

// 企业信息实体
data class EnterpriseCompanyProfile(
    val companyName: String,
    val shortName: String,
    val industry: String,
    val scale: String,          // 人员规模，如 "10000人以上"
    val financeStage: String = "已上市",   // 融资阶段，如 "已上市"
    val address: String = "深圳市南山区粤海街道高新南十二路88号星澜智谷研发中心A座",
    val creditCode: String = "91440300MA5H8X9Q2L",     // 统一社会信用代码
    val isVerified: Boolean = true,
    val recruiterName: String,
    val recruiterTitle: String,
    val recruiterWorkNo: String,
    val recruiterDepartment: String = "人力资源平台 / 校园招聘与雇主品牌部",
    val recruiterPhone: String = "138-0013-8820",
    val recruiterEmail: String = "recruiter@starstream-ai.com",
    val recruiterWelcomeMsg: String = "渴望与持有硬核专业资格证书的优秀年轻人才一同创造未来！",
    val slogan: String,
    val welfareTags: List<String>
)
