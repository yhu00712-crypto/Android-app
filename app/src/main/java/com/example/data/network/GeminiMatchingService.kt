package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// AI 诊断结果实体
data class AiMatchedItem(
    val title: String,               // 对应的证书名（或反向推导时的职业名）
    val requirementType: String,      // "MANDATORY" (法定硬性) 或 "PREFERRED" (优选加分)
    val score: Int,                  // 匹配度 (80~99)
    val policyAnalysis: String       // 权威政策红线与竞争力解析
)

object GeminiMatchingService {
    private const val TAG = "GeminiMatchingService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * 1. 正向推导：输入【职业】，AI 匹配出必须或优选的【证书清单】
     */
    suspend fun matchCertificatesForOccupation(occupationName: String, occupationDesc: String): List<AiMatchedItem> =
        withContext(Dispatchers.IO) {
            val prompt = """
                你是一名国家人社部与高校就业指导中心专家。
                目标职业：【$occupationName】
                岗位职责描述：$occupationDesc
                
                请为该职业智能匹配并推导出 3~5 本国家认可的高价值专业证书或执业资质。
                必须明确区分：
                - "MANDATORY": 国家法规规定的法定持证上岗红线门槛。
                - "PREFERRED": 行业大厂与国企核心筛选加分/定级津贴项。
                
                必须直接返回纯合法的 JSON 数组，严禁包含 markdown 代码块包裹标记，格式如下：
                [
                  {
                    "title": "证书全称",
                    "requirementType": "MANDATORY或PREFERRED",
                    "score": 95,
                    "policyAnalysis": "详细说明国家法规红线条例或行业招聘加分理由"
                  }
                ]
            """.trimIndent()

            try {
                val results = callGemini(prompt)
                if (results.isNotEmpty()) results else getFallbackCertificates(occupationName)
            } catch (e: Exception) {
                Log.e(TAG, "Gemini matching error: ${e.message}", e)
                getFallbackCertificates(occupationName)
            }
        }

    /**
     * 2. 反向推导：输入【证书】，AI 反向拓宽其【可就业职业清单】
     */
    suspend fun matchOccupationsForCertificate(certificateName: String, certDesc: String): List<AiMatchedItem> =
        withContext(Dispatchers.IO) {
            val prompt = """
                你是一名国家职业规划与权威考证发展专家。
                用户持有的证书：【$certificateName】
                证书权威背景：$certDesc
                
                请为持有该证书的人才，反向拓宽其求职视野，匹配出 3~5 个对口度极高的高价值就业岗位（涵盖国企、大厂、事业单位或专业机构）。
                必须直接返回纯合法的 JSON 数组，格式严格如下：
                [
                  {
                    "title": "职业名称",
                    "requirementType": "MANDATORY或PREFERRED",
                    "score": 92,
                    "policyAnalysis": "说明该证书在应聘此岗位时的核心竞争壁垒与执业特权"
                  }
                ]
            """.trimIndent()

            try {
                val results = callGemini(prompt)
                if (results.isNotEmpty()) results else getFallbackOccupations(certificateName)
            } catch (e: Exception) {
                Log.e(TAG, "Gemini reverse matching error: ${e.message}", e)
                getFallbackOccupations(certificateName)
            }
        }

