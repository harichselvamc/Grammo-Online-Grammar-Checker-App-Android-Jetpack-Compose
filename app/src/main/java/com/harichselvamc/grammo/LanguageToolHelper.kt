package com.harichselvamc.grammo

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * LanguageTool helper that calls the public HTTP API:
 *  https://api.languagetool.org/v2/check
 *
 * NOTE: Requires Internet. For full offline, you would need to run
 * your own LT server somewhere and point the URL to it.
 */
object LanguageToolHelper {

    private val client = OkHttpClient()

    data class GrammarMatch(
        val offset: Int,
        val length: Int,
        val message: String,
        val ruleId: String,
        val replacements: List<String>
    )

    data class GrammarResult(
        val correctedText: String,
        val matches: List<GrammarMatch>,
        val totalIssues: Int,
        val autoCorrected: Int,
        val needsReview: Int
    )

    /**
     * Synchronous call – run this on a background dispatcher (IO / Default).
     * autoFix = apply first suggestion for each match.
     */
    fun checkAndAutoCorrect(
        text: String,
        languageCode: String = "en-US",
        autoFix: Boolean = true
    ): GrammarResult {

        if (text.isBlank()) {
            return GrammarResult(
                correctedText = text,
                matches = emptyList(),
                totalIssues = 0,
                autoCorrected = 0,
                needsReview = 0
            )
        }

        // Build POST body for LT API
        val body = FormBody.Builder()
            .add("language", languageCode)
            .add("text", text)
            .build()

        val request = Request.Builder()
            .url("https://api.languagetool.org/v2/check")
            .post(body)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            // If API fails, return original text
            return GrammarResult(
                correctedText = text,
                matches = emptyList(),
                totalIssues = 0,
                autoCorrected = 0,
                needsReview = 0
            )
        }

        val raw = response.body?.string() ?: return GrammarResult(
            correctedText = text,
            matches = emptyList(),
            totalIssues = 0,
            autoCorrected = 0,
            needsReview = 0
        )

        val json = JSONObject(raw)
        val matchesJson = json.optJSONArray("matches") ?: JSONArray()

        val matches = mutableListOf<GrammarMatch>()

        for (i in 0 until matchesJson.length()) {
            val m = matchesJson.getJSONObject(i)
            val offset = m.optInt("offset")
            val length = m.optInt("length")
            val message = m.optString("message")
            val ruleId = m.optJSONObject("rule")?.optString("id") ?: ""

            val repsJson = m.optJSONArray("replacements") ?: JSONArray()
            val replacements = mutableListOf<String>()
            for (j in 0 until repsJson.length()) {
                val r = repsJson.getJSONObject(j).optString("value")
                if (r.isNotBlank()) replacements.add(r)
            }

            matches.add(
                GrammarMatch(
                    offset = offset,
                    length = length,
                    message = message,
                    ruleId = ruleId,
                    replacements = replacements
                )
            )
        }

        val sorted = matches.sortedBy { it.offset }
        val sb = StringBuilder(text)
        var offsetDelta = 0
        var fixedCount = 0

        if (autoFix) {
            for (m in sorted) {
                if (m.replacements.isNotEmpty()) {
                    val replacement = m.replacements.first()
                    val start = m.offset + offsetDelta
                    val end = start + m.length

                    if (start in 0..sb.length && end in 0..sb.length && start <= end) {
                        sb.replace(start, end, replacement)
                        offsetDelta += replacement.length - (end - start)
                        fixedCount++
                    }
                }
            }
        }

        val total = matches.size
        return GrammarResult(
            correctedText = if (autoFix) sb.toString() else text,
            matches = matches,
            totalIssues = total,
            autoCorrected = fixedCount,
            needsReview = total - fixedCount
        )
    }
}
