package com.vibechess.api.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.util.MimeTypeUtils
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.content.Media
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.core.io.ByteArrayResource

// Structured output models for better data handling
data class UserInfo(
    val color: String?, // "White", "Black", or "Uncertain"
    val elo: Int?, // null if uncertain
    val colorConfidence: String? = null,
    val eloConfidence: String? = null
)

data class CoachingAdvice(
    val suggestedMove: String,
    val explanation: String,
    val reasoning: String,
    val difficulty: String, // "beginner", "intermediate", "advanced"
    val confidence: String // "high", "medium", "low"
)

@Service
class ChessCoachingService(
    private val builder: ChatClient.Builder,
    private val chessApiTool: ChessApiTool
) {
    private val chatClient = builder.build()

    /**
     * Main analysis function - improved version with structured outputs and ELO-based depth
     */
    suspend fun analyzePosition(screenshot: MultipartFile, moves: String): String? {
        // Step 1: Extract user info with structured output
        val userInfo = getUserInfoStructured(screenshot)
        println("User Info: $userInfo")

        // Step 2: Determine appropriate analysis depth based on ELO
        val analysisDepth = determineAnalysisDepth(userInfo.elo)
        println("Analysis depth for ELO ${userInfo.elo}: $analysisDepth")

        // Step 3: Calculate move number for coaching context
        val moveNumber = extractMoveNumber(moves)
        println("Position Info: Move $moveNumber")

        // Step 4: Create a ChatClient with the chess analysis tool
        val clientWithTool = builder
            .defaultTools(chessApiTool)
            .build()

        // Step 5: Let the AI use the tool to analyze and provide coaching advice
        val skillLevel = when {
            userInfo.elo == null -> "intermediate"
            userInfo.elo < 1000 -> "beginner"
            userInfo.elo < 1800 -> "intermediate" 
            else -> "advanced"
        }

        val userColor = when (userInfo.color?.lowercase()) {
            "white" -> "White"
            "black" -> "Black"
            else -> "Unknown"
        }

        // System prompt - defines the AI's role and behavior
        val systemPrompt = """
            You are an expert chess coach helping players improve their game. Your coaching style should be:
            
            COACHING PRINCIPLES:
            - Engaging and educational - make chess exciting!
            - Tailored to skill level (beginner/intermediate/advanced)
            - Use proper chess terminology and opening names when applicable
            - Focus on WHY moves are good, not just WHAT to play
            
            CHESS EDUCATION GUIDELINES:
            - Identify famous openings when possible (Queen's Gambit, Sicilian Defense, Italian Game, etc.)
            - Use engaging chess terminology: "controls the long diagonal", "develops with tempo", "centralizes the knight"
            - Mention strategic themes: piece development, king safety, pawn breaks, space advantage
            - For beginners: Explain WHY moves are good in simple terms
            - For higher ELO: Include deeper strategic concepts and opening theory
            
            COMMON OPENING PATTERNS TO RECOGNIZE:
            - d4 d5 c4 = Queen's Gambit ("offering a pawn for central control and rapid development")
            - e4 e5 Nf3 = King's Pawn Game ("classical development fighting for the center")
            - e4 c5 = Sicilian Defense ("Black's most ambitious defense, fighting for counterplay")
            - d4 Nf6 = Indian Defense systems ("hypermodern approach, controlling center from afar")
            
            ANALYSIS APPROACH:
            1. Use the chess analysis tool to analyze the current position from the given moves
            2. Provide specific move recommendations based on the analysis
            3. Explain the reasoning appropriate to the player's skill level
            
            Always make your coaching engaging and educational!
        """.trimIndent()

        // User prompt - contains specific player data and request
        val userPrompt = """
            Please analyze this chess position and provide coaching advice.
            
            Player Information:
            - ELO Rating: ${userInfo.elo ?: "Unknown"}
            - Playing as: $userColor
            - Skill Level: $skillLevel
            - Current Move Number: $moveNumber
            
            Game Moves So Far: "$moves"
            
            Instructions:
            - Use analysis depth of $analysisDepth for this player's skill level
            - Provide specific coaching advice tailored to $skillLevel level
            - Suggest the best move and explain why it's good
            - Make the explanation engaging and educational
        """.trimIndent()

        return try {
            val response = clientWithTool.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content()
            
            println("AI Response with Tool: $response")
            response
        } catch (e: Exception) {
            println("Error with tool-enabled analysis: ${e.message}")
            // Fallback to basic advice
            generateBasicAdvice(userInfo, moves, moveNumber)
        }
    }

    /**
     * Determine analysis depth based on player ELO
     */
    private fun determineAnalysisDepth(elo: Int?): Int {
        return when {
            elo == null -> 12 // Default for uncertain ELO
            elo < 800 -> 8    // Beginners need simpler analysis
            elo < 1200 -> 10  // Intermediate beginners
            elo < 1600 -> 12  // Club level
            elo < 2000 -> 15  // Advanced
            else -> 18        // Expert level (max depth)
        }
    }

    /**
     * Extract move number from moves string for coaching context
     */
    private fun extractMoveNumber(moves: String): Int {
        if (moves.isBlank()) return 1
        
        // Count the number of moves (split by spaces, filter out empty)
        val moveList = moves.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        
        // Each full move consists of White move + Black move
        // So move number = (total moves + 1) / 2
        return (moveList.size + 1) / 2
    }

    /**
     * Get descriptive piece name from engine piece notation
     */
    private fun getPieceDescription(piece: String?): String {
        return when (piece?.lowercase()) {
            "p" -> "pawn"
            "n" -> "knight"
            "b" -> "bishop"
            "r" -> "rook"
            "q" -> "queen"
            "k" -> "king"
            else -> "piece"
        }
    }

    /**
     * Extract user info with structured output using Spring AI's BeanOutputConverter
     */
    suspend fun getUserInfoStructured(screenshot: MultipartFile): UserInfo {
        val systemPrompt = """
            You are a vision system specialized in parsing Chess.com interfaces. 
            
            Your job is to extract user information from chess game screenshots with high accuracy.
            
            Rules:
            - Determine user color from board orientation (user typically at bottom)
            - Extract ELO rating from username area
            - If ELO is unclear or unreadable, set to null
            - Assess confidence levels based on image clarity
            - Only use what is visible in the image - do not guess or infer
        """.trimIndent()

        val userPrompt = """
            Analyze this Chess.com game screenshot and extract the user information.
            
            Please identify:
            1. User's color (White/Black) based on board orientation
            2. User's ELO rating from the visible interface
            3. Confidence levels for both pieces of information
        """.trimIndent()

        val mimeType = MimeTypeUtils.parseMimeType(screenshot.contentType ?: "image/png")
        val resource = ByteArrayResource(screenshot.bytes)
        val media = Media(mimeType, resource)
        val options = OpenAiChatOptions.builder()
            .model("gpt-4o")
            .temperature(0.1)
            .build()

        return try {
            // Use Spring AI's structured output capability
            val result = chatClient.prompt()
                .system(systemPrompt)
                .user { userMessageBuilder ->
                    userMessageBuilder.text(userPrompt).media(media)
                }
                .options(options)
                .call()
                .entity(UserInfo::class.java)
            
            result ?: UserInfo(color = "Uncertain", elo = null)
        } catch (e: Exception) {
            println("Error parsing user info: ${e.message}")
            UserInfo(color = "Uncertain", elo = null)
        }
    }

    /**
     * Generate basic coaching advice as fallback when tool analysis fails
     */
    private fun generateBasicAdvice(userInfo: UserInfo, moves: String, moveNumber: Int): String {
        val skillLevel = when {
            userInfo.elo == null -> "intermediate"
            userInfo.elo < 1000 -> "beginner"
            userInfo.elo < 1800 -> "intermediate" 
            else -> "advanced"
        }

        return when {
            moveNumber <= 10 -> {
                """Hello! You're in the opening phase (move $moveNumber). 
                
As a $skillLevel player, focus on these opening principles:
1. Control the center with pawns (e4, d4)
2. Develop knights before bishops
3. Castle early to keep your king safe
4. Don't move the same piece twice

Current moves: $moves

Keep developing your pieces and maintain central control!"""
            }
            moveNumber <= 25 -> {
                """You're in the middlegame now (move $moveNumber)! 
                
Focus on:
1. Look for tactical opportunities (forks, pins, skewers)
2. Improve your piece positions
3. Create threats against your opponent
4. Keep your king safe

Remember to calculate your moves carefully and look for your opponent's threats too!"""
            }
            else -> {
                """Welcome to the endgame! This is where precision matters most.
                
Key endgame principles:
1. Activate your king - it's a strong piece in the endgame
2. Push passed pawns toward promotion
3. Stop your opponent's dangerous pawns
4. Use your pieces actively

Every move counts in the endgame - good luck!"""
            }
        }
    }
} 