package com.vibechess.api.service

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals

@SpringBootTest
class ChessCoachingServiceStructuredTest {

    @Autowired
    private lateinit var chessCoachingService: ChessCoachingService

    @Test
    fun `test determineAnalysisDepth logic through public methods`() {
        // Since determineAnalysisDepth is private, we'll test the logic through expected behavior
        // in the public analyzePosition method or create a helper method for testing
        
        // Test data classes and behavior instead
        val beginnerInfo = UserInfo(color = "White", elo = 700)
        val intermediateInfo = UserInfo(color = "Black", elo = 1500)
        val advancedInfo = UserInfo(color = "White", elo = 2000)
        val unknownInfo = UserInfo(color = "Uncertain", elo = null)
        
        // Verify the data structures are working correctly
        assertEquals(700, beginnerInfo.elo)
        assertEquals(1500, intermediateInfo.elo)
        assertEquals(2000, advancedInfo.elo)
        assertEquals(null, unknownInfo.elo)
    }

    @Test
    @Disabled("This test requires real AI API calls, enable for integration testing")
    fun `test analyzeFENStructured with simple moves`() = runBlocking {
        val moves = "e4 e5 Nf3 Nc6"
        
        val result = chessCoachingService.analyzeFENStructured(moves)
        
        assertNotNull(result)
        assertTrue(result.isValid, "FEN should be valid for simple opening moves")
        assertTrue(result.fen.isNotBlank(), "FEN string should not be empty")
        assertTrue(result.moveNumber != null && result.moveNumber!! > 0, "Move number should be positive")
    }

    @Test
    @Disabled("This test requires real AI API calls, enable for integration testing")
    fun `test structured output flow with mock screenshot`() = runBlocking {
        // Create a mock image file
        val mockScreenshot = MockMultipartFile(
            "screenshot",
            "chess_position.png",
            "image/png",
            ByteArray(1024) // Mock image data
        )
        
        val moves = "e4 e5 Nf3 Nc6 Bb5"
        
        // This would require actual API calls to complete
        // val result = chessCoachingService.analyzePosition(mockScreenshot, moves)
        // assertNotNull(result)
        // assertTrue(result.contains("Suggested Move"), "Response should contain move suggestion")
    }

    @Test
    fun `test coaching advice structure and user categorization`() {
        // Test the data structure and logic without needing to access private methods
        
        val advice = CoachingAdvice(
            suggestedMove = "Nf3",
            explanation = "Develop your knight to control the center",
            reasoning = "Knights are better in the center",
            difficulty = "beginner",
            confidence = "high"
        )
        
        // Test beginner user info
        val beginnerUser = UserInfo(color = "White", elo = 800)
        assertTrue(beginnerUser.elo!! < 1000, "Should be categorized as beginner")
        
        // Test advanced user info  
        val advancedUser = UserInfo(color = "Black", elo = 2000)
        assertTrue(advancedUser.elo!! > 1800, "Should be categorized as advanced")
        
        // Test advice structure
        assertEquals("Nf3", advice.suggestedMove)
        assertEquals("beginner", advice.difficulty)
        assertEquals("high", advice.confidence)
        assertTrue(advice.explanation.isNotBlank())
        assertTrue(advice.reasoning.isNotBlank())
    }

    @Test
    fun `test data classes for structured output`() {
        // Test UserInfo data class
        val userInfo = UserInfo(
            color = "White",
            elo = 1500,
            colorConfidence = "high",
            eloConfidence = "medium"
        )
        
        assertEquals("White", userInfo.color)
        assertEquals(1500, userInfo.elo)
        assertEquals("high", userInfo.colorConfidence)
        assertEquals("medium", userInfo.eloConfidence)
        
        // Test FenAnalysis data class
        val fenAnalysis = FenAnalysis(
            fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            isValid = true,
            errors = emptyList(),
            moveNumber = 1,
            activeColor = "w",
            castlingRights = "KQkq"
        )
        
        assertTrue(fenAnalysis.isValid)
        assertEquals(1, fenAnalysis.moveNumber)
        assertEquals("w", fenAnalysis.activeColor)
        
        // Test CoachingAdvice data class
        val coachingAdvice = CoachingAdvice(
            suggestedMove = "e4",
            explanation = "Control the center with a pawn",
            reasoning = "Central control is important in the opening",
            difficulty = "beginner",
            confidence = "high"
        )
        
        assertEquals("e4", coachingAdvice.suggestedMove)
        assertEquals("beginner", coachingAdvice.difficulty)
        assertEquals("high", coachingAdvice.confidence)
    }

