package com.vibechess.api.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vibechess.api.controller.CoachingResponse
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.core.ParameterizedTypeReference
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

data class FenAnalysis(
    val fen: String,
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val moveNumber: Int? = null,
    val activeColor: String? = null,
    val castlingRights: String? = null
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
    private val objectMapper = jacksonObjectMapper()

    suspend fun extractUserInfoFromScreenshot(screenshot: MultipartFile): String? {
        val promptText = """
            Analyze the attached chess screenshot. 
            - What is the ELO rating of the user (bottom player)?
            - Is the user playing as white or black?
            Respond in a short, clear sentence.
        """.trimIndent()

        val mimeType = MimeTypeUtils.parseMimeType(screenshot.contentType ?: "image/png")
        val resource = ByteArrayResource(screenshot.bytes)
        val media = Media(mimeType, resource)
        val userMessage = UserMessage.builder().text(promptText).media(listOf(media)).build()
        val options = OpenAiChatOptions.builder().model("gpt-4o").build()
        val prompt = Prompt(userMessage, options)

        return try {
            chatClient.prompt(prompt).call().content()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun generateCoachingAdvice(moves: String, userInfo: String?): String? {
        val promptText = """
            You are a chess coach. Here is information about the user: $userInfo
            For all position and move analysis, rely solely on the provided array of moves below. 
            Carefully reconstruct the board position from the moves list, step by step, and do not make any assumptions about the position or pawn structure beyond what the moves indicate.
            Track captures and piece movements carefully. If a piece is captured (e.g., Nc3 Bxc3+), it is no longer on the board. If a piece moves to a new square, it is no longer on its previous square. Do not reference pieces that have been captured or moved away.
            Here are the moves played so far: $moves
            Give advice that is appropriate for the user's ELO and color, and be specific to the position reached by the moves.
            Respond as a real coach would: be conversational, encouraging, and specific to the user's skill and the current position.
            Do NOT return JSON. Just give your advice in natural language.
        """.trimIndent()

        val userMessage = UserMessage.builder().text(promptText).build()
        val options = OpenAiChatOptions.builder().model("gpt-4.1").build()
        val prompt = Prompt(userMessage, options)

        return try {
            chatClient.prompt(prompt).call().content()
        } catch (e: Exception) {
            "Unable to generate advice at this time."
        }
    }

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

        // Step 3: Get engine analysis directly from moves (bypassing FEN generation)
        val engineAnalysis = chessApiTool.getAnalysisResponseFromMoves(
            moves = moves,
            depth = analysisDepth,
            variants = if (userInfo.elo != null && userInfo.elo > 1500) 3 else 1
        )
        println("Engine Analysis: $engineAnalysis")

        // Step 4: Create a simplified FEN analysis from engine response (for coaching logic)
        val fenAnalysis = if (engineAnalysis != null) {
            FenAnalysis(
                fen = engineAnalysis.fen ?: "",
                isValid = true,
                errors = emptyList(),
                moveNumber = extractMoveNumber(moves),
                activeColor = engineAnalysis.turn,
                castlingRights = null
            )
        } else {
            // Fallback: try our AI-based FEN generation
            println("Engine analysis failed, falling back to AI FEN generation")
            analyzeFENStructured(moves)
        }
        println("Position Info: Move ${fenAnalysis.moveNumber}, Active: ${fenAnalysis.activeColor}")

        // Step 5: Generate coaching advice with validation
        val coachingAdvice = if (engineAnalysis != null) {
            generateStructuredCoachingAdvice(userInfo, fenAnalysis, engineAnalysis)
        } else {
            generateFallbackAdvice(userInfo, fenAnalysis)
        }
        
        println("Coaching Advice: $coachingAdvice")

        // Step 6: Format final output
        return formatFinalCoachingMessage(coachingAdvice, userInfo)
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
     * Analyze FEN with structured output using Spring AI's BeanOutputConverter
     */
    suspend fun analyzeFENStructured(moves: String): FenAnalysis {
        val promptText = """
            You are a chess rules expert. Convert the given moves to FEN notation.
            
            Moves to process: $moves
            
            Rules:
            1. Start from the standard initial position: rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
            2. Apply each move in sequence following chess rules exactly
            3. Track piece locations carefully - when a piece moves, it leaves its old square and occupies the new square
            4. Generate the complete FEN string for the final position
            5. Set isValid to true if all moves are legal and FEN is correct
            6. Only set isValid to false if there are actual illegal moves or errors
            
            CRITICAL Move Tracking - CLEAR OLD SQUARES:
            - Track WHITE moves (odd positions): d4, e4, Nc3, e5, a3...
            - Track BLACK moves (even positions): e6, Nf6, d5, Bb4...
            - When a piece moves, the OLD square becomes EMPTY and the NEW square has the piece
            - When White plays "d3" then "d4": d3 becomes empty, d4 gets the pawn
            - When Black plays "Bb4+": f8 becomes empty, b4 gets the bishop
            - When White plays "Bd2": c1 becomes empty, d2 gets the bishop
            - Captures: When "exd5", the e4 pawn disappears, d5 gets the capturing pawn
            - Example: After "d4 e6 e4 Nf6 Nc3 d5 e5", rank 5 has both pawns: "3pP3"
            
            En Passant Rules:
            - En passant is ONLY possible immediately after a pawn moves 2 squares AND an enemy pawn can capture it
            - Most sequences have no en passant, so use "-"
            
            FEN Format:
            - Piece placement: 8 ranks from rank 8 to rank 1, using / as separator
            - White pieces: PNBRQK (uppercase), Black pieces: pnbrqk (lowercase)
            - Active color: "w" for White's turn, "b" for Black's turn  
            - Castling rights: "KQkq" if all castling available, or combination, or "-" if none
            - En passant: Target square ONLY if en passant capture is legally possible, otherwise "-"
            - Halfmove clock: Number of halfmoves since last capture or pawn move
            - Fullmove number: Increments after Black's move, starts at 1
            
            Examples:
            - After 1.d4 d5: rnbqkbnr/pppp1ppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2
            - After 1.d4 e6 2.e4 Nf6 3.Nc3 d5 4.e5: rnbqkb1r/pppp1ppp/4pn2/3pP3/3P4/2N5/PPP2PPP/R1BQKBNR b KQkq - 0 4
            - After 1.e4 e6 2.d3 d5 3.exd5 exd5 4.d4: d3 is NOW EMPTY, d4 has the pawn, d5 has black pawn
        """.trimIndent()

        val options = OpenAiChatOptions.builder()
            .model("gpt-4o")
            .temperature(0.1)
            .build()

        return try {
            // Use Spring AI's structured output capability
            val result = chatClient.prompt()
                .user(promptText)
                .options(options)
                .call()
                .entity(FenAnalysis::class.java)
            
            val finalResult = result ?: FenAnalysis(fen = "", isValid = false, errors = listOf("No response received"))
            
            // Double-check the FEN validity with our own basic validation
            if (finalResult.isValid == false && finalResult.fen.isNotBlank()) {
                val basicValidation = validateFenBasic(finalResult.fen)
                if (basicValidation) {
                    println("AI marked FEN as invalid, but basic validation passed. Overriding to valid.")
                    return finalResult.copy(isValid = true, errors = emptyList())
                }
            }
            
            finalResult
        } catch (e: Exception) {
            println("Error parsing FEN analysis: ${e.message}")
            FenAnalysis(fen = "", isValid = false, errors = listOf("Failed to parse moves: ${e.message}"))
        }
    }

    /**
     * Basic FEN validation to double-check AI assessment
     */
    private fun validateFenBasic(fen: String): Boolean {
        if (fen.isBlank()) return false
        
        val parts = fen.split(" ")
        if (parts.size != 6) return false
        
        val boardPart = parts[0]
        val activeColor = parts[1]
        val castlingRights = parts[2]
        val enPassant = parts[3]
        val halfmoveClock = parts[4]
        val fullmoveNumber = parts[5]
        
        // Basic checks
        return try {
            // Board should have 8 ranks separated by /
            val ranks = boardPart.split("/")
            if (ranks.size != 8) return false
            
            // Active color should be w or b
            if (activeColor !in listOf("w", "b")) return false
            
            // Castling rights should be valid
            if (!castlingRights.matches(Regex("^[KQkq-]+$"))) return false
            
            // En passant should be valid
            if (!enPassant.matches(Regex("^([a-h][36]|-)$"))) return false
            
            // Additional en passant validation - check if it makes sense
            if (enPassant != "-") {
                val file = enPassant[0]
                val rank = enPassant[1]
                // En passant square should be on rank 3 or 6
                if (rank !in listOf('3', '6')) return false
            }
            
            // Halfmove and fullmove should be numbers
            halfmoveClock.toIntOrNull() != null && fullmoveNumber.toIntOrNull() != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generate structured coaching advice using engine analysis with Spring AI's structured output
     */
    suspend fun generateStructuredCoachingAdvice(
        userInfo: UserInfo, 
        fenAnalysis: FenAnalysis, 
        engineAnalysis: ChessApiResponse
    ): CoachingAdvice {
        val skillLevel = when {
            userInfo.elo == null -> "intermediate"
            userInfo.elo < 1000 -> "beginner"
            userInfo.elo < 1800 -> "intermediate" 
            else -> "advanced"
        }

        // Determine whose turn it is vs who the user is
        val activeColor = fenAnalysis.activeColor ?: "w"
        val userColor = when (userInfo.color?.lowercase()) {
            "white" -> "w"
            "black" -> "b"
            else -> "w" // default assumption
        }
        
        val isUserTurn = activeColor == userColor
        
        val promptText = if (isUserTurn) {
            // It's the user's turn - give them direct advice
            """
            You are an expert chess coach. The player you are coaching is about to move.
            
            Player Info:
            - ELO: ${userInfo.elo ?: "Unknown"}
            - Color: ${if (userColor == "w") "White" else "Black"}
            - Skill Level: $skillLevel
            - IT IS THE PLAYER'S TURN TO MOVE
            
            Position:
            - FEN: ${fenAnalysis.fen}
            - Move Number: ${fenAnalysis.moveNumber}
            
            Engine Analysis (for the player's move):
            - Best Move: ${engineAnalysis.san ?: engineAnalysis.move}
            - Move Details: ${engineAnalysis.from} to ${engineAnalysis.to}
            - Piece Moving: ${getPieceDescription(engineAnalysis.piece)}
            - Evaluation: ${engineAnalysis.eval}
            - Depth: ${engineAnalysis.depth}
            - Win Chance: ${engineAnalysis.winChance}%
            ${if (engineAnalysis.mate != null) "- Mate in: ${engineAnalysis.mate}" else ""}
            
            CRITICAL CHESS RULES - VERIFY ALL CLAIMS:
            1. PIECE MOVEMENT RULES:
               - Pawns: Move forward, capture diagonally
               - Knights: L-shape moves only (2+1 squares)
               - Bishops: Diagonal moves only
               - Rooks: Horizontal/vertical moves only  
               - Queens: Combine bishop + rook movement
               - Kings: One square in any direction
            
            2. ATTACK VERIFICATION:
               - Before claiming "attacks" or "threatens", verify the geometric relationship
               - Bishops CANNOT attack pieces on same rank/file (only diagonals)
               - Rooks CANNOT attack pieces on diagonals (only ranks/files)
               - Example: Bishop on b6 CANNOT attack rook on b1 (same file, not diagonal)
            
            3. EXPLANATION GUIDELINES:
               - If unsure about tactical details, focus on general positional benefits
               - Use phrases like "improves position" rather than claiming specific attacks
               - Piece names: p=pawn, n=knight, b=bishop, r=rook, q=queen, k=king
            
            4. OPENING RECOGNITION & CHESS EDUCATION:
               - Identify famous openings when possible (Queen's Gambit, Sicilian Defense, Italian Game, etc.)
               - Mention strategic themes: pawn breaks, piece development, king safety, space advantage
               - Use engaging chess terminology: "controls the long diagonal", "develops with tempo", "centralizes the knight"
               - For beginners: Explain WHY moves are good in simple terms
               - For higher ELO: Include deeper strategic concepts and opening theory
               
            5. COMMON OPENING PATTERNS:
               - d4 d5 c4 = Queen's Gambit ("offering a pawn for central control and rapid development")
               - e4 e5 Nf3 = King's Pawn Game ("classical development fighting for the center")
               - e4 c5 = Sicilian Defense ("Black's most ambitious defense, fighting for counterplay")
               - d4 Nf6 = Indian Defense systems ("hypermodern approach, controlling center from afar")
            
            Give direct advice to the player about what move they should make next.
            
            Tailor explanation complexity to skill level:
            - Beginner: Focus on basic tactics, safety, piece development
            - Intermediate: Include positional concepts, planning
            - Advanced: Complex strategies, deep calculations
            """
        } else {
            // It's the opponent's turn - explain what opponent might do and how to respond
            """
            You are an expert chess coach. The player's opponent is about to move.
            
            Player Info:
            - ELO: ${userInfo.elo ?: "Unknown"}
            - Color: ${if (userColor == "w") "White" else "Black"}
            - Skill Level: $skillLevel
            - IT IS THE OPPONENT'S TURN TO MOVE
            
            Position:
            - FEN: ${fenAnalysis.fen}
            - Move Number: ${fenAnalysis.moveNumber}
            
            Engine Analysis (showing opponent's likely move):
            - Opponent's Best Move: ${engineAnalysis.san ?: engineAnalysis.move}
            - Move Details: ${engineAnalysis.from} to ${engineAnalysis.to}
            - Piece Moving: ${getPieceDescription(engineAnalysis.piece)}
            - Evaluation: ${engineAnalysis.eval}
            - Depth: ${engineAnalysis.depth}
            - Win Chance: ${engineAnalysis.winChance}%
            ${if (engineAnalysis.mate != null) "- Mate in: ${engineAnalysis.mate}" else ""}
            
            CRITICAL CHESS RULES - VERIFY ALL CLAIMS:
            1. PIECE MOVEMENT RULES:
               - Pawns: Move forward, capture diagonally
               - Knights: L-shape moves only (2+1 squares)
               - Bishops: Diagonal moves only
               - Rooks: Horizontal/vertical moves only  
               - Queens: Combine bishop + rook movement
               - Kings: One square in any direction
            
            2. ATTACK VERIFICATION:
               - Before claiming "attacks" or "threatens", verify the geometric relationship
               - Bishops CANNOT attack pieces on same rank/file (only diagonals)
               - Rooks CANNOT attack pieces on diagonals (only ranks/files)
               - Example: Bishop on b6 CANNOT attack rook on b1 (same file, not diagonal)
            
            3. EXPLANATION GUIDELINES:
               - If unsure about tactical details, focus on general positional benefits
               - Use phrases like "improves position" rather than claiming specific attacks
               - Piece names: p=pawn, n=knight, b=bishop, r=rook, q=queen, k=king
            
            4. OPENING RECOGNITION & CHESS EDUCATION:
               - Identify famous openings when possible (Queen's Gambit, Sicilian Defense, Italian Game, etc.)
               - Mention strategic themes: pawn breaks, piece development, king safety, space advantage
               - Use engaging chess terminology: "controls the long diagonal", "develops with tempo", "centralizes the knight"
               - For beginners: Explain WHY moves are good in simple terms
               - For higher ELO: Include deeper strategic concepts and opening theory
               
            5. COMMON OPENING PATTERNS:
               - d4 d5 c4 = Queen's Gambit ("offering a pawn for central control and rapid development")
               - e4 e5 Nf3 = King's Pawn Game ("classical development fighting for the center")
               - e4 c5 = Sicilian Defense ("Black's most ambitious defense, fighting for counterplay")
               - d4 Nf6 = Indian Defense systems ("hypermodern approach, controlling center from afar")
            
            Explain what the opponent is likely to do and give the player advice on how to respond.
            Start with something like "Your opponent will likely play..." and then suggest how to respond.
            
            Tailor explanation complexity to skill level:
            - Beginner: Focus on basic tactics, safety, piece development
            - Intermediate: Include positional concepts, planning
            - Advanced: Complex strategies, deep calculations
            """
        }.trimIndent()

        val options = OpenAiChatOptions.builder()
            .model("gpt-4o")
            .temperature(0.3)
            .build()

        return try {
            // Use Spring AI's structured output capability
            val result = chatClient.prompt()
                .user(promptText)
                .options(options)
                .call()
                .entity(CoachingAdvice::class.java)
            
            result ?: CoachingAdvice(
                suggestedMove = engineAnalysis.san ?: engineAnalysis.move ?: "See position",
                explanation = "Unable to generate coaching advice at this time.",
                reasoning = "No response received from AI model.",
                difficulty = skillLevel,
                confidence = "low"
            )
        } catch (e: Exception) {
            println("Error generating coaching advice: ${e.message}")
            CoachingAdvice(
                suggestedMove = engineAnalysis.san ?: engineAnalysis.move ?: "See position",
                explanation = "I suggest playing ${engineAnalysis.san ?: engineAnalysis.move ?: "the engine recommended move"}.",
                reasoning = "This move was recommended by the chess engine.",
                difficulty = skillLevel,
                confidence = "low"
            )
        }
    }

    /**
     * Generate fallback advice when engine analysis is unavailable
     */
    private suspend fun generateFallbackAdvice(userInfo: UserInfo, fenAnalysis: FenAnalysis): CoachingAdvice {
        // For very low ELO players, use specific beginner advice instead of complex analysis
        if (userInfo.elo != null && userInfo.elo < 600) {
            return generateBeginnerSpecificAdvice(userInfo, fenAnalysis)
        }
        
        // Check whose turn it is
        val activeColor = fenAnalysis.activeColor ?: "w"
        val userColor = when (userInfo.color?.lowercase()) {
            "white" -> "w"
            "black" -> "b"
            else -> "w"
        }
        val isUserTurn = activeColor == userColor
        
        val clientWithTool = builder
            .defaultTools(chessApiTool)
            .build()

        val promptText = """
            You are a chess coach. The engine analysis is unavailable, but you have access to chess analysis tools.
            
            Player Info: ELO ${userInfo.elo ?: "Unknown"}, Color: ${if (userColor == "w") "White" else "Black"}
            Position FEN: ${fenAnalysis.fen}
            ${if (isUserTurn) "IT IS THE PLAYER'S TURN TO MOVE" else "IT IS THE OPPONENT'S TURN TO MOVE"}
            
            IMPORTANT: Make your coaching engaging and educational!
            - Identify opening names when possible (Queen's Gambit, Sicilian Defense, Italian Game, etc.)
            - Use exciting chess terminology: "controls the long diagonal", "develops with tempo", "centralizes powerfully"
            - Mention strategic themes: piece development, king safety, pawn breaks, space advantage
            - For beginners: Explain WHY moves are good with enthusiasm
            - Include chess history/famous players when relevant
            
            Analyze this position and provide coaching advice appropriate for the player's level.
            ${if (isUserTurn) 
                "Give advice on what move the player should make next." 
            else 
                "Explain what the opponent might do and how the player should respond."
            }
            Focus on general principles and clear, actionable advice - but make it interesting!
        """.trimIndent()

        return try {
            val response = clientWithTool.prompt()
                .user(promptText)
                .call()
                .content()
            
            // Parse the response or create a structured advice object
            CoachingAdvice(
                suggestedMove = "See analysis",
                explanation = response ?: "Unable to provide specific advice at this time.",
                reasoning = "Based on general chess principles and position analysis.",
                difficulty = when {
                    userInfo.elo == null -> "intermediate"
                    userInfo.elo < 1000 -> "beginner"
                    userInfo.elo < 1800 -> "intermediate"
                    else -> "advanced"
                },
                confidence = "medium"
            )
        } catch (e: Exception) {
            // Fall back to beginner advice for any errors
            generateBeginnerSpecificAdvice(userInfo, fenAnalysis)
        }
    }

    /**
     * Format the final coaching message for the user
     */
    private fun formatFinalCoachingMessage(advice: CoachingAdvice, userInfo: UserInfo): String {
        val greeting = when {
            userInfo.elo != null && userInfo.elo < 1000 -> "Hi there! "
            userInfo.elo != null && userInfo.elo > 1800 -> "Hello, "
            else -> "Hello! "
        }

        return buildString {
            append(greeting)
            append("Suggested Move: ${advice.suggestedMove}\n\n")
            append(advice.explanation)
            if (advice.reasoning.isNotBlank() && advice.difficulty != "beginner") {
                append("\n\nWhy this move: ${advice.reasoning}")
            }
            if (userInfo.elo != null) {
                append("\n\n(Advice tailored for ~${userInfo.elo} ELO)")
            }
        }
    }

    suspend fun analyzeFEN(moves: String): String? {
        val promptText = """ 
            You are a chess rules expert and notation engine. I will provide a list of legal chess moves in Standard Algebraic Notation (SAN), beginning from the standard initial position.

            Your job is to:

            Apply the moves exactly, strictly following the rules of chess (e.g., castling, en passant, pawn promotion, captures).

            Output the final position in Forsyth-Edwards Notation (FEN), which must include:

            Accurate piece placement (8 ranks from 8 to 1)

            Correct side to move (w or b)

            Accurate castling rights (KQkq or -)

            Correct en passant target square (or - if none)

            Correct halfmove clock (number of halfmoves since last capture or pawn move)

            Correct fullmove number (starting from 1, incremented after each Black move)

            🔒 Strict rules:

            Do not guess or infer anything not directly derived from the moves.

            If a move is ambiguous or illegal, stop and explain the issue.

            If the final FEN is uncertain in any component, indicate which part is uncertain and why.

            📌 Example (for the move list below):

            Moves: d4 e6 Nf3 c6 Nc3 Nh6 e4 Rg8 Be2 c5 dxc5 Bxc5 O-O f5 exf5 Nxf5 Ne4 Be7

            FEN Output:
            rnbqk1r1/pp1pb1pp/4p3/5n2/4N3/5N2/PPP1BPPP/R1BQ1RK1 w q - 2 10

            Please process the next move list and return only the resulting FEN.
            $moves
        """.trimIndent()


        val userMessage = UserMessage.builder().text(promptText).build()
        val options = OpenAiChatOptions.builder()
            .model("gpt-4o")
            .temperature(0.2)
            .build()
        val prompt = Prompt(userMessage, options)

        return try {
            chatClient.prompt(prompt).call().content()
        } catch (e: Exception) {
            "Unable to generate advice at this time."
        }
    }

    suspend fun getUserInfo(screenshot: MultipartFile): String? {
        val promptText = """
          You are a vision system specialized in parsing Chess.com interfaces. I will upload a screenshot of a Chess.com game. Your job is to strictly and accurately extract two pieces of information:

User's color – Determine whether the user is playing White or Black. This is typically the side displayed at the bottom of the board.

User's rating (ELO) – Extract the rating next to the username shown on the user's side of the board (typically bottom).

🔒 Rules:

Use visual layout only — do not assume user color based on first or second move.

If the board is flipped (e.g., user is playing from the top), detect it from orientation.

If the ELO is obscured or unreadable, respond with "uncertain".

Do not infer or guess. Only use what is visible in the image.

✅ Output format:

Color: White / Black / Uncertain

ELO: [number] / Uncertain
        """.trimIndent()

        val mimeType = MimeTypeUtils.parseMimeType(screenshot.contentType ?: "image/png")
        val resource = ByteArrayResource(screenshot.bytes)
        val media = Media(mimeType, resource)
        val userMessage = UserMessage.builder().text(promptText).media(listOf(media)).build()
        val options = OpenAiChatOptions.builder()
            .model("gpt-4o")
            .temperature(0.2)
            .build()
        val prompt = Prompt(userMessage, options)

        return try {
            chatClient.prompt(prompt).call().content()
        } catch (e: Exception) {
            "Unable to generate advice at this time."
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

    suspend fun nextMoveAdvice(userInfo: String?, fen: String?): String? {
        // Try using the tool-based approach first
        val toolBasedAdvice = getAdviceWithTools(userInfo, fen)
        
        // If that works, return it; otherwise fall back to the original method
        return if (!toolBasedAdvice.isNullOrBlank() && !toolBasedAdvice.contains("Unable to generate")) {
            toolBasedAdvice
        } else {
            // Original implementation as fallback
            val promptText = """ 
                You are a real-time chess coach helping a player during their game. I will provide a block of free-form text containing:
                
                The player's ELO rating
                
                The FEN (Forsyth-Edwards Notation) for the current position
                
                Optional: recent move list or indication of which color the player is
                
                Your job is to:
                
                Extract the ELO and FEN from the input text.
                
                Validate the FEN:
                
                Confirm the board structure is legal and consistent with chess rules.
                
                Determine whose turn it is.
                
                Identify the location and type of all pieces on the board.
                
                Check for hanging (undefended) opponent pieces that can be captured safely.
                
                If one exists, suggest a legal capture of the most valuable hanging piece.
                
                Confirm that the piece doing the capturing exists on the correct square.
                
                If no safe captures are available, suggest a legal, safe, and developmentally sound move, such as developing a knight, bishop, or castling.
                
                Ensure the move is 100% legal based on the FEN. Do not hallucinate pieces (e.g., don't suggest Nf6 if no knight is on f6).
                
                Write a short explanation of the move in a clear, friendly, coaching voice, suitable for the player's ELO.
                
                At low ELOs (below 800), avoid deep strategy and focus on threats, captures, safety, and piece development.
                
                Do not use evaluation scores or engine-style depth (e.g., "+0.35" or "depth 20").
                
                Optionally include the name of a tactic or theme if it's relevant (e.g., fork, pin, hanging piece).
                
                Input:
                ELO: $userInfo
                FEN: $fen
            """.trimIndent()
            
            val userMessage = UserMessage.builder().text(promptText).build()
            val options = OpenAiChatOptions.builder()
                .model("gpt-4o")
                .build()
            val prompt = Prompt(userMessage, options)
            
            try {
                chatClient.prompt(prompt).call().content()
            } catch (e: Exception) {
                "Unable to generate advice at this time."
            }
        }
    }

    suspend fun validator(nextMove: String?): String? {
        val promptText = """ 
You are a strict, rule-based chess validator. I will give you a structured explanation block that includes:

A FEN (Forsyth-Edwards Notation) string

A move suggestion (in notation or natural language)

An explanation of the move

Your job is to evaluate the move strictly based on the FEN, ensuring full rule compliance.

✅ Your Responsibilities:
1. Parse the FEN:
Reconstruct the board.

Determine which side is to move.

Identify all piece locations.

Confirm castling rights and other FEN fields.

2. Validate the Suggested Move:
Check each of the following in this exact order:

🧩 Piece Existence
✅ The piece making the move must exist on the board and belong to the player to move.

♞ Knight Moves:
Must move in an L-shape: two squares in one direction, one perpendicular.

E.g., from f3, valid destinations are: g5, h4, h2, g1, e1, d2, d4, e5

❌ Moves like f3 → e4 are not legal

♝ Bishop Moves:
Must move diagonally

❌ Cannot jump over pieces

♜ Rook Moves:
Must move in straight lines (files or ranks)

❌ Cannot jump over pieces

♛ Queen Moves:
Combines bishop and rook movement

❌ Cannot jump over pieces

♚ King Moves:
One square in any direction

If castling:

Check castling rights from FEN (KQkq)

Ensure squares between king and rook are empty

Ensure the king is not in, passing through, or ending in check

♙ Pawn Moves:
Move 1 square forward (or 2 from starting rank)

Capture only diagonally forward (1 file over, 1 rank up/down)

❌ Cannot move forward into occupied squares

🔫 Captures:
The destination square must be occupied by an opponent's piece

❌ If any check fails:
Respond:
❌ Illegal move. [Explain what's wrong — e.g., "The knight on f3 cannot reach e4."]
Then suggest one correct, legal alternative move, ideally based on basic principles (development, safety, center control)

✅ If all checks pass:
Respond:
✅ Valid move. [Confirm legality and give a brief justification.]
🧪 Example Input (Bad Move):
**ELO and FEN Extraction:**
- FEN: rnbqkb1r/pppppppp/8/8/3Pn3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 0 2

**Move Suggestion:**
- Move: Nxe4

**Explanation:**
Capture the hanging knight on e4 with your knight on f3.
✅ Expected Validator Output:
❌ Illegal move. The knight on f3 cannot move to e4 — that is not a valid knight move. Knights move in L-shapes, and f3 → e4 is only one square diagonally. Consider a legal development move instead, such as Bc4 or Be2.

INPUT:
$nextMove
        """.trimIndent()


        val userMessage = UserMessage.builder().text(promptText).build()
        val options = OpenAiChatOptions.builder()
            .model("gpt-4o")
            .build()
        val prompt = Prompt(userMessage, options)

        return try {
            chatClient.prompt(prompt).call().content()
        } catch (e: Exception) {
            "Unable to generate advice at this time."
        }
    }

    suspend fun finalOutput(validation: String?): String? {
        val promptText = """ 
            You are a helpful chess coach. I will give you a validator's response that reviews a previous move suggestion. The validator output may contain:

A ✅ valid assessment and brief reasoning, or

A ❌ rejection with an explanation of what was wrong and a corrected move with reasoning

Your task is to:

Read the validator output carefully.

Ignore any mention of the original mistake.

Use the corrected move and explanation from the validator output to generate a clean, concise coaching message as if you had suggested the correct move from the beginning.

Your message must:

Start with: Suggested Move: [move]

Follow with a short explanation of why it's a good move, using beginner-friendly language (especially under 1000 ELO)

Avoid technical jargon, evaluation scores, or long move sequences

Use a friendly, confident, human tone — like a coach giving calm advice mid-game

🔒 Rules:

Do not mention the original invalid suggestion

Do not include validator meta-language (e.g., "the correct move is..." or "this fixes the issue")

Speak directly to the player, not about the analysis

Input:

$validation
        """.trimIndent()


        val userMessage = UserMessage.builder().text(promptText).build()
        val options = OpenAiChatOptions.builder()
            .model("gpt-4o")
            .build()
        val prompt = Prompt(userMessage, options)

        return try {
            chatClient.prompt(prompt).call().content()
        } catch (e: Exception) {
            "Unable to generate advice at this time."
        }
    }

    /**
     * Extract a clean FEN string from text that might contain extra information
     */
    private fun extractFenString(text: String): String? {
        // Common FEN pattern: 8 segments separated by slashes, followed by w/b, etc.
        val fenRegex = """[rnbqkpRNBQKP1-8]{1,8}/[rnbqkpRNBQKP1-8]{1,8}/[rnbqkpRNBQKP1-8]{1,8}/[rnbqkpRNBQKP1-8]{1,8}/[rnbqkpRNBQKP1-8]{1,8}/[rnbqkpRNBQKP1-8]{1,8}/[rnbqkpRNBQKP1-8]{1,8}/[rnbqkpRNBQKP1-8]{1,8} [wb] [KQkq-]+ (?:[a-h][36]|-) \d+ \d+""".toRegex()
        val match = fenRegex.find(text)
        return match?.value
    }
    
    /**
     * Generate move advice using both AI and engine analysis
     */
    suspend fun nextMoveAdviceWithEngine(userInfo: String?, fen: String?, engineAnalysis: ChessApiResponse): String? {
        val promptText = """ 
            You are a real-time chess coach helping a player during their game. I will provide:
            
            - The player's information (ELO rating and color)
            - The FEN (Forsyth-Edwards Notation) for the current position
            - Engine analysis of the position
            
            Engine Analysis:
            - Evaluation: ${engineAnalysis.eval} (negative means Black is winning)
            - Best Move: ${engineAnalysis.san ?: engineAnalysis.move}
            - Depth: ${engineAnalysis.depth}
            - Win Chance: ${engineAnalysis.winChance}%
            ${if (engineAnalysis.mate != null) "- Mate in: ${engineAnalysis.mate}" else ""}
            
            Your job is to:
            
            1. Extract the player's ELO and color from the input text.
            2. Based on the player's skill level and the engine analysis, suggest the appropriate move.
            3. For beginners (under 1000 ELO), focus on basic principles and simple tactical opportunities.
            4. For intermediate players (1000-1800), provide more nuanced advice that includes positional considerations.
            5. For advanced players (1800+), you can include more sophisticated concepts.
            
            Write a short explanation of the move in a clear, friendly, coaching voice, suitable for the player's ELO.
            
            Do not directly state the engine evaluation in raw numerical terms. Instead, translate it into concepts that are appropriate for the player's level.
            
            Input:
            ELO: $userInfo
            FEN: $fen
        """.trimIndent()

        val userMessage = UserMessage.builder().text(promptText).build()
        val options = OpenAiChatOptions.builder()
            .model("gpt-4o")
            .build()
        val prompt = Prompt(userMessage, options)

        return try {
            chatClient.prompt(prompt).call().content()
        } catch (e: Exception) {
            "Unable to generate advice at this time."
        }
    }

    /**
     * Generate coaching advice specifically for very low ELO players (under 600)
     */
    private fun generateBeginnerSpecificAdvice(userInfo: UserInfo, fenAnalysis: FenAnalysis): CoachingAdvice {
        // Detect game phase based on move number and remaining pieces
        val moveNumber = fenAnalysis.moveNumber ?: 1
        val fen = fenAnalysis.fen
        
        // Count pieces on the board to determine game phase
        val pieceCount = fen.split(" ")[0].count { it.isLetter() }
        
        return when {
            userInfo.elo != null && userInfo.elo < 600 -> {
                val (suggestedMove, explanation, reasoning) = when {
                    // Opening phase (first 10 moves, many pieces on board)
                    moveNumber <= 10 && pieceCount > 24 -> {
                        if (fenAnalysis.fen.contains("4p3/3P4")) {
                            Triple(
                                "Nf3",
                                """Great job opening with d4! That's the Queen's pawn opening - a solid choice. 
                                
Your opponent responded with e5, which is unusual but playable. Now you should focus on developing your pieces. 

Try Nf3 next! This develops your knight and controls important central squares. Remember the opening principles:
1. Control the center (you're doing this with d4!)
2. Develop your pieces (knights before bishops)
3. Get your king to safety (castle early)""",
                                "Developing pieces is more important than trying to win material at your level"
                            )
                        } else if (fenAnalysis.fen.contains("3p4/3P4")) {
                            Triple(
                                "c4",
                                """Excellent! You've played **d4 d5**, and now you can play the famous **Queen's Gambit** with c4!
                                
The Queen's Gambit is one of the oldest and most respected openings in chess. By playing c4, you're:
- Offering a pawn to gain rapid development
- Fighting for control of the center
- Following in the footsteps of world champions!

This opening has been played by legends like Garry Kasparov and Magnus Carlsen. Don't worry if Black takes the pawn - you'll get great piece development in return!""",
                                "The Queen's Gambit is about rapid development and central control"
                            )
                        } else {
                            Triple(
                                "Develop pieces",
                                """Focus on the basic opening principles:
1. Control the center with pawns (e4, d4)
2. Develop knights before bishops
3. Castle early to keep your king safe
4. Don't move the same piece twice in the opening""",
                                "Basic opening principles are most important"
                            )
                        }
                    }
                    
                    // Middlegame (moves 10-25, moderate pieces)
                    moveNumber in 11..25 && pieceCount > 12 -> {
                        Triple(
                            "Look for tactics",
                            """You're in the middlegame now! At your level, focus on:

1. **Look for hanging pieces** - Can you capture something for free?
2. **Keep your pieces safe** - Make sure your opponent can't capture your pieces
3. **Look for simple tactics** - Forks, pins, and skewers
4. **Improve your worst piece** - Move pieces to better squares

Don't worry about complex strategy yet - just focus on not losing material and finding simple tactics!""",
                            "Tactical awareness and piece safety are key in the middlegame"
                        )
                    }
                    
                    // Endgame (late moves, few pieces)
                    moveNumber > 25 || pieceCount <= 12 -> {
                        Triple(
                            "Push passed pawns",
                            """You're in the endgame! This is where games are won and lost. Key endgame principles for beginners:

1. **King activity** - Bring your king into the fight! Kings are strong in the endgame
2. **Push passed pawns** - Pawns that can't be stopped by enemy pawns are very powerful
3. **Stop opponent's pawns** - Don't let their pawns promote to queens
4. **Use your remaining pieces actively** - Every piece matters in the endgame

The endgame is all about pawn promotion - try to get your pawns to the other side of the board!""",
                            "Endgames are about kings, pawns, and activity"
                        )
                    }
                    
                    else -> {
                        Triple(
                            "Play carefully",
                            "Focus on not losing material and look for simple tactics. Keep your pieces safe and active.",
                            "Safety first, then look for opportunities"
                        )
                    }
                }
                
                CoachingAdvice(
                    suggestedMove = suggestedMove,
                    explanation = explanation,
                    reasoning = reasoning,
                    difficulty = "beginner",
                    confidence = "high"
                )
            }
            else -> {
                CoachingAdvice(
                    suggestedMove = "Develop pieces",
                    explanation = "Focus on developing your pieces and controlling the center",
                    reasoning = "Basic chess principles are most important",
                    difficulty = "beginner", 
                    confidence = "medium"
                )
            }
        }
    }
} 