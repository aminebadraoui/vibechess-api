package com.vibechess.api.ai.agent.task

import com.vibechess.api.model.domain.UserInfo
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.content.Media
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ByteArrayResource
import org.springframework.stereotype.Component
import org.springframework.util.MimeTypeUtils
import org.springframework.web.multipart.MultipartFile

/**
 * Specialized Task Agent for extracting user information from chess screenshots
 * Single responsibility: Vision AI parsing of Chess.com interface elements
 */
@Component
class UserInfoExtractionAgent(
    @Qualifier("visionChatClient") private val visionClient: ChatClient
) {

    /**
     * Extract user information from chess screenshot using vision AI
     * 
     * @param screenshot Chess game screenshot
     * @return UserInfo with color, ELO, and confidence levels
     */
    suspend fun extractUserInfo(screenshot: MultipartFile): UserInfo {
        val systemPrompt = createVisionSystemPrompt()
        val userPrompt = createVisionUserPrompt()

        val mimeType = MimeTypeUtils.parseMimeType(screenshot.contentType ?: "image/png")
        val resource = ByteArrayResource(screenshot.bytes)
        val media = Media(mimeType, resource)

        return try {
            val result = visionClient.prompt()
                .system(systemPrompt)
                .user { userMessageBuilder ->
                    userMessageBuilder.text(userPrompt).media(media)
                }
                .call()
                .entity(UserInfo::class.java)
            
            result ?: UserInfo(color = "Uncertain", elo = null)
        } catch (e: Exception) {
            println("Error parsing user info: ${e.message}")
            UserInfo(color = "Uncertain", elo = null)
        }
    }

    /**
     * Create specialized system prompt for vision-based user info extraction
     */
    private fun createVisionSystemPrompt(): String {
        return """
            You are a specialized vision AI system for parsing Chess.com game interfaces.
            
            Your ONLY responsibility is to extract user information from screenshots with high accuracy.
            
            EXTRACTION RULES:
            - Determine user color from board orientation (user typically plays from bottom)
            - Extract ELO rating from the visible username/rating area
            - If ELO is unclear, blurry, or unreadable, set to null
            - Assess confidence levels based on image clarity and visibility
            - Only extract what is clearly visible - never guess or infer
            - Focus on precision over assumptions
            
            CONFIDENCE ASSESSMENT:
            - High: Information is clearly visible and unambiguous
            - Medium: Information is visible but may have minor clarity issues
            - Low: Information is partially obscured or unclear
            
            IMPORTANT: You are a specialized extraction agent - focus only on accurate data extraction.
        """.trimIndent()
    }

    /**
     * Create focused user prompt for vision analysis
     */
    private fun createVisionUserPrompt(): String {
        return """
            Analyze this Chess.com game screenshot and extract user information with precision.
            
            Extract:
            1. User's playing color (White/Black) - determined by board orientation
            2. User's ELO rating - from visible interface elements
            3. Confidence levels for both extracted pieces of information
            
            Return structured data with your confidence assessment for each field.
        """.trimIndent()
    }
} 