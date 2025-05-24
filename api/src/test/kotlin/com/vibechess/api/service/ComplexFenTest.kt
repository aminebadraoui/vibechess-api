package com.vibechess.api.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.coroutines.runBlocking

@SpringBootTest
class ComplexFenTest {

    @Autowired
    private lateinit var chessCoachingService: ChessCoachingService

    @Test
    fun `test complex move sequence FEN generation`() {
        // This is the exact sequence that was causing "wrong FEN" errors
        val moves = "d4 e6 e4 Nf6 Nc3 d5 e5 Bb4 a3"
        
        val fenAnalysis = runBlocking {
            chessCoachingService.analyzeFENStructured(moves)
        }
        
        println("Generated FEN Analysis for '$moves':")
        println("- FEN: ${fenAnalysis.fen}")
        println("- Valid: ${fenAnalysis.isValid}")
        println("- Errors: ${fenAnalysis.errors}")
        
        // Extract rank 5 to check pawn positions
        val fenParts = fenAnalysis.fen.split(" ")
        val boardPart = fenParts[0]
        val ranks = boardPart.split("/")
        val rank5 = ranks[3] // rank 5 (0-indexed: rank 8=0, rank 5=3)
        
        println("- Rank 5: '$rank5'")
        
        // After d4 e6 e4 Nf6 Nc3 d5 e5:
        // - Black pawn on d5 (lowercase 'p')  
        // - White pawn on e5 (uppercase 'P')
        // - Rank 5 should be: "3pP3"
        
        assertTrue(fenAnalysis.isValid, "FEN should be marked as valid")
        
        // Check that rank 5 has both pawns in correct positions
        assertTrue(
            rank5.contains('p') && rank5.contains('P'),
            "Rank 5 should contain both black pawn (p) and White pawn (P), but got: '$rank5'"
        )
        
        // The position should be something like "3pP3" for rank 5
        assertTrue(
            rank5.matches(Regex(".*p.*P.*")) || rank5.matches(Regex(".*P.*p.*")),
            "Rank 5 should have both pawns positioned correctly: '$rank5'"
        )
    }

    @Test
    fun `test French Defense advance variation`() {
        // Test a classic French Defense line
        val moves = "e4 e6 d4 d5 e5"
        
        val fenAnalysis = runBlocking {
            chessCoachingService.analyzeFENStructured(moves)
        }
        
        println("\nFrench Defense test for '$moves':")
        println("- FEN: ${fenAnalysis.fen}")
        println("- Valid: ${fenAnalysis.isValid}")
        
        // This should result in a valid position with pawn chain
        assertTrue(fenAnalysis.isValid, "French Defense position should be valid")
        
        // Check that we have the expected pawn structure
        val fenParts = fenAnalysis.fen.split(" ")
        val boardPart = fenParts[0]
        val ranks = boardPart.split("/")
        val rank5 = ranks[3] // rank 5
        val rank4 = ranks[4] // rank 4
        
        println("- Rank 5: '$rank5'")
        println("- Rank 4: '$rank4'")
        
        // Should have white pawn on e5 and black pawn on d5
        assertTrue(
            rank5.contains('P') && rank5.contains('p'),
            "Should have both pawns on rank 5 in French Defense"
        )
    }

    @Test
    fun `test piece development tracking`() {
        // Test that knights are properly tracked when developed
        val moves = "Nf3 Nc6 Nc3 Nf6"
        
        val fenAnalysis = runBlocking {
            chessCoachingService.analyzeFENStructured(moves)
        }
        
        println("\nKnight development test for '$moves':")
        println("- FEN: ${fenAnalysis.fen}")
        println("- Valid: ${fenAnalysis.isValid}")
        
        assertTrue(fenAnalysis.isValid, "Knight development should create valid position")
        
        // Check that knights have moved from starting squares
        val fen = fenAnalysis.fen
        
        // Starting position has knights on b1,g1 (White) and b8,g8 (Black)
        // After Nf3 Nc6 Nc3 Nf6, they should be on f3,c3 (White) and c6,f6 (Black)
        assertFalse(
            fen.contains("RNBQKBNR") || fen.contains("rnbqkbnr"),
            "Back ranks should show knights have moved"
        )
    }
} 