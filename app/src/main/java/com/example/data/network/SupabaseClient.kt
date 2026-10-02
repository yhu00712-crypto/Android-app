package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Certificate
import com.example.data.model.CertificateWithRelationInfo
import com.example.data.model.Occupation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class CloudCrossRef(
    val occupationName: String,
    val certificateName: String,
    val requirementType: String,
    val matchScore: Int,
    val benefitDescription: String,
    val direction: String
)

object SupabaseClient {
    private const val TAG = "SupabaseClient"
    private const val DEFAULT_SUPABASE_URL = "https://wnvvkoboeroeetixszku.supabase.co/rest/v1/"
    private const val DEFAULT_SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndudnZrb2JvZXJvZWV0aXhzemt1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTA2NjYxMDIsImV4cCI6MjEwNjI0MjEwMn0.lyiYgWmCSo9dZEdHWqroognO53MmlnSMap7m6baN5Cg"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * 智能格式化并清洗 Supabase REST Base URL，防止拼错、包含环境变量名前缀 (如 SUPABASE_URL=)、引号或 Dashboard 网页链接
     */
    private fun getBaseUrl(): String {
        var raw = BuildConfig.SUPABASE_URL.trim().trim('"', '\'', ' ')

        // 1. 去除环境变量名前缀 (例如 "SUPABASE_URL=https://..." 或 "SUPABASE_URL = ...")
        if (raw.startsWith("SUPABASE_URL", ignoreCase = true)) {
            raw = raw.substringAfter("=").trim().trim('"', '\'', ' ')
        }

        // 2. 如果包含多余字符，定位 http:// 或 https:// 起始位置
        val httpIdx = raw.indexOf("http://")
        val httpsIdx = raw.indexOf("https://")
        val startIdx = when {
            httpsIdx >= 0 && httpIdx >= 0 -> minOf(httpsIdx, httpIdx)
            httpsIdx >= 0 -> httpsIdx
            httpIdx >= 0 -> httpIdx
            else -> -1
        }
        if (startIdx > 0) {
            raw = raw.substring(startIdx).trim().trim('"', '\'', ' ')
        } else if (startIdx == -1 && raw.isNotBlank()) {
            if (raw.contains("supabase.co")) {
                val cleanDomain = raw.substringAfter("=").trim().trimStart('/', ':', ' ')
                raw = "https://$cleanDomain"
            }
        }

        // 3. 兜底默认有效 URL
        if (raw.isBlank() || (!raw.startsWith("http://") && !raw.startsWith("https://"))) {
            raw = DEFAULT_SUPABASE_URL
        }

        // 4. 标准化 Supabase REST v1 路径格式
        val formatted = if (raw.contains("dashboard/project/")) {
            val proj = raw.substringAfter("dashboard/project/").trim('/')
            "https://$proj.supabase.co/rest/v1/"
        } else if (raw.endsWith("/rest/v1") || raw.endsWith("/rest/v1/")) {
            if (raw.endsWith("/")) raw else "$raw/"
        } else if (raw.endsWith(".supabase.co") || raw.endsWith(".supabase.co/")) {
            raw.trimEnd('/') + "/rest/v1/"
        } else {
            if (raw.endsWith("/")) raw else "$raw/"
        }

        return formatted
    }

    private fun getApiKey(): String {
        var key = BuildConfig.SUPABASE_KEY.trim().trim('"', '\'', ' ')
        if (key.startsWith("SUPABASE_KEY", ignoreCase = true)) {
            key = key.substringAfter("=").trim().trim('"', '\'', ' ')
        }
        if (key.startsWith("Bearer ", ignoreCase = true)) {
            key = key.substring(7).trim().trim('"', '\'', ' ')
        }
        if (key.isBlank() || key == "your-supabase-anon-public-key") {
            key = DEFAULT_SUPABASE_KEY
        }
        return key
    }

    private fun isConfigured(): Boolean {
        val url = getBaseUrl()
        val key = getApiKey()
        val isValidHttpUrl = url.toHttpUrlOrNull() != null
        return isValidHttpUrl && key.isNotBlank() && key != "your-supabase-anon-public-key"
    }

    /**
     * 1. 尝试从 Supabase 缓存中拉取已算过的对应关系 (按岗位名称)
     */
    suspend fun getCachedRelationsForOccupation(occupationName: String): List<AiMatchedItem> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()

