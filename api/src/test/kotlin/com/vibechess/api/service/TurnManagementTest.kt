package com.vibechess.api.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.coroutines.runBlocking

@SpringBootTest
class TurnManagementTest {

    @Autowired
    private lateinit var chessCoachingService: ChessCoachingService

    @Test
    fun `test coaching when user is WHITE and it is BLACK to move`() {
        // This is the exact scenario from the user's issue:
        // User is WHITE, but it's BLACK's turn to move
        val userInfo = UserInfo(
            color = "white", 
            elo = 350, 
            colorConfidence = "high", 
            eloConfidence = "high"
        )
        
        val fenAnalysis = FenAnalysis(
            fen = "r1bk1bnr/1p3ppp/p1n5/1Bp5/2P5/1Q2P3/P3P1PP/RN2K1NR b - - 1 15",
            isValid = true,
            errors = emptyList(),
            moveNumber = 15,
            activeColor = "b", // BLACK to move
            castlingRights = "-"
        )
        
        // Mock engine analysis suggesting Black's best move
        val engineAnalysis = ChessApiResponse(
            text = "Move a6 → b5 (axb5): [2.89]. White is winning. Depth 8.",
            eval = 2.89,
            move = "a6b5",
            san = "axb5",
            depth = 8,
            winChance = 74.34773086886246,
            continuationArr = listOf("c4b5", "c6b4", "g1f3", "c8e6", "b3c3", "d8e8", "b1d2"),
            mate = null
        )
        
        // Generate coaching advice
        val advice = runBlocking {
            chessCoachingService.generateStructuredCoachingAdvice(userInfo, fenAnalysis, engineAnalysis)
        }
        
        println("=".repeat(60))
        println("TURN MANAGEMENT TEST - WHITE USER, BLACK TO MOVE")
        println("User: ${userInfo.color} (ELO ${userInfo.elo})")
        println("Active Color: ${fenAnalysis.activeColor} (Black to move)")
        println("Engine suggests: ${engineAnalysis.san} (Black's move)")
        println("=".repeat(60))
        println("Coaching Advice:")
        println("- Suggested Move: ${advice.suggestedMove}")
        println("- Explanation: ${advice.explanation}")
        println("=".repeat(60))
        
        // Verify the advice is appropriate for the user (WHITE) not the opponent (BLACK)
        val explanationText = advice.explanation.lowercase()
        
        // Should NOT contain advice for Black to move
        assertFalse(
            explanationText.contains("the best move for black"),
            "Should not give advice for Black when coaching White"
        )
        
        // Should contain advice about what opponent will do
        assertTrue(
            explanationText.contains("opponent") || 
            explanationText.contains("black will") ||
            explanationText.contains("they will") ||
            explanationText.contains("expect"),
            "Should mention what opponent will do"
        )
        
        // Should NOT suggest axb5 as if it's the user's move
        assertFalse(
            advice.suggestedMove.equals("axb5", ignoreCase = true),
            "Should not suggest opponent's move (axb5) as user's move"
        )
    }

    @Test
    fun `test coaching when user is WHITE and it is WHITE to move`() {
        // Test the normal case: user's turn
        val userInfo = UserInfo(
            color = "white", 
            elo = 350, 
            colorConfidence = "high", 
            eloConfidence = "high"
        )
        
        val fenAnalysis = FenAnalysis(
            fen = "rnbqkbnr/pppp1ppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2",
            isValid = true,
            errors = emptyList(),
            moveNumber = 2,
            activeColor = "w", // WHITE to move (user's turn)
            castlingRights = "KQkq"
        )
        
        // Mock engine analysis for White's move
        val engineAnalysis = ChessApiResponse(
            text = "Best move for White: Nf3",
            eval = 0.2,
            move = "g1f3",
            san = "Nf3",
            depth = 8,
            winChance = 52.0,
            continuationArr = listOf("g1f3", "b8c6"),
            mate = null
        )
        
        val advice = runBlocking {
            chessCoachingService.generateStructuredCoachingAdvice(userInfo, fenAnalysis, engineAnalysis)
        }
        
        println("\n" + "=".repeat(60))
        println("NORMAL CASE - WHITE USER, WHITE TO MOVE")
        println("User: ${userInfo.color} (ELO ${userInfo.elo})")
        println("Active Color: ${fenAnalysis.activeColor} (White to move)")
        println("Engine suggests: ${engineAnalysis.san} (White's move)")
        println("=".repeat(60))
        println("Coaching Advice:")
        println("- Suggested Move: ${advice.suggestedMove}")
        println("- Explanation: ${advice.explanation}")
        println("=".repeat(60))
        
        // Should give direct advice to the user
        assertTrue(
            advice.suggestedMove.isNotBlank(),
            "Should suggest a move for the user"
        )
        
        // Should not mention opponent in this context
        val explanationText = advice.explanation.lowercase()
        assertFalse(
            explanationText.contains("your opponent will"),
            "Should not focus on opponent when it's user's turn"
        )
    }
} 