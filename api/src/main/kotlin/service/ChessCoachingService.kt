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

        val promptText = """
            You are an expert chess coach helping a player during their game. 

            Player Information:
            - ELO Rating: ${userInfo.elo ?: "Unknown"}
            - Playing as: $userColor
            - Skill Level: $skillLevel
            - Current Move Number: $moveNumber

            Game Moves So Far: "$moves"

            Your task is to:
            1. Use the chess analysis tool to analyze the current position from these moves
            2. Use analysis depth of $analysisDepth for this player's skill level
            3. Based on the analysis, provide specific coaching advice

            For the coaching advice, please:
            - Suggest the best move and explain why
            - Tailor the explanation to the player's skill level ($skillLevel)
            - For beginners: Focus on basic principles, safety, piece development
            - For intermediate: Include positional concepts and tactical awareness  
            - For advanced: Discuss deeper strategy and complex calculations

            IMPORTANT CHESS EDUCATION GUIDELINES:
            - Identify famous openings when possible (Queen's Gambit, Sicilian Defense, Italian Game, etc.)
            - Use engaging chess terminology: "controls the long diagonal", "develops with tempo", "centralizes the knight"
            - Mention strategic themes: piece development, king safety, pawn breaks, space advantage
            - For beginners: Explain WHY moves are good in simple terms
            - For higher ELO: Include deeper strategic concepts and opening theory

            Make your coaching engaging and educational! Use the analysis tool to get accurate position evaluation and move recommendations.
        """.trimIndent()

        return try {
            val response = clientWithTool.prompt()
                .user(promptText)
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
        val promptText = """
            You are a vision system specialized in parsing Chess.com interfaces. Analyze the screenshot and extract user information.

            Rules:
            - Determine user color from board orientation (user typically at bottom)
            - Extract ELO rating from username area
            - If ELO is unclear or unreadable, set to null
            - Assess confidence levels based on image clarity
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
                .user { userMessageBuilder ->
                    userMessageBuilder.text(promptText).media(media)
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
     * Generate move advice using the chess position and tools for analysis
     */
    suspend fun getAdviceWithTools(userInfo: String?, fen: String?): String? {
        val promptText = """
            You are a real-time chess coach helping a player during their game.
            
            Player Info: $userInfo
            Current Position FEN: $fen
            
            First, analyze the position to understand what's happening on the board.
            Then, give advice that's appropriate for the player's skill level.
            Your response should be conversational and encouraging, with specific advice for the next move.
        """.trimIndent()

        // Create a chat client with the ChessApiTool
        val clientWithTool = builder
            .defaultTools(chessApiTool)
            .build()

        val userMessage = UserMessage.builder().text(promptText).build()
        val options = OpenAiChatOptions.builder()
            .model("gpt-4o")
            .build()
        val prompt = Prompt(userMessage, options)

        return try {
            clientWithTool.prompt(prompt).call().content()
        } catch (e: Exception) {
            "Unable to generate advice at this time: ${e.message}"
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