    private fun callGemini(promptText: String): List<AiMatchedItem> {
        var apiKey = BuildConfig.GEMINI_API_KEY.trim().trim('"', '\'', ' ')
        if (apiKey.startsWith("GEMINI_API_KEY", ignoreCase = true)) {
            apiKey = apiKey.substringAfter("=").trim().trim('"', '\'', ' ')
        }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "GEMINI_API_KEY 未配置真实密钥，触发智能离线兜底规则")
            return emptyList()
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", promptText)
                }))
            }))
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API HTTP Error: ${response.code} ${response.message}")
                return emptyList()
            }
            val respString = response.body?.string() ?: return emptyList()

            val root = JSONObject(respString)
            val candidates = root.optJSONArray("candidates") ?: return emptyList()
            if (candidates.length() == 0) return emptyList()

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return emptyList()
            val parts = content.optJSONArray("parts") ?: return emptyList()
            if (parts.length() == 0) return emptyList()

            val text = parts.getJSONObject(0).optString("text", "").trim()
            val cleanJson = if (text.startsWith("```json")) {
                text.removePrefix("```json").substringBeforeLast("```").trim()
            } else if (text.startsWith("```")) {
                text.removePrefix("```").substringBeforeLast("```").trim()
            } else {
                text
            }

            val jsonArray = JSONArray(cleanJson)
            val list = mutableListOf<AiMatchedItem>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                list.add(
                    AiMatchedItem(
                        title = item.optString("title", "专业资质"),
                        requirementType = item.optString("requirementType", "PREFERRED"),
                        score = item.optInt("score", 90),
                        policyAnalysis = item.optString("policyAnalysis", "行业核心胜任力加分背书。")
                    )
                )
            }
            return list
        }
    }

    // 智能领域降级兜底生成规则 (覆盖各大行业权威法定红线与招聘真实加分项)
    private fun getFallbackCertificates(occupationName: String): List<AiMatchedItem> {
        val name = occupationName.lowercase()
        return when {
            name.contains("消") || name.contains("防") -> listOf(
                AiMatchedItem("一级注册消防工程师", "MANDATORY", 98, "应急管理部消防局权威法定资质，承担大中型楼宇、高危场所消防设计审核与中控室操作红线。"),
                AiMatchedItem("中级消防设施操作员", "MANDATORY", 95, "人社部与应急管理部联合颁发，消防控制室值班操作法定上岗门槛。"),
                AiMatchedItem("中级注册安全工程师", "PREFERRED", 88, "国家安全生产法硬性规定的安全总监与风险排查核心加分资质。")
            )
            name.contains("教") || name.contains("师") || name.contains("学") -> listOf(
                AiMatchedItem("高级中学教师资格证", "MANDATORY", 99, "教育部国家法定准入红线，公办与私立学校正式学科教学上岗前置凭证。"),
                AiMatchedItem("普通话水平测试 (二级甲等及以上)", "MANDATORY", 96, "国家语委硬性规定：从事中小学及语言教学岗位必须达标等级。"),
                AiMatchedItem("英语专业八级证书 (TEM-8)", "PREFERRED", 90, "重点高中或外语特色教学机构招录的第一道简历优选硬件指标。")
            )
            name.contains("律") || name.contains("法") || name.contains("合规") -> listOf(
                AiMatchedItem("法律职业资格证书 (A证)", "MANDATORY", 99, "司法部法定前置红线，从事专职执业律师、合规总监的唯一法定资质。"),
                AiMatchedItem("专利代理师资格证", "PREFERRED", 92, "知识产权局认可资质，知识产权诉讼与高价值专利布局双证复合黄金溢价。"),
                AiMatchedItem("大学英语六级 (CET-6 500+)", "PREFERRED", 85, "红圈律所与大型出海企业审核英文跨国商业采购协议必备。")
            )
            name.contains("会") || name.contains("财") || name.contains("审") || name.contains("税") -> listOf(
                AiMatchedItem("注册会计师 (CPA)", "MANDATORY", 98, "中国注册会计师协会唯一审计报告签字权，四大会计所与国企财务主管晋升天花板。"),
                AiMatchedItem("初级/中级会计专业技术资格", "MANDATORY", 93, "财政部法定会计岗位准入评定，企业总账核算与税务申报必备。"),
                AiMatchedItem("商业数据分析师 (CDA LEVEL I)", "PREFERRED", 86, "现代IT审计与海量银行业务流水核对的核心技术加分工具。")
            )
            name.contains("银") || name.contains("金") || name.contains("证") || name.contains("理财") || name.contains("基金") -> listOf(
                AiMatchedItem("证券从业资格证", "MANDATORY", 97, "证券业协会规定进入券商、投行及分析师岗位核心法定门槛。"),
                AiMatchedItem("基金从业资格证", "MANDATORY", 95, "证券投资基金业协会规定公募、私募基金推介与投资经理法定持证要求。"),
                AiMatchedItem("银行业专业人员初级职业资格", "MANDATORY", 92, "商业银行网点柜面业务及客户经理考核晋升基础资质。")
            )
            name.contains("建") || name.contains("造") || name.contains("工") || name.contains("暖通") || name.contains("市政") || name.contains("监理") -> listOf(
                AiMatchedItem("二级建造师 (机电/建筑/市政)", "MANDATORY", 98, "住建部规定：担任工程施工现场项目经理与技术负责人的法定红线执照。"),
                AiMatchedItem("中级注册安全工程师", "MANDATORY", 95, "安全生产法规定工程总包与高危作业现场EHS安全监督红线。"),
                AiMatchedItem("一级建造师", "PREFERRED", 92, "特级建筑企业承接国家级大型工程施工总承包的核心配置资质。")
            )
            name.contains("药") || name.contains("医") || name.contains("护") -> listOf(
                AiMatchedItem("执业药师资格证", "MANDATORY", 99, "国家药监局规定零售药房处方审核与合规用药指导唯一合法在岗证书。"),
                AiMatchedItem("卫生专业技术资格", "MANDATORY", 94, "卫生健康委医疗体系职称评定与临床合规从业通道。")
            )
            name.contains("心") || name.contains("社") || name.contains("人") || name.contains("hr") || name.contains("行政") -> listOf(
                AiMatchedItem("企业人力资源管理师 (三级/二级)", "PREFERRED", 92, "人社部权威认证，涵盖企业招聘、培训、绩效薪酬全模块规范实操。"),
                AiMatchedItem("心理咨询师专业技能培训证书", "PREFERRED", 90, "中科院心理所专业认证，掌握情绪疏导、危机干预与员工EAP援助。"),
                AiMatchedItem("项目管理专业人士认证 (PMP)", "PREFERRED", 86, "跨部门推进重大行政与人资变革项目国际通用项目管理标准。")
            )
            name.contains("译") || name.contains("外") || name.contains("语") || name.contains("导") -> listOf(
                AiMatchedItem("全国翻译专业资格证书 (CATTI 二级)", "MANDATORY", 98, "国家外文局权威认证，外事同传与高级涉外商贸谈判唯一国家译审标准。"),
                AiMatchedItem("全国导游资格证书", "MANDATORY", 96, "文化和旅游部规定在国内正规带团讲解法定必须悬挂证书。"),
                AiMatchedItem("英语专业八级证书 (TEM-8)", "PREFERRED", 92, "涉外语料翻译、国际期刊审校高校公认顶级外语水平背书。")
            )
            name.contains("安") || name.contains("网") || name.contains("攻") || name.contains("黑") -> listOf(
                AiMatchedItem("注册信息安全专业人员 (CISP)", "MANDATORY", 98, "中国信息安全测评中心官方背书，大厂网安合规与等保测评必备资质。"),
                AiMatchedItem("软件设计师 (软考中级)", "PREFERRED", 90, "计算机技术与软件中级职称，事业单位与国企评薪硬性指标。")
            )
            name.contains("后") || name.contains("软") || name.contains("程") || name.contains("云") || name.contains("数") -> listOf(
                AiMatchedItem("软件设计师 (软考中级)", "PREFERRED", 94, "工信部与人社部中级工程师职称，入编、国企评级及落户绿色通道。"),
                AiMatchedItem("程序员 (软考初级)", "PREFERRED", 88, "高校低年级与非科班同学证明代码规范与算法基本功的高效凭证。"),
                AiMatchedItem("商业数据分析师 (CDA LEVEL I)", "PREFERRED", 87, "掌握SQL多表复杂关联挖掘与指标体系搭建的大厂实操凭证。")
            )
            else -> listOf(
                AiMatchedItem("行业对口中高级职业技能等级证书", "PREFERRED", 90, "国家人社部认可职业等级，企业定岗定级与技能津贴发放重要参考。"),
                AiMatchedItem("项目管理专业人士认证 (PMP)", "PREFERRED", 85, "国际通用项目管理体系，证明跨部门推进业务交付与资源协调硬实力。")
            )
        }
    }

    private fun getFallbackOccupations(certName: String): List<AiMatchedItem> {
        val name = certName.lowercase()
        return when {
            name.contains("消") || name.contains("安全") -> listOf(
                AiMatchedItem("消防设施操作巡查员", "MANDATORY", 99, "大型商业综合体、数据中心与中控室操作的法定持证红线岗位。"),
                AiMatchedItem("企业 EHS 安全生产主管", "MANDATORY", 95, "负责工厂特种设备合规、重大隐患排查与工伤应急处理的核心管理岗。"),
                AiMatchedItem("建筑机电消防工程师", "PREFERRED", 92, "负责商业建筑消防管网、自动喷淋与排烟系统施工指导及竣工验收。")
            )
            name.contains("教") || name.contains("普通话") -> listOf(
                AiMatchedItem("公办/民办中学学科教师", "MANDATORY", 99, "必须持对应学科教资及普通话二甲证书方可参加教师编招聘与执教。"),
                AiMatchedItem("国际双语学校教学辅导导师", "PREFERRED", 92, "负责中外教课程协同辅导、双语教学组织与家校成长沟通。"),
                AiMatchedItem("高校就业/生涯规划指导教师", "PREFERRED", 88, "从事大学生成长规划、赛事实践辅导与职业素养培育。")
            )
            name.contains("法") -> listOf(
                AiMatchedItem("执业诉讼律师", "MANDATORY", 99, "必须持证才能独立开庭代理诉讼，享全额案源提成收益。"),
                AiMatchedItem("大厂法务专员/合规经理", "PREFERRED", 92, "享有专业法律津贴，负责核心业务合同审查与数据合规风控。"),
                AiMatchedItem("投行/券商内核质控合规岗", "PREFERRED", 89, "高薪金融中后台核心岗位，偏好具备法考背景的复合型人才。")
            )
            name.contains("cpa") || name.contains("会计") || name.contains("财") || name.contains("审") -> listOf(
                AiMatchedItem("四大会计师事务所初级审计员", "PREFERRED", 96, "持证即享月度千元 Q-Pay 津贴，也是晋升审计经理的法定资质。"),
                AiMatchedItem("上市公司总账会计/税务专员", "PREFERRED", 92, "负责复杂财务报表合并编制、所得税汇算清缴与税优申请。"),
                AiMatchedItem("股权投资机构财务尽调专家", "PREFERRED", 90, "负责拟投企业财务真实性核查与资产估值建模。")
            )
            name.contains("软考") || name.contains("计算机") || name.contains("程序") -> listOf(
                AiMatchedItem("国企/事业单位数字化研发工程师", "PREFERRED", 95, "入职即认中级/高级工程师职称，享受职称补贴与人才落户优待。"),
                AiMatchedItem("高并发后端架构开发工程师", "PREFERRED", 91, "扎实底层计算机系统结构与高可用分布式设计能力背书。"),
                AiMatchedItem("政企业务信息化技术总监", "PREFERRED", 88, "政府采购与大型标书招投标中作为团队核心技术人员的关键资质。")
            )
            name.contains("建造") || name.contains("工程") -> listOf(
                AiMatchedItem("建筑施工项目总工程师 / 项目经理", "MANDATORY", 98, "建筑施工企业总承包项目现场法定必须到岗履职的项目经理红线。"),
                AiMatchedItem("现场机电工程监理", "PREFERRED", 91, "负责工程质量隐患把控、原材料进场检验与安全资料签证。")
            )
            name.contains("药") -> listOf(
                AiMatchedItem("连锁药房执业药师 / 店长", "MANDATORY", 99, "负责零售药店处方药销售审核与合规经营法定负责人。"),
                AiMatchedItem("制药企业质量管理 (QA/QC)", "PREFERRED", 92, "负责药品生产工艺合规质控与国家 GMP 飞行检查迎检。")
            )
            else -> listOf(
                AiMatchedItem("行业对口核心技术骨干", "PREFERRED", 92, "持证享受岗位优先录用权与企业专项技能津贴。"),
                AiMatchedItem("中层业务主管 / 团队管理者", "PREFERRED", 86, "晋升团队管理层与中层技术负责人的硬实力背书。")
            )
        }
    }
}