        try {
            val encodedName = URLEncoder.encode(occupationName, "UTF-8")
            val urlStr = "${getBaseUrl()}occupation_certificate_cross_ref?occupation_name=eq.$encodedName&direction=eq.OCC_TO_CERT&select=*"
            val httpUrl = urlStr.toHttpUrlOrNull() ?: run {
                Log.w(TAG, "Invalid Supabase URL: $urlStr")
                return@withContext emptyList()
            }

            val request = Request.Builder()
                .url(httpUrl)
                .addHeader("apikey", getApiKey())
                .addHeader("Authorization", "Bearer ${getApiKey()}")
                .addHeader("Content-Type", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Supabase query error: ${response.code}")
                    return@withContext emptyList()
                }
                val respString = response.body?.string() ?: return@withContext emptyList()
                val jsonArray = JSONArray(respString)
                val list = mutableListOf<AiMatchedItem>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        AiMatchedItem(
                            title = obj.optString("certificate_name", ""),
                            requirementType = obj.optString("requirement_type", "PREFERRED"),
                            score = obj.optInt("match_score", 90),
                            policyAnalysis = obj.optString("benefit_description", "")
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch from Supabase: ${e.message}")
            emptyList()
        }
    }

    /**
     * 2. 尝试从 Supabase 缓存中拉取已算过的对应关系 (按证书名称反向推导)
     */
    suspend fun getCachedRelationsForCertificate(certificateName: String): List<AiMatchedItem> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()

