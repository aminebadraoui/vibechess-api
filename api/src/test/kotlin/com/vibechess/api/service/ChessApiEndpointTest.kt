package com.vibechess.api.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
class ChessApiEndpointTest {

    @Autowired
    private lateinit var chessApiTool: ChessApiTool

    @Test
    @Disabled("Enable to test actual API endpoint - requires internet connection")
    fun `test chess-api endpoint with correct URL`() {
        // Test the exact position from user's scenario
        val fen = "rnbqkbnr/ppp1p1pp/5p2/3p2p1/3P1B2/5N2/PPP1PPPP/RN1QKB1R w KQkq - 0 4"
        
        val result = chessApiTool.getAnalysisResponse(fen, depth = 8)
        
        println("Chess API Response:")
        println("- Text: ${result?.text}")
        println("- Eval: ${result?.eval}")
        println("- Best Move: ${result?.san ?: result?.move}")
        println("- Depth: ${result?.depth}")
        println("- Win Chance: ${result?.winChance}")
        
        assertNotNull(result, "Should get a response from chess-api.com")
        assertNotNull(result.text, "Response should have text description")
        assertTrue(result.text.isNotBlank(), "Text should not be empty")
    }

    @Test
    fun `test opening position structure for 342 ELO`() {
        // Test the user's scenario data without API call
        val userInfo = UserInfo(
            color = "white", 
            elo = 342, 
            colorConfidence = "high", 
            eloConfidence = "high"
        )
        
        val fenAnalysis = FenAnalysis(
            fen = "rnbqkbnr/ppp1p1pp/5p2/3p2p1/3P1B2/5N2/PPP1PPPP/RN1QKB1R w KQkq - 0 4",
            isValid = true,
            errors = emptyList(),
            moveNumber = 4,
            activeColor = "w",
            castlingRights = "KQkq"
        )
        
        // This is move 4 with many pieces - should be opening advice
        assertTrue(fenAnalysis.moveNumber!! <= 10, "Should be in opening phase")
        
        // Count pieces to verify it's still opening
        val pieceCount = fenAnalysis.fen.split(" ")[0].count { it.isLetter() }
        assertTrue(pieceCount > 24, "Should have most pieces still on board")
        
        println("Position Analysis:")
        println("- Move: ${fenAnalysis.moveNumber}")
        println("- Piece Count: $pieceCount")
        println("- ELO: ${userInfo.elo}")
        println("- Expected Phase: Opening")
    }
} 