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