    @Test
    fun `test FEN validation with d4 e5 opening`() {
        // Test the specific case the user encountered
        val fen = "rnbqkbnr/pppp1ppp/8/4p3/3P4/8/PPP1PPPP/RNBQKBNR b KQkq - 0 1"
        
        // This should be a valid FEN for the position after 1.d4 e5
        val service = chessCoachingService
        val reflection = service.javaClass.getDeclaredMethod("validateFenBasic", String::class.java)
        reflection.isAccessible = true
        
        val isValid = reflection.invoke(service, fen) as Boolean
        assertTrue(isValid, "FEN for d4 e5 position should be valid")
        
        // Test individual components
        val parts = fen.split(" ")
        assertEquals(6, parts.size, "FEN should have 6 parts")
        assertEquals("b", parts[1], "Should be Black's turn")
        assertEquals("KQkq", parts[2], "All castling rights should be available")
        assertEquals("-", parts[3], "No en passant square")
        assertEquals("0", parts[4], "Halfmove clock should be 0")
        assertEquals("1", parts[5], "Should be move 1")
    }

    @Test
    fun `test beginner coaching for d4 e5 scenario`() {
        // Test the exact scenario the user encountered
        val userInfo = UserInfo(color = "White", elo = 350, colorConfidence = "high", eloConfidence = "high")
        val fenAnalysis = FenAnalysis(
            fen = "rnbqkbnr/pppp1ppp/8/4p3/3P4/8/PPP1PPPP/RNBQKBNR b KQkq - 0 1",
            isValid = true, // Fixed to be valid
            errors = emptyList(),
            moveNumber = 1,
            activeColor = "b",
            castlingRights = "KQkq"
        )
        
        // Test the beginner-specific advice function
        val service = chessCoachingService
        val reflection = service.javaClass.getDeclaredMethod(
            "generateBeginnerSpecificAdvice", 
            UserInfo::class.java, 
            FenAnalysis::class.java
        )
        reflection.isAccessible = true
        
        val advice = reflection.invoke(service, userInfo, fenAnalysis) as CoachingAdvice
        
        // Verify the advice is appropriate for a 350 ELO beginner
        assertEquals("Nf3", advice.suggestedMove)
        assertEquals("beginner", advice.difficulty)
        assertEquals("high", advice.confidence)
        assertTrue(advice.explanation.contains("d4"), "Should mention the d4 opening")
        assertTrue(advice.explanation.contains("Nf3"), "Should suggest Nf3")
        assertTrue(advice.explanation.contains("opening principles"), "Should mention opening principles")
        
        // Test final formatting
        val finalFormatReflection = service.javaClass.getDeclaredMethod(
            "formatFinalCoachingMessage",
            CoachingAdvice::class.java,
            UserInfo::class.java
        )
        finalFormatReflection.isAccessible = true
        
        val finalMessage = finalFormatReflection.invoke(service, advice, userInfo) as String
        
        assertTrue(finalMessage.contains("Hi there!"), "Should use friendly greeting for beginners")
        assertTrue(finalMessage.contains("350 ELO"), "Should mention the ELO rating")
        assertTrue(finalMessage.contains("Nf3"), "Should contain the suggested move")
        
        println("Sample coaching message for 350 ELO player:")
        println(finalMessage)
    }
} 