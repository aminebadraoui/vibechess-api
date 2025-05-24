package com.vibechess.api.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.coroutines.runBlocking

@SpringBootTest
class ChessRulesValidationTest {

    @Autowired
    private lateinit var chessCoachingService: ChessCoachingService

    @Test
    fun `test bishop move advice does not claim impossible attacks`() {
        // This reproduces the exact scenario where AI incorrectly claimed Bb6 attacks rook on b1
        val mockEngineResponse = ChessApiResponse(
            text = "Move c7 → b6 (Bb6): [-5.95]. Black is winning. Depth 8.",
            eval = -5.95,
            move = "c7b6",
            san = "Bb6",
            piece = "b",  // Bishop
            from = "c7",
            to = "b6",
            depth = 8,
            winChance = 10.0,
            turn = "b"
        )
        
        val userInfo = UserInfo(color = "black", elo = 334)
        val fenAnalysis = FenAnalysis(
            fen = "1n1r2k1/p1bb1ppp/5q2/1B6/3N1P2/2P5/P1P3PP/1RB1K2R b K - 0 17",
            isValid = true,
            moveNumber = 17,
            activeColor = "b"
        )
        
        val advice = runBlocking {
            chessCoachingService.generateStructuredCoachingAdvice(userInfo, fenAnalysis, mockEngineResponse)
        }
        
        println("Generated advice for bishop move Bb6:")
        println("- Suggested Move: ${advice.suggestedMove}")
        println("- Explanation: ${advice.explanation}")
        println("- Reasoning: ${advice.reasoning}")
        
        val fullText = "${advice.explanation} ${advice.reasoning}".lowercase()
        
        // Should NOT claim the bishop attacks the rook (impossible geometrically)
        assertFalse(
            fullText.contains("attack") && fullText.contains("rook") && fullText.contains("b1"),
            "Should not claim bishop on b6 attacks rook on b1 (same file, not diagonal). Got: $fullText"
        )
        
        // Should NOT use phrases like "threatens the rook" 
        assertFalse(
            fullText.contains("threaten") && fullText.contains("rook"),
            "Should not claim bishop threatens rook when geometrically impossible. Got: $fullText"
        )
        
        // Should focus on valid positional concepts instead
        assertTrue(
            fullText.contains("improve") || fullText.contains("diagonal") || fullText.contains("position") || fullText.contains("active"),
            "Should focus on valid positional benefits like improving position or controlling diagonals. Got: $fullText"
        )
    }

    @Test
    fun `test rook move advice validation`() {
        // Test a more realistic rook move in the middlegame
        val mockEngineResponse = ChessApiResponse(
            text = "Move a1 → d1 (Rd1): [0.34]. The game is balanced. Depth 8.",
            eval = 0.34,
            move = "a1d1",
            san = "Rd1",
            piece = "r",  // Rook
            from = "a1",
            to = "d1",
            depth = 8,
            winChance = 55.0,
            turn = "w"
        )
        
        val userInfo = UserInfo(color = "white", elo = 334)
        val fenAnalysis = FenAnalysis(
            fen = "r3kbnr/pppppppp/2n5/8/8/2N5/PPPPPPPP/R3KBNR w KQkq - 0 5",
            isValid = true,
            moveNumber = 5,
            activeColor = "w"
        )
        
        val advice = runBlocking {
            chessCoachingService.generateStructuredCoachingAdvice(userInfo, fenAnalysis, mockEngineResponse)
        }
        
        println("Generated advice for rook move Rd1:")
        println("- Suggested Move: ${advice.suggestedMove}")
        println("- Explanation: ${advice.explanation}")
        println("- Reasoning: ${advice.reasoning}")
        
        val fullText = "${advice.explanation} ${advice.reasoning}".lowercase()
        
        // Should correctly mention it's a rook (or at least suggest the right move)
        assertTrue(
            fullText.contains("rook") || advice.suggestedMove.contains("Rd1") || advice.suggestedMove.contains("d1"),
            "Should correctly identify the piece as a rook or suggest the right move. Got: $fullText, Move: ${advice.suggestedMove}"
        )
        
        // Should NOT claim diagonal attacks (rooks can't move diagonally)
        assertFalse(
            fullText.contains("diagonal") && fullText.contains("attack"),
            "Should not claim rook makes diagonal attacks. Got: $fullText"
        )
    }

    @Test
    fun `test knight move advice validation`() {
        // Test that knight moves are described with L-shape movement
        val mockEngineResponse = ChessApiResponse(
            text = "Move g1 → f3 (Nf3): [0.25]. The game is balanced. Depth 8.",
            eval = 0.25,
            move = "g1f3",
            san = "Nf3",
            piece = "n",  // Knight
            from = "g1",
            to = "f3",
            depth = 8,
            winChance = 52.0,
            turn = "w"
        )
        
        val userInfo = UserInfo(color = "white", elo = 334)
        val fenAnalysis = FenAnalysis(
            fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1",
            isValid = true,
            moveNumber = 2,
            activeColor = "w"
        )
        
        val advice = runBlocking {
            chessCoachingService.generateStructuredCoachingAdvice(userInfo, fenAnalysis, mockEngineResponse)
        }
        
        val fullText = "${advice.explanation} ${advice.reasoning}".lowercase()
        
        // Should correctly identify as knight
        assertTrue(
            fullText.contains("knight"),
            "Should correctly identify the piece as a knight. Got: $fullText"
        )
        
        // Should NOT claim impossible straight-line attacks  
        assertFalse(
            fullText.contains("straight") && fullText.contains("line"),
            "Should not describe knight movement as straight line. Got: $fullText"
        )
    }
} 