        try {
            val encodedName = URLEncoder.encode(certificateName, "UTF-8")
            val urlStr = "${getBaseUrl()}occupation_certificate_cross_ref?certificate_name=eq.$encodedName&direction=eq.CERT_TO_OCC&select=*"
            val httpUrl = urlStr.toHttpUrlOrNull() ?: run {
                Log.w(TAG, "Invalid Supabase URL: $urlStr")
                return@withContext emptyList()
            }

            val request = Request.Builder()
                .url(httpUrl)
                .addHeader("apikey", getApiKey())
                .addHeader("Authorization", "Bearer ${getApiKey()}")
                .addHeader("Content-Type", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val respString = response.body?.string() ?: return@withContext emptyList()
                val jsonArray = JSONArray(respString)
                val list = mutableListOf<AiMatchedItem>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        AiMatchedItem(
                            title = obj.optString("occupation_name", ""),
                            requirementType = obj.optString("requirement_type", "PREFERRED"),
                            score = obj.optInt("match_score", 90),
                            policyAnalysis = obj.optString("benefit_description", "")
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch from Supabase: ${e.message}")
            emptyList()
        }
    }

    /**
     * 3. 将 AI 计算出的匹配结果异步沉淀回 Supabase 数据库
     */
    suspend fun saveAiResultsToSupabase(
        targetName: String,
        results: List<AiMatchedItem>,
        isOccupationToCert: Boolean
    ) = withContext(Dispatchers.IO) {
        if (!isConfigured() || results.isEmpty()) return@withContext

        try {
            val jsonArray = JSONArray()
            results.forEach { item ->
                val obj = JSONObject().apply {
                    if (isOccupationToCert) {
                        put("occupation_name", targetName)
                        put("certificate_name", item.title)
                        put("direction", "OCC_TO_CERT")
                    } else {
                        put("occupation_name", item.title)
                        put("certificate_name", targetName)
                        put("direction", "CERT_TO_OCC")
                    }
                    put("requirement_type", item.requirementType)
                    put("match_score", item.score)
                    put("benefit_description", item.policyAnalysis)
                }
                jsonArray.put(obj)
            }

            val urlStr = "${getBaseUrl()}occupation_certificate_cross_ref"
            val httpUrl = urlStr.toHttpUrlOrNull() ?: return@withContext
            val body = jsonArray.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(httpUrl)
                .addHeader("apikey", getApiKey())
                .addHeader("Authorization", "Bearer ${getApiKey()}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "🎉 成功将 AI 推导图谱沉淀回 Supabase 云端!")
                } else {
                    Log.w(TAG, "Supabase 写入失败: ${response.code} ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Save to Supabase error: ${e.message}")
        }
    }

    /**
     * 4. 在后端 Supabase 数据库中检索职业 (后端直连)
     */
    suspend fun searchOccupations(query: String): List<Occupation> = withContext(Dispatchers.IO) {
        if (!isConfigured() || query.isBlank()) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val urlStr = "${getBaseUrl()}occupations?or=(name.ilike.*$encoded*,description.ilike.*$encoded*)&select=*"
            val httpUrl = urlStr.toHttpUrlOrNull() ?: run {
                Log.w(TAG, "Invalid Supabase URL for searchOccupations: $urlStr")
                return@withContext emptyList()
            }
            val request = Request.Builder()
                .url(httpUrl)
                .addHeader("apikey", getApiKey())
                .addHeader("Authorization", "Bearer ${getApiKey()}")
                .addHeader("Content-Type", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Supabase searchOccupations error: ${response.code}")
                    return@withContext emptyList()
                }
                val body = response.body?.string() ?: return@withContext emptyList()
                val jsonArray = JSONArray(body)
                val list = mutableListOf<Occupation>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        Occupation(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            category = obj.optString("category", "准入类"),
                            salaryExpectation = obj.optString("salary_expectation", "面议"),
                            description = obj.optString("description", "")
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase searchOccupations exception: ${e.message}")
            emptyList()
        }
    }

    /**
     * 5. 在后端 Supabase 数据库中检索证书 (后端直连)
     */
    suspend fun searchCertificates(query: String): List<Certificate> = withContext(Dispatchers.IO) {
        if (!isConfigured() || query.isBlank()) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val urlStr = "${getBaseUrl()}certificates?or=(name.ilike.*$encoded*,description.ilike.*$encoded*)&select=*"
            val httpUrl = urlStr.toHttpUrlOrNull() ?: run {
                Log.w(TAG, "Invalid Supabase URL for searchCertificates: $urlStr")
                return@withContext emptyList()
            }
            val request = Request.Builder()
                .url(httpUrl)
                .addHeader("apikey", getApiKey())
                .addHeader("Authorization", "Bearer ${getApiKey()}")
                .addHeader("Content-Type", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val jsonArray = JSONArray(body)
                val list = mutableListOf<Certificate>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        Certificate(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            authority = obj.optString("authority", "国家部委"),
                            difficulty = obj.optString("classes", "准入类") + " ★★★★★",
                            examFrequency = obj.optString("exam_frequency", "每年一次"),
                            description = obj.optString("description", "")
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase searchCertificates exception: ${e.message}")
            emptyList()
        }
    }

    /**
     * 6. 从后端 Supabase 数据库拉取该职业关联的法定证书
     */
    suspend fun getCertificatesForOccupation(occupation: Occupation): List<CertificateWithRelationInfo> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        try {
            // 提取前缀，例如 OCC_002_01 对应 CERT_002
            val certId = if (occupation.id.startsWith("OCC_") && occupation.id.contains("_")) {
                val parts = occupation.id.split("_")
                if (parts.size >= 2) "CERT_${parts[1]}" else null
            } else null

            val list = mutableListOf<CertificateWithRelationInfo>()
            if (certId != null) {
                val urlStr = "${getBaseUrl()}certificates?id=eq.$certId&select=*"
                val httpUrl = urlStr.toHttpUrlOrNull() ?: return@withContext emptyList()
                val request = Request.Builder()
                    .url(httpUrl)
                    .addHeader("apikey", getApiKey())
                    .addHeader("Authorization", "Bearer ${getApiKey()}")
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val arr = JSONArray(body)
                        if (arr.length() > 0) {
                            val obj = arr.getJSONObject(0)
                            val cert = Certificate(
                                id = obj.getString("id"),
                                name = obj.getString("name"),
                                authority = obj.optString("authority", "国家部委"),
                                difficulty = obj.optString("classes", "准入类") + " ★★★★★",
                                examFrequency = obj.optString("exam_frequency", "每年一次"),
                                description = obj.optString("description", "")
                            )
                            list.add(
                                CertificateWithRelationInfo(
                                    certificate = cert,
                                    requirementType = "MANDATORY",
                                    benefitDescription = "【法定红线】${cert.authority}强制持证准入规定。从事${occupation.name}独立执业必须通过该国家统考。"
                                )
                            )
                        }
                    }
                }
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "getCertificatesForOccupation error: ${e.message}")
            emptyList()
        }
    }
}
