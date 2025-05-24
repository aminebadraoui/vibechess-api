package com.vibechess.api.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.coroutines.runBlocking

@SpringBootTest
class PawnMovementTest {

    @Autowired
    private lateinit var chessCoachingService: ChessCoachingService

    @Test
    fun `test pawn movement with square clearing`() {
        // This is the exact sequence that is still causing "wrong FEN" errors
        val moves = "e4 e6 d3 d5 exd5 exd5 d4 b6 c4 Bb4+ Bd2"
        
        val fenAnalysis = runBlocking {
            chessCoachingService.analyzeFENStructured(moves)
        }
        
        println("Generated FEN Analysis for '$moves':")
        println("- FEN: ${fenAnalysis.fen}")
        println("- Valid: ${fenAnalysis.isValid}")
        println("- Errors: ${fenAnalysis.errors}")
        
        // Extract ranks to check specific positions
        val fenParts = fenAnalysis.fen.split(" ")
        val boardPart = fenParts[0]
        val ranks = boardPart.split("/")
        
        val rank4 = ranks[4] // rank 4 (0-indexed: rank 8=0, rank 4=4)
        val rank3 = ranks[5] // rank 3 (0-indexed: rank 8=0, rank 3=5)
        val rank2 = ranks[6] // rank 2
        val rank1 = ranks[7] // rank 1
        
        println("- Rank 4: '$rank4' (should have White pawns on c4 and d4)")
        println("- Rank 3: '$rank3' (should be mostly empty - d3 pawn moved to d4)")
        println("- Rank 2: '$rank2' (should show White bishop on d2)")
        println("- Rank 1: '$rank1' (should show bishop moved from c1)")
        
        assertTrue(fenAnalysis.isValid, "FEN should be marked as valid")
        
        // Key checks:
        // 1. After d3 then d4, rank 3 should NOT have a pawn on d-file
        assertFalse(
            rank3.contains("3P") || rank3.contains("P3"),
            "Rank 3 should not have pawn on d-file after d3-d4 move, but got: '$rank3'"
        )
        
        // 2. Rank 4 should have pawns on c4 and d4
        assertTrue(
            rank4.contains("PP") || (rank4.contains("P") && rank4.length >= 4),
            "Rank 4 should have White pawns on c4 and d4, but got: '$rank4'"
        )
        
        // 3. White bishop should be on d2 (rank 2)
        assertTrue(
            rank2.contains("B"),
            "Rank 2 should have White bishop on d2, but got: '$rank2'"
        )
    }

    @Test
    fun `test simple pawn movement clearing`() {
        // Test basic pawn movement to ensure squares are cleared
        val moves = "d3 e6 d4"
        
        val fenAnalysis = runBlocking {
            chessCoachingService.analyzeFENStructured(moves)
        }
        
        println("\nSimple pawn movement test for '$moves':")
        println("- FEN: ${fenAnalysis.fen}")
        println("- Valid: ${fenAnalysis.isValid}")
        
        val fenParts = fenAnalysis.fen.split(" ")
        val boardPart = fenParts[0]
        val ranks = boardPart.split("/")
        
        val rank4 = ranks[4] // rank 4
        val rank3 = ranks[5] // rank 3
        
        println("- Rank 4: '$rank4' (should have White pawn on d4)")
        println("- Rank 3: '$rank3' (should NOT have White pawn on d3)")
        
        assertTrue(fenAnalysis.isValid, "Simple pawn movement should be valid")
        
        // After d3 d4, rank 3 should be empty of the d-pawn
        assertFalse(
            rank3.matches(Regex(".*[3-8]P.*")),
            "Rank 3 should not have pawn on d-file after it moved to d4, but got: '$rank3'"
        )
        
        // Rank 4 should have the pawn
        assertTrue(
            rank4.contains("P"),
            "Rank 4 should have White pawn after d4 move, but got: '$rank4'"
        )
    }

    @Test
    fun `test capture and recapture sequence`() {
        // Test the exd5 exd5 sequence specifically
        val moves = "e4 e6 d4 d5 exd5 exd5"
        
        val fenAnalysis = runBlocking {
            chessCoachingService.analyzeFENStructured(moves)
        }
        
        println("\nCapture sequence test for '$moves':")
        println("- FEN: ${fenAnalysis.fen}")
        println("- Valid: ${fenAnalysis.isValid}")
        
        val fenParts = fenAnalysis.fen.split(" ")
        val boardPart = fenParts[0]
        val ranks = boardPart.split("/")
        
        val rank5 = ranks[3] // rank 5
        val rank4 = ranks[4] // rank 4
        
        println("- Rank 5: '$rank5' (should have black pawn on d5)")
        println("- Rank 4: '$rank4' (should have White pawn on d4, NO pawn on e4)")
        
        assertTrue(fenAnalysis.isValid, "Capture sequence should be valid")
        
        // After exd5 exd5, should have black pawn on d5 and White pawn on d4
        assertTrue(
            rank5.contains("p"),
            "Rank 5 should have black pawn on d5 after recapture, but got: '$rank5'"
        )
        
        // Should NOT have White pawn on e4 anymore (it captured on d5)
        assertFalse(
            rank4.matches(Regex(".*4P.*")) || rank4.matches(Regex(".*P4.*")),
            "Rank 4 should not have pawn on e-file after exd5, but got: '$rank4'"
        )
    }
} 