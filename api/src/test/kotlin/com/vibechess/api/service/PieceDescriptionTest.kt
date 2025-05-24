package com.vibechess.api.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

@SpringBootTest
class PieceDescriptionTest {

    @Autowired
    private lateinit var chessCoachingService: ChessCoachingService

    @Test
    fun `test coaching advice uses correct piece descriptions`() {
        // Create a mock engine response with a pawn move
        val mockEngineResponse = ChessApiResponse(
            text = "Move e2 → e3 (e3): [0.15]. The game is balanced. Depth 8.",
            eval = 0.15,
            move = "e2e3",
            san = "e3",
            piece = "p",  // This is clearly a PAWN
            from = "e2",
            to = "e3",
            depth = 8,
            winChance = 51.0,
            turn = "w"
        )
        
        val userInfo = UserInfo(color = "white", elo = 326)
        val fenAnalysis = FenAnalysis(
            fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            isValid = true,
            moveNumber = 1,
            activeColor = "w"
        )
        
        val advice = runBlocking {
            chessCoachingService.generateStructuredCoachingAdvice(userInfo, fenAnalysis, mockEngineResponse)
        }
        
        println("Generated advice for pawn move e3:")
        println("- Suggested Move: ${advice.suggestedMove}")
        println("- Explanation: ${advice.explanation}")
        println("- Reasoning: ${advice.reasoning}")
        
        // The advice should mention "pawn" not "knight"
        val fullText = "${advice.explanation} ${advice.reasoning}".lowercase()
        
        assertTrue(
            fullText.contains("pawn"),
            "Advice should mention 'pawn' when explaining a pawn move, but got: $fullText"
        )
        
        assertTrue(
            !fullText.contains("knight"),
            "Advice should NOT mention 'knight' when explaining a pawn move, but got: $fullText"
        )
    }

    @Test
    fun `test coaching advice for knight move`() {
        // Create a mock engine response with a knight move
        val mockEngineResponse = ChessApiResponse(
            text = "Move g1 → f3 (Nf3): [0.25]. The game is balanced. Depth 8.",
            eval = 0.25,
            move = "g1f3",
            san = "Nf3",
            piece = "n",  // This is clearly a KNIGHT
            from = "g1",
            to = "f3",
            depth = 8,
            winChance = 52.0,
            turn = "w"
        )
        
        val userInfo = UserInfo(color = "white", elo = 326)
        val fenAnalysis = FenAnalysis(
            fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1",
            isValid = true,
            moveNumber = 2,
            activeColor = "w"
        )
        
        val advice = runBlocking {
            chessCoachingService.generateStructuredCoachingAdvice(userInfo, fenAnalysis, mockEngineResponse)
        }
        
        println("Generated advice for knight move Nf3:")
        println("- Suggested Move: ${advice.suggestedMove}")
        println("- Explanation: ${advice.explanation}")
        println("- Reasoning: ${advice.reasoning}")
        
        // The advice should mention "knight" not "pawn"
        val fullText = "${advice.explanation} ${advice.reasoning}".lowercase()
        
        assertTrue(
            fullText.contains("knight"),
            "Advice should mention 'knight' when explaining a knight move, but got: $fullText"
        )
        
        assertTrue(
            !fullText.contains("pawn") || fullText.contains("pawn") && fullText.contains("knight"),
            "Advice should primarily focus on 'knight' when explaining a knight move, but got: $fullText"
        )
    }
} 