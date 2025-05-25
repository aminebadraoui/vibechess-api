package com.vibechess.api.service

import com.vibechess.api.ai.agent.orchestrator.ChessCoachingOrchestratorAgent
import com.vibechess.api.model.domain.CoachingResult
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

/**
 * Chess coaching service focused on business logic orchestration
 * Delegates AI orchestration to specialized orchestrator agent
 */
@Service
class ChessCoachingService(
    private val orchestratorAgent: ChessCoachingOrchestratorAgent
) {

    /**
     * Main analysis function - delegates to AI orchestrator for complete workflow
     * 
     * @param screenshot Chess game screenshot for user info extraction
     * @param moves Game moves in algebraic notation
     * @return CoachingResult with best move and detailed advice
     */
    suspend fun analyzePosition(screenshot: MultipartFile, moves: String): CoachingResult {
        println("🚀 ChessCoachingService: Starting position analysis...")
        
        // Calculate move number for coaching context
        val moveNumber = extractMoveNumber(moves)
        println("📊 Position Info: Move $moveNumber")
        
        // Delegate complete AI workflow to orchestrator agent
        val result = orchestratorAgent.orchestrateCoachingWithAutoDepth(
            screenshot = screenshot,
            moves = moves,
            moveNumber = moveNumber
        )
        
        println("✅ ChessCoachingService: Analysis completed successfully")
        return result
    }

    /**
     * Advanced analysis with custom depth (for future extensibility)
     * 
     * @param screenshot Chess game screenshot
     * @param moves Game moves in algebraic notation
     * @param customDepth Custom analysis depth
     * @return CoachingResult
     */
    suspend fun analyzePositionWithCustomDepth(
        screenshot: MultipartFile, 
        moves: String, 
        customDepth: Int
    ): CoachingResult {
        println("🚀 ChessCoachingService: Starting custom depth analysis (depth: $customDepth)...")
        
        val moveNumber = extractMoveNumber(moves)
        
        return orchestratorAgent.orchestrateCoaching(
            screenshot = screenshot,
            moves = moves,
            analysisDepth = customDepth,
            moveNumber = moveNumber
        )
    }

    /**
     * Extract move number from moves string for coaching context
     * Used to determine game phase (opening/middlegame/endgame)
     */
    private fun extractMoveNumber(moves: String): Int {
        if (moves.isBlank()) return 1
        
        // Count the number of moves (split by spaces, filter out empty)
        val moveList = moves.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        
        // In chess, each "move" consists of both white and black moves
        // So the move number is roughly half the total moves played + 1
        return (moveList.size / 2) + 1
    }
} 