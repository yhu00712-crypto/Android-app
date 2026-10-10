package com.example.data.network

object ApiConfigHelper {
    fun getDeepSeekKey(): String {
        return try {
            val field = com.example.BuildConfig::class.java.getField("DEEPSEEK_API_KEY")
            val v = field.get(null) as? String
            if (!v.isNullOrBlank()) v else getDeepSeekKeyFallback()
        } catch (e: Throwable) {
            getDeepSeekKeyFallback()
        }
    }

    private fun getDeepSeekKeyFallback(): String {
        return try {
            val field = com.example.BuildConfig::class.java.getField("deepseek")
            (field.get(null) as? String) ?: ""
        } catch (e: Throwable) {
            ""
        }
    }

    fun getGeminiKey(): String {
        return try {
            val field = com.example.BuildConfig::class.java.getField("GEMINI_API_KEY")
            (field.get(null) as? String) ?: ""
        } catch (e: Throwable) {
            ""
        }
    }
}
