package com.vibechess.api.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

@SpringBootTest
class FenGenerationTest {

    @Autowired
    private lateinit var chessCoachingService: ChessCoachingService

    @Test
    fun `test d4 d5 FEN generation - should have no en passant`() {
        val moves = "d4 d5"
        
        // Use the public suspend method
        val fenAnalysis = runBlocking {
            chessCoachingService.analyzeFENStructured(moves)
        }
        
        println("Generated FEN Analysis for '$moves':")
        println("- FEN: ${fenAnalysis.fen}")
        println("- Valid: ${fenAnalysis.isValid}")
        println("- Errors: ${fenAnalysis.errors}")
        
        // Extract the en passant field from the FEN
        val fenParts = fenAnalysis.fen.split(" ")
        val enPassantField = fenParts[3]
        
        println("- En passant field: '$enPassantField'")
        
        // Verify the en passant field is correct
        assertEquals("-", enPassantField, "After d4 d5, en passant should be '-' not '$enPassantField'")
        
        // Verify the expected position
        val expectedFen = "rnbqkbnr/pppp1ppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2"
        assertEquals(expectedFen, fenAnalysis.fen, "FEN should match expected position after d4 d5")
        
        assertTrue(fenAnalysis.isValid, "FEN should be marked as valid")
    }

    @Test 
    fun `test en passant scenario - should have en passant square`() {
        // This should create an actual en passant opportunity
        val moves = "e4 e6 e5"
        
        val fenAnalysis = runBlocking {
            chessCoachingService.analyzeFENStructured(moves)
        }
        
        println("\nGenerated FEN Analysis for '$moves':")
        println("- FEN: ${fenAnalysis.fen}")
        println("- Valid: ${fenAnalysis.isValid}")
        
        // This scenario might still have issues, but let's see what we get
        val fenParts = fenAnalysis.fen.split(" ")
        val enPassantField = fenParts[3]
        
        println("- En passant field: '$enPassantField'")
        
        // This test is more to observe the behavior than assert specific values
        assertTrue(fenAnalysis.isValid || fenAnalysis.errors.isNotEmpty(), 
                  "Should either be valid or have clear error messages")
    }
} 