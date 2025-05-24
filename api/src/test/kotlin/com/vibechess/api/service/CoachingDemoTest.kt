package com.vibechess.api.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
class CoachingDemoTest {

    @Autowired
    private lateinit var chessCoachingService: ChessCoachingService

    @Test
    fun `demo coaching for 350 ELO d4 e5 scenario`() {
        // Recreate the exact scenario the user encountered
        val userInfo = UserInfo(
            color = "White", 
            elo = 350, 
            colorConfidence = "high", 
            eloConfidence = "high"
        )
        
        val fenAnalysis = FenAnalysis(
            fen = "rnbqkbnr/pppp1ppp/8/4p3/3P4/8/PPP1PPPP/RNBQKBNR b KQkq - 0 1",
            isValid = true, // Now correctly marked as valid
            errors = emptyList(),
            moveNumber = 1,
            activeColor = "b",
            castlingRights = "KQkq"
        )
        
        // Get beginner-specific advice
        val service = chessCoachingService
        val beginnerAdviceMethod = service.javaClass.getDeclaredMethod(
            "generateBeginnerSpecificAdvice", 
            UserInfo::class.java, 
            FenAnalysis::class.java
        )
        beginnerAdviceMethod.isAccessible = true
        
        val advice = beginnerAdviceMethod.invoke(service, userInfo, fenAnalysis) as CoachingAdvice
        
        // Format the final message
        val formatMethod = service.javaClass.getDeclaredMethod(
            "formatFinalCoachingMessage",
            CoachingAdvice::class.java,
            UserInfo::class.java
        )
        formatMethod.isAccessible = true
        
        val finalMessage = formatMethod.invoke(service, advice, userInfo) as String
        
        println("=".repeat(60))
        println("IMPROVED COACHING OUTPUT FOR 350 ELO PLAYER")
        println("Moves: d4 e5")
        println("=".repeat(60))
        println(finalMessage)
        println("=".repeat(60))
        
        // Show the structured data
        println("\nStructured Advice Object:")
        println("- Suggested Move: ${advice.suggestedMove}")
        println("- Difficulty: ${advice.difficulty}")
        println("- Confidence: ${advice.confidence}")
        println("- Reasoning: ${advice.reasoning}")
    }

    @Test
    fun `demo coaching for 350 ELO endgame scenario - 30 moves`() {
        // Recreate the user's actual complex endgame scenario
        val userInfo = UserInfo(
            color = "White", 
            elo = 350, 
            colorConfidence = "high", 
            eloConfidence = "high"
        )
        
        val fenAnalysis = FenAnalysis(
            fen = "8/1k1n1p2/b1p1p3/3b4/2P5/1P2P1P1/P2P1Q2/1R3B1B w - - 0 30",
            isValid = true,
            errors = emptyList(),
            moveNumber = 30, // This is the key - it's move 30, not opening!
            activeColor = "w",
            castlingRights = "-"
        )
        
        // Get beginner-specific advice for endgame
        val service = chessCoachingService
        val beginnerAdviceMethod = service.javaClass.getDeclaredMethod(
            "generateBeginnerSpecificAdvice", 
            UserInfo::class.java, 
            FenAnalysis::class.java
        )
        beginnerAdviceMethod.isAccessible = true
        
        val advice = beginnerAdviceMethod.invoke(service, userInfo, fenAnalysis) as CoachingAdvice
        
        // Format the final message
        val formatMethod = service.javaClass.getDeclaredMethod(
            "formatFinalCoachingMessage",
            CoachingAdvice::class.java,
            UserInfo::class.java
        )
        formatMethod.isAccessible = true
        
        val finalMessage = formatMethod.invoke(service, advice, userInfo) as String
        
        println("=".repeat(60))
        println("IMPROVED ENDGAME COACHING FOR 350 ELO PLAYER")
        println("Move 30 - Complex Endgame Position")
        println("=".repeat(60))
        println(finalMessage)
        println("=".repeat(60))
        
        // Verify it's giving endgame advice, not opening advice
        assertFalse(finalMessage.contains("Nf3"), "Should not suggest opening moves in endgame")
        assertFalse(finalMessage.contains("develop"), "Should not talk about development in endgame")
        assertTrue(
            finalMessage.contains("endgame") || 
            finalMessage.contains("king") || 
            finalMessage.contains("pawn"), 
            "Should contain endgame concepts"
        )
        
        println("\nStructured Advice Analysis:")
        println("- Suggested Move: ${advice.suggestedMove}")
        println("- Difficulty: ${advice.difficulty}")
        println("- Confidence: ${advice.confidence}")
        println("- Contains endgame advice: ${advice.explanation.contains("endgame")}")
    }

    @Test
    fun `demo coaching for 342 ELO opening scenario - move 4`() {
        // Test the user's latest scenario - opening position at move 4
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
            moveNumber = 4, // Early opening
            activeColor = "w",
            castlingRights = "KQkq"
        )
        
        // Get beginner-specific advice for opening
        val service = chessCoachingService
        val beginnerAdviceMethod = service.javaClass.getDeclaredMethod(
            "generateBeginnerSpecificAdvice", 
            UserInfo::class.java, 
            FenAnalysis::class.java
        )
        beginnerAdviceMethod.isAccessible = true
        
        val advice = beginnerAdviceMethod.invoke(service, userInfo, fenAnalysis) as CoachingAdvice
        
        // Format the final message
        val formatMethod = service.javaClass.getDeclaredMethod(
            "formatFinalCoachingMessage",
            CoachingAdvice::class.java,
            UserInfo::class.java
        )
        formatMethod.isAccessible = true
        
        val finalMessage = formatMethod.invoke(service, advice, userInfo) as String
        
        println("=".repeat(60))
        println("OPENING COACHING FOR 342 ELO PLAYER")
        println("Move 4 - Early Opening Position")
        println("After: d4 f6 Bf4 d5 Nf3 g5 (White to move)")
        println("=".repeat(60))
        println(finalMessage)
        println("=".repeat(60))
        
        // Verify it's giving opening advice
        assertTrue(
            finalMessage.contains("opening") || 
            finalMessage.contains("develop") || 
            finalMessage.contains("center"), 
            "Should contain opening concepts"
        )
        assertFalse(finalMessage.contains("endgame"), "Should not mention endgame in opening")
        
        println("\nPosition Analysis:")
        println("- Move Number: ${fenAnalysis.moveNumber}")
        println("- Pieces on Board: ${fenAnalysis.fen.split(" ")[0].count { it.isLetter() }}")
        println("- Game Phase: Opening")
        println("- Suggested Move: ${advice.suggestedMove}")
        println("- Advice Type: ${advice.difficulty}")
    }
} 