package com.vibechess.api.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
class MovesBasedAnalysisTest {

    @Autowired
    private lateinit var chessApiTool: ChessApiTool

    @Test
    @Disabled("Enable to test actual API with moves - requires internet connection")
    fun `test moves-based analysis with problematic sequence`() {
        // This is the exact sequence that was causing "wrong FEN" errors
        val moves = "e4 e6 d3 d5 exd5 exd5 d4 b6 c4 Bb4+ Bd2"
        
        val result = chessApiTool.getAnalysisResponseFromMoves(moves, depth = 8)
        
        println("Chess API Response from MOVES:")
        println("- Text: ${result?.text}")
        println("- Eval: ${result?.eval}")
        println("- Best Move: ${result?.san ?: result?.move}")
        println("- FEN: ${result?.fen}")
        println("- Turn: ${result?.turn}")
        println("- Depth: ${result?.depth}")
        println("- Win Chance: ${result?.winChance}")
        
        assertNotNull(result, "Should get a response from chess-api.com using moves")
        assertNotNull(result.text, "Response should have text description")
        assertTrue(result.text.isNotBlank(), "Text should not be empty")
        
        // The key benefit: we should get a valid FEN back from the engine
        assertNotNull(result.fen, "Engine should return the correct FEN")
    }

    @Test
    @Disabled("Enable to test simple moves analysis")
    fun `test simple moves analysis`() {
        val moves = "e4 e5 Nf3 Nc6"
        
        val result = chessApiTool.getAnalysisResponseFromMoves(moves, depth = 10)
        
        println("Simple moves analysis:")
        println("- Moves: $moves")
        println("- Text: ${result?.text}")
        println("- Best Move: ${result?.san}")
        println("- FEN: ${result?.fen}")
        println("- Turn: ${result?.turn}")
        
        assertNotNull(result, "Should analyze simple opening moves")
        assertTrue(result.text.isNotBlank(), "Should have analysis text")
    }

    @Test
    fun `test move number extraction`() {
        val testCases = mapOf(
            "e4" to 1,
            "e4 e5" to 1,
            "e4 e5 Nf3" to 2,
            "e4 e5 Nf3 Nc6" to 2,
            "e4 e5 Nf3 Nc6 Bb5" to 3,
            "e4 e6 d3 d5 exd5 exd5 d4 b6 c4 Bb4+ Bd2" to 6
        )
        
        testCases.forEach { (moves, expectedMoveNumber) ->
            val extractedNumber = extractMoveNumberHelper(moves)
            println("Moves: '$moves' -> Move number: $extractedNumber (expected: $expectedMoveNumber)")
            assertTrue(
                extractedNumber == expectedMoveNumber,
                "Move number for '$moves' should be $expectedMoveNumber but got $extractedNumber"
            )
        }
    }

    private fun extractMoveNumberHelper(moves: String): Int {
        if (moves.isBlank()) return 1
        
        val moveList = moves.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return (moveList.size + 1) / 2
    }
} 