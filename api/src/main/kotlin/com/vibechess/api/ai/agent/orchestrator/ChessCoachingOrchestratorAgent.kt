package com.vibechess.api.ai.agent.orchestrator

import com.vibechess.api.ai.agent.task.UserInfoExtractionAgent
import com.vibechess.api.ai.agent.task.CoachingAdviceAgent
import com.vibechess.api.ai.agent.task.CoachingAdviceException
import com.vibechess.api.model.domain.CoachingResult
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

/**
 * Orchestrator Agent that coordinates specialized task agents for chess coaching
 * Manages the workflow between user info extraction and coaching advice generation
 */
@Component
class ChessCoachingOrchestratorAgent(
    private val userInfoExtractionAgent: UserInfoExtractionAgent,
    private val coachingAdviceAgent: CoachingAdviceAgent
) {

    /**
     * Orchestrate the complete chess coaching workflow
     * 
     * @param screenshot Chess game screenshot for user info extraction
     * @param moves Game moves in algebraic notation
     * @param analysisDepth Engine analysis depth
     * @param moveNumber Current move number
     * @return CoachingResult with best move and comprehensive advice
     * @throws ChessCoachingException if any step in the workflow fails
     */
    suspend fun orchestrateCoaching(
        screenshot: MultipartFile,
        moves: String,
        analysisDepth: Int,
        moveNumber: Int
    ): CoachingResult {
        println("🎯 Chess Coaching Orchestrator: Starting workflow...")
        
        try {
            // Step 1: Extract user information using specialized vision agent
            println("📷 Step 1: Extracting user info from screenshot...")
            val userInfo = userInfoExtractionAgent.extractUserInfo(screenshot)
            println("✅ User Info extracted: $userInfo")
            
            // Step 2: Generate coaching advice using specialized coaching agent
            println("🏆 Step 2: Generating coaching advice with analysis depth $analysisDepth...")
            val coachingResult = coachingAdviceAgent.generateCoachingAdvice(
                userInfo = userInfo,
                moves = moves,
                analysisDepth = analysisDepth,
                moveNumber = moveNumber
            )
            println("✅ Coaching advice generated successfully")
            
            println("🎉 Chess Coaching Orchestrator: Workflow completed!")
            return coachingResult
            
        } catch (e: CoachingAdviceException) {
            // Re-throw coaching advice exceptions with orchestrator context
            throw ChessCoachingException(
                "Coaching advice generation failed in orchestrator workflow",
                cause = e,
                screenshot = screenshot.originalFilename,
                moves = moves,
                analysisDepth = analysisDepth,
                moveNumber = moveNumber
            )
        } catch (e: Exception) {
            // Wrap other exceptions with orchestrator context
            throw ChessCoachingException(
                "Chess coaching orchestration failed: ${e.message}",
                cause = e,
                screenshot = screenshot.originalFilename,
                moves = moves,
                analysisDepth = analysisDepth,
                moveNumber = moveNumber
            )
        }
    }

    /**
     * Orchestrate coaching with automatic depth determination
     * 
     * @param screenshot Chess game screenshot
     * @param moves Game moves
     * @param moveNumber Current move number
     * @return CoachingResult
     * @throws ChessCoachingException if any step fails
     */
    suspend fun orchestrateCoachingWithAutoDepth(
        screenshot: MultipartFile,
        moves: String,
        moveNumber: Int
    ): CoachingResult {
        try {
            // First extract user info to determine appropriate depth
            val userInfo = userInfoExtractionAgent.extractUserInfo(screenshot)
            val analysisDepth = determineAnalysisDepth(userInfo.elo)
            
            // Then generate coaching advice with determined depth
            return coachingAdviceAgent.generateCoachingAdvice(
                userInfo = userInfo,
                moves = moves,
                analysisDepth = analysisDepth,
                moveNumber = moveNumber
            )
        } catch (e: CoachingAdviceException) {
            // Re-throw coaching advice exceptions with auto-depth context
            throw ChessCoachingException(
                "Auto-depth coaching workflow failed during advice generation",
                cause = e,
                screenshot = screenshot.originalFilename,
                moves = moves,
                moveNumber = moveNumber
            )
        } catch (e: Exception) {
            // Wrap other exceptions with auto-depth context
            throw ChessCoachingException(
                "Auto-depth chess coaching orchestration failed: ${e.message}",
                cause = e,
                screenshot = screenshot.originalFilename,
                moves = moves,
                moveNumber = moveNumber
            )
        }
    }

    /**
     * Determine appropriate analysis depth based on player ELO
     * Higher rated players get deeper analysis for more sophisticated advice
     */
    private fun determineAnalysisDepth(elo: Int?): Int {
        return when {
            elo == null -> 12 // Default for uncertain ELO
            elo < 800 -> 8    // Beginners need simpler analysis
            elo < 1200 -> 10  // Intermediate beginners
            elo < 1600 -> 12  // Club level
            elo < 2000 -> 15  // Advanced
            else -> 18        // Expert level (max depth)
        }
    }
}

/**
 * Custom exception for chess coaching orchestration failures
 * Provides detailed context about the orchestration failure for debugging
 */
class ChessCoachingException(
    message: String,
    cause: Throwable? = null,
    val screenshot: String? = null,
    val moves: String? = null,
    val analysisDepth: Int? = null,
    val moveNumber: Int? = null
) : Exception(message, cause) {
    
    override fun toString(): String {
        val context = buildString {
            append("ChessCoachingException: $message")
            val exceptionCause = cause
            if (exceptionCause != null) append(" (caused by: ${exceptionCause.message})")
            append("\nOrchestration Context:")
            append("\n  - Screenshot: $screenshot")
            append("\n  - Moves: $moves")
            append("\n  - Analysis Depth: $analysisDepth")
            append("\n  - Move Number: $moveNumber")
            if (exceptionCause is CoachingAdviceException) {
                append("\n  - Underlying Coaching Error: ${exceptionCause.toString()}")
            }
        }
        return context
    }
} 