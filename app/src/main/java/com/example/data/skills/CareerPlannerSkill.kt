package com.example.data.skills

import org.json.JSONObject

/**
 * 资深职业生涯规划师与公考/企招咨询专家 Skill
 * 用于根据用户背景与推荐岗位，调用 DeepSeek 输出结构化 JSON 职业规划方案
 */
object CareerPlannerSkill {
    const val SYSTEM_PROMPT_TEMPLATE = """# Role
你是一名资深职业生涯规划师与公考/企招咨询专家。你的任务是根据用户的“个人背景信息”和“数据库推荐岗位”，为用户量身定制一份结构化、可执行的职业规划方案。

# Constraints & Rules
1. 语言风格：专业、客观、鼓励性，避免废话。
2. 建议必须具体：明确指出用户在专业、学历或证书方面的优劣势。
3. 证书推荐逻辑：若目标岗位需要某种证书而用户缺失，必须列出该证书的“考取难度”与“优先推荐度”。
4. 输出格式：必须严格输出标准 JSON 格式，方便 App 前端解析渲染 UI。不要包含多余的开场白或寒暄，仅输出可被解析的 JSON 字符串。

# Input Context
- 用户专业：{user_major}
- 用户学历：{user_education}
- 已持有证书：{user_certs}
- 目标方向：{user_goal} (例如：考公/进国企/大厂UI/系统开发)
- 匹配到的岗位数据：{matched_jobs_json}

# Output Format (JSON)
{
  "career_summary": "一句话评估用户的竞争力和当前匹配度",
  "match_score": 85,
  "advantages": ["优势1", "优势2"],
  "gaps": ["劣势/不足1", "劣势/不足2"],
  "short_term_roadmap": {
    "timeline": "1-3个月（准备冲刺期）",
    "action_items": ["具体动作1", "建议考取的证书A"]
  },
  "mid_term_roadmap": {
    "timeline": "6-12个月（稳固提升期）",
    "action_items": ["目标岗位投递/备考建议", "技能补强"]
  },
  "recommended_certificates": [
    {
      "cert_name": "法律职业资格证书(A证)",
      "reason": "可解锁税务局/司法局等高含金量岗位",
      "priority": "HIGH"
    }
  ]
}"""

    fun buildSystemPrompt(
        userMajor: String,
        userEducation: String,
        userCerts: String,
        userGoal: String,
        matchedJobsJson: String
    ): String {
        return SYSTEM_PROMPT_TEMPLATE
            .replace("{user_major}", userMajor.ifBlank { "普通全日制专业" })
            .replace("{user_education}", userEducation.ifBlank { "本科在读" })
            .replace("{user_certs}", userCerts.ifBlank { "暂无专业准入证书（待规划）" })
            .replace("{user_goal}", userGoal.ifBlank { "综合考公/国企/大厂就业" })
            .replace("{matched_jobs_json}", matchedJobsJson.ifBlank { "[]" })
    }

    fun parseReport(text: String): CareerPlanningReport? {
        try {
            var cleanJson = text.trim()
            // 如果被 ```json 包裹，提取 JSON 内容
            if (cleanJson.contains("```json")) {
                val start = cleanJson.indexOf("```json") + 7
                val end = cleanJson.lastIndexOf("```")
                if (end > start) {
                    cleanJson = cleanJson.substring(start, end).trim()
                }
            } else if (cleanJson.contains("```")) {
                val start = cleanJson.indexOf("```") + 3
                val end = cleanJson.lastIndexOf("```")
                if (end > start) {
                    cleanJson = cleanJson.substring(start, end).trim()
                }
            }

            val startBrace = cleanJson.indexOf('{')
            val endBrace = cleanJson.lastIndexOf('}')
            if (startBrace == -1 || endBrace == -1 || endBrace <= startBrace) return null

            cleanJson = cleanJson.substring(startBrace, endBrace + 1)
            val json = JSONObject(cleanJson)

            val careerSummary = json.optString("career_summary", "")
            val matchScore = json.optInt("match_score", 80)

            val advantages = mutableListOf<String>()
            json.optJSONArray("advantages")?.let { arr ->
                for (i in 0 until arr.length()) {
                    advantages.add(arr.getString(i))
                }
            }

            val gaps = mutableListOf<String>()
            json.optJSONArray("gaps")?.let { arr ->
                for (i in 0 until arr.length()) {
                    gaps.add(arr.getString(i))
                }
            }

            var shortTerm: RoadmapPeriod? = null
            json.optJSONObject("short_term_roadmap")?.let { obj ->
                val timeline = obj.optString("timeline", "1-3个月（准备冲刺期）")
                val items = mutableListOf<String>()
                obj.optJSONArray("action_items")?.let { arr ->
                    for (i in 0 until arr.length()) items.add(arr.getString(i))
                }
                shortTerm = RoadmapPeriod(timeline, items)
            }

            var midTerm: RoadmapPeriod? = null
            json.optJSONObject("mid_term_roadmap")?.let { obj ->
                val timeline = obj.optString("timeline", "6-12个月（稳固提升期）")
                val items = mutableListOf<String>()
                obj.optJSONArray("action_items")?.let { arr ->
                    for (i in 0 until arr.length()) items.add(arr.getString(i))
                }
                midTerm = RoadmapPeriod(timeline, items)
            }

            val recCerts = mutableListOf<RecommendedCertItem>()
            json.optJSONArray("recommended_certificates")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    recCerts.add(
                        RecommendedCertItem(
                            certName = obj.optString("cert_name", "资格证书"),
                            reason = obj.optString("reason", "岗位必备资质"),
                            priority = obj.optString("priority", "HIGH")
                        )
                    )
                }
            }

            if (careerSummary.isNotBlank() || advantages.isNotEmpty() || recCerts.isNotEmpty()) {
                return CareerPlanningReport(
                    careerSummary = careerSummary,
                    matchScore = matchScore,
                    advantages = advantages,
                    gaps = gaps,
                    shortTermRoadmap = shortTerm,
                    midTermRoadmap = midTerm,
                    recommendedCertificates = recCerts
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}

data class CareerPlanningReport(
    val careerSummary: String,
    val matchScore: Int,
    val advantages: List<String>,
    val gaps: List<String>,
    val shortTermRoadmap: RoadmapPeriod?,
    val midTermRoadmap: RoadmapPeriod?,
    val recommendedCertificates: List<RecommendedCertItem>
)

data class RoadmapPeriod(
    val timeline: String,
    val actionItems: List<String>
)

data class RecommendedCertItem(
    val certName: String,
    val reason: String,
    val priority: String // HIGH, MEDIUM, LOW
)
