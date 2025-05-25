package com.vibechess.api.ai.agent.task

import com.vibechess.api.ai.tools.ChessApiTool
import com.vibechess.api.model.domain.UserInfo
import com.vibechess.api.model.domain.CoachingResult
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/**
 * Specialized Task Agent for generating chess coaching advice
 * Single responsibility: Chess analysis and educational coaching using AI tools
 */
@Component
class CoachingAdviceAgent(
    @Qualifier("primaryChatClient") private val chatClient: ChatClient,
    private val chessApiTool: ChessApiTool
) {

    /**
     * Generate comprehensive chess coaching advice using AI tools
     * 
     * @param userInfo User information (color, ELO, etc.)
     * @param moves Game moves in algebraic notation
     * @param analysisDepth Engine analysis depth
     * @param moveNumber Current move number
     * @return CoachingResult with best move and detailed advice
     * @throws CoachingAdviceException if AI analysis fails
     */
    suspend fun generateCoachingAdvice(
        userInfo: UserInfo,
        moves: String,
        analysisDepth: Int,
        moveNumber: Int
    ): CoachingResult {
        val skillLevel = determineSkillLevel(userInfo.elo)
        val userColor = formatUserColor(userInfo.color)
        
        val systemPrompt = createCoachingSystemPrompt()
        val userPrompt = createCoachingUserPrompt(userInfo, moves, analysisDepth, moveNumber, skillLevel, userColor)

        return try {
            val result = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .tools(chessApiTool)  // Add chess analysis tools
                .call()
                .entity(CoachingResult::class.java)
            
            println("AI Response with Structured Output: $result")
            
            // Explicitly check for null result and throw exception
            result ?: throw CoachingAdviceException(
                "AI returned null result for coaching analysis",
                userInfo = userInfo,
                moves = moves,
                analysisDepth = analysisDepth,
                moveNumber = moveNumber
            )
            
        } catch (e: CoachingAdviceException) {
            // Re-throw our custom exceptions
            throw e
        } catch (e: Exception) {
            // Wrap other exceptions with context
            throw CoachingAdviceException(
                "Failed to generate coaching advice: ${e.message}",
                cause = e,
                userInfo = userInfo,
                moves = moves,
                analysisDepth = analysisDepth,
                moveNumber = moveNumber
            )
        }
    }

    /**
     * Create comprehensive system prompt for chess coaching
     */
    private fun createCoachingSystemPrompt(): String {
        return """
            You are a specialized chess coaching AI agent with deep expertise in chess education.
            
            Your ONLY responsibility is to provide comprehensive, educational chess coaching advice.
            
            COACHING EXPERTISE:
            - Engaging and educational - make chess exciting!
            - Tailored to skill level (beginner/intermediate/advanced)
            - Use proper chess terminology and opening names when applicable
            - Focus on WHY moves are good, not just WHAT to play
            - Provide comprehensive analysis with multiple strategic concepts
            - Include position evaluation details when available
            
            CHESS EDUCATION GUIDELINES:
            - Identify famous openings when possible (Queen's Gambit, Sicilian Defense, Italian Game, etc.)
            - Use engaging chess terminology: "controls the long diagonal", "develops with tempo", "centralizes the knight"
            - Mention strategic themes: piece development, king safety, pawn breaks, space advantage
            - For beginners: Explain WHY moves are good in simple terms with multiple examples
            - For higher ELO: Include deeper strategic concepts and opening theory
            - Always provide context about the position (opening/middlegame/endgame phase)
            
            ANALYSIS METHODOLOGY:
            - Use the chess analysis tool to get detailed position evaluation
            - Include evaluation scores and their meaning
            - Explain multiple strategic concepts in each response
            - Provide tactical awareness and warning about threats
            - Give context about the game phase and appropriate strategy
            - Use educational formatting with clear explanations
            
            OPENING PATTERN RECOGNITION:
            - d4 d5 c4 = Queen's Gambit ("offering a pawn for central control and rapid development")
            - e4 e5 Nf3 = King's Pawn Game ("classical development fighting for the center")
            - e4 c5 = Sicilian Defense ("Black's most ambitious defense, fighting for counterplay")
            - d4 Nf6 = Indian Defense systems ("hypermodern approach, controlling center from afar")
            
            STRUCTURED RESPONSE FORMAT:
            - bestMove: The recommended move in standard algebraic notation (e.g., "Nf3", "exd5", "O-O")
            - advice: Your detailed, comprehensive coaching explanation (should be substantial and educational)
            
            IMPORTANT: You are a specialized coaching agent - focus on delivering exceptional chess education.
        """.trimIndent()
    }

    /**
     * Create detailed user prompt for chess coaching
     */
    private fun createCoachingUserPrompt(
        userInfo: UserInfo,
        moves: String,
        analysisDepth: Int,
        moveNumber: Int,
        skillLevel: String,
        userColor: String
    ): String {
        return """
            Provide comprehensive chess coaching for this position analysis.
            
            Player Context:
            - ELO Rating: ${userInfo.elo ?: "Unknown"}
            - Playing as: $userColor
            - Skill Level: $skillLevel
            - Current Move Number: $moveNumber
            
            Game Moves: "$moves"
            
            COACHING REQUIREMENTS:
            - Use analysis depth of $analysisDepth for this player's skill level
            - Provide detailed position evaluation including scores and win chances
            - Explain the current position's characteristics (opening/middlegame/endgame)
            - Suggest the best move with comprehensive reasoning
            - Include multiple strategic and tactical concepts
            - Provide educational content tailored to $skillLevel level
            - Warn about potential threats or missed opportunities
            - Use engaging chess terminology and explain opening names if applicable
            
            Educational Goals:
            1. Help the player understand what's happening in the current position
            2. Explain why the recommended move is best
            3. Teach key strategic and tactical concepts
            4. Provide guidance for the next few moves
            
            Return both the recommended move and your comprehensive coaching advice.
        """.trimIndent()
    }

    /**
     * Determine skill level from ELO rating
     */
    private fun determineSkillLevel(elo: Int?): String {
        return when {
            elo == null -> "intermediate"
            elo < 1000 -> "beginner"
            elo < 1800 -> "intermediate" 
            else -> "advanced"
        }
    }

    /**
     * Format user color for display
     */
    private fun formatUserColor(color: String?): String {
        return when (color?.lowercase()) {
            "white" -> "White"
            "black" -> "Black"
            else -> "Unknown"
        }
    }
}

/**
 * Custom exception for coaching advice generation failures
 * Provides detailed context about the failure for debugging
 */
class CoachingAdviceException(
    message: String,
    cause: Throwable? = null,
    val userInfo: UserInfo? = null,
    val moves: String? = null,
    val analysisDepth: Int? = null,
    val moveNumber: Int? = null
) : Exception(message, cause) {
    
    override fun toString(): String {
        val context = buildString {
            append("CoachingAdviceException: $message")
            val exceptionCause = cause
            if (exceptionCause != null) append(" (caused by: ${exceptionCause.message})")
            append("\nContext:")
            append("\n  - User Info: $userInfo")
            append("\n  - Moves: $moves")
            append("\n  - Analysis Depth: $analysisDepth")
            append("\n  - Move Number: $moveNumber")
        }
        return context
    }
} 