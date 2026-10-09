package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Embedded

// 1. 标准职业实体
@Entity(tableName = "occupations")
data class Occupation(
    @PrimaryKey val id: String,                 // 例如 "OCC_LEGAL_ADV"
    val name: String,                           // 职业名称，如 "法务助理/合规专员"
    val category: String,                       // 分类，如 "法律/财务"、"低门槛·高成长"
    val salaryExpectation: String,              // 薪资中位数，如 "6k-10k"
    val description: String                     // 职业职责描述
)

// 2. 权威证书实体
@Entity(tableName = "certificates")
data class Certificate(
    @PrimaryKey val id: String,                 // 例如 "CERT_LEGAL"
    val name: String,                           // 证书名称，如 "法律职业资格证书 (A证)"
    val authority: String,                      // 发证机构，如 "司法部"
    val difficulty: String,                     // 考试难度，如 "极高"
    val examFrequency: String,                  // 考试频次，如 "每年1次"
    val description: String                     // 证书功能简介
)

// 3. 高密度多对多交叉关联表
@Entity(
    tableName = "occupation_certificate_cross_ref",
    primaryKeys = ["occupationId", "certificateId"],
    foreignKeys = [
        ForeignKey(
            entity = Occupation::class,
            parentColumns = ["id"],
            childColumns = ["occupationId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Certificate::class,
            parentColumns = ["id"],
            childColumns = ["certificateId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class OccupationCertificateCrossRef(
    val occupationId: String,                   // 关联职业ID
    val certificateId: String,                  // 关联证书ID
    val requirementType: String,                // 关系类型："MANDATORY" (法定硬性准入) 或 "PREFERRED" (优选行业加分)
    val benefitDescription: String              // 具体政策和加分依据描述
)

// 4. 双向查询结构体
data class CertificateWithRelationInfo(
    @Embedded val certificate: Certificate,
    val requirementType: String,
    val benefitDescription: String
)

data class OccupationWithRelationInfo(
    @Embedded val occupation: Occupation,
    val requirementType: String,
    val benefitDescription: String
)

// 5. 搜索直显聚合结构体（取消下拉折叠，全量信息在单个UI中直展呈现）
data class UnifiedOccupationResult(
    val occupation: Occupation,
    val mandatoryCert: Certificate? = null,
    val requirementType: String = "MANDATORY",
    val legalNote: String = "",
    val recruiterId: String? = null,
    val recruiterCompany: String? = null
)

data class UnifiedCertificateResult(
    val certificate: Certificate,
    val relatedOccupations: List<Occupation> = emptyList()
)

// 6. 国家职业资格分类政策与说明助手 (准入类 vs 水平评价类)
object QualificationPolicyHelper {
    const val ACCESS_NAME = "准入类职业资格"
    const val ACCESS_SHORT = "准入类"
    const val ACCESS_SLOGAN = "无证不得执业"
    const val ACCESS_EXPLANATION = "依法实行准入控制，无证不得执业。关系公共利益、国家安全或生命财产安全，必须依法取得证书方可合法上岗从事该职业。"

    const val EVAL_NAME = "水平评价类职业资格"
    const val EVAL_SHORT = "水平评价类"
    const val EVAL_SLOGAN = "衡量技术水平，非强制执业"
    const val EVAL_EXPLANATION = "衡量专业技术水平与实操能力，非强制执业准入。供企事业单位选拔、聘任专业技术职务、定岗定级、技能津贴及职称评定参考。"

    /**
     * 判断证书属于准入类还是水平评价类
     * 依据人社部《国家职业资格目录》：
     * 准入类：法律职业资格、注册会计师(CPA)、教师资格、建造师、消防工程师、执业医师/药师、导游资格、民航驾驶、注册安全工程师等
     * 水平评价类：软考系列、翻译专业资格(CATTI)、证券/基金/期货、精算师、经济专业技术、电子商务师等
     */
    fun isAccessQualification(certName: String): Boolean {
        val n = certName.lowercase()
        return n.contains("法律") || n.contains("法考") || n.contains("教师") || n.contains("会计师") ||
               n.contains("cpa") || n.contains("消防") || n.contains("建造") || n.contains("执业") ||
               n.contains("医师") || n.contains("药师") || n.contains("导游") || n.contains("普通话") ||
               n.contains("caac") || n.contains("飞行") || n.contains("安全工程") || n.contains("特种")
    }

    fun getCategoryInfo(certName: String): QualificationCategoryInfo {
        return if (isAccessQualification(certName)) {
            QualificationCategoryInfo(
                categoryName = ACCESS_NAME,
                shortTag = ACCESS_SHORT,
                slogan = ACCESS_SLOGAN,
                explanation = ACCESS_EXPLANATION,
                isAccess = true
            )
        } else {
            QualificationCategoryInfo(
                categoryName = EVAL_NAME,
                shortTag = EVAL_SHORT,
                slogan = EVAL_SLOGAN,
                explanation = EVAL_EXPLANATION,
                isAccess = false
            )
        }
    }
}

data class QualificationCategoryInfo(
    val categoryName: String,
    val shortTag: String,
    val slogan: String,
    val explanation: String,
    val isAccess: Boolean
)

