package com.vibechess.api.ai.tools

import com.vibechess.api.model.domain.ChessApiResponse
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.BodyInserters
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Spring AI Tool for interacting with chess-api.com
 */
class ChessApiTool {
    private val webClient = WebClient.builder()
        .baseUrl("https://chess-api.com/v1")
        .build()
    
    private val objectMapper = jacksonObjectMapper()
    
    /**
     * Analyze a chess position from a sequence of moves
     * 
     * @param moves Sequence of moves in standard algebraic notation (e.g., "e4 e5 Nf3")
     * @param depth Analysis depth (max: 18)
     * @param variants Number of variants to consider (max: 5)
     * @param maxThinkingTime Maximum thinking time in milliseconds (max: 100)
     * @return Analysis result in text format with evaluation, best move, and win chance
     */
    @Tool(description = "Analyze a chess position from a sequence of moves using Stockfish engine")
    fun analyzeFromMoves(
        @ToolParam(description = "Sequence of moves in standard algebraic notation (e.g., 'e4 e5 Nf3')") 
        moves: String,
        
        @ToolParam(description = "Analysis depth (max: 18, default: 12)")
        depth: Int = 12,
        
        @ToolParam(description = "Number of variants to consider (max: 5, default: 1)")
        variants: Int = 1,
        
        @ToolParam(description = "Maximum thinking time in ms (max: 100, default: 50)")
        maxThinkingTime: Int = 50
    ): String {
        val requestBody = mapOf(
            "input" to moves,
            "depth" to depth,
            "variants" to variants,
            "maxThinkingTime" to maxThinkingTime
        )
        
        return try {
            val responseString = webClient.post()
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(String::class.java)
                .block() ?: throw Exception("Empty response from chess-api.com")
                
            val response = objectMapper.readValue<ChessApiResponse>(responseString)
            
            """
            Analysis Result:
            Evaluation: ${response.eval} (negative means Black is winning)
            Best Move: ${response.san ?: response.move}
            Move Description: ${response.text}
            Depth: ${response.depth}
            Win Chance: ${response.winChance}%
            Position FEN: ${response.fen}
            Turn: ${if (response.turn == "w") "White" else "Black"} to move
            ${if (response.mate != null) "Mate in: ${response.mate}" else ""}
            """.trimIndent()
        } catch (e: Exception) {
            "Error analyzing position from moves: ${e.message}"
        }
    }
    
    /**
     * Fallback parser for when JSON parsing fails
     */
    private fun parseTextResponse(responseText: String): ChessApiResponse? {
        return try {
            // Look for key patterns in the response text
            val movePattern = """(best move|move):\s*([a-h][1-8][a-h][1-8]|[NBRQK]?[a-h]?[1-8]?x?[a-h][1-8])""".toRegex(RegexOption.IGNORE_CASE)
            val evalPattern = """(eval|evaluation):\s*([-+]?\d*\.?\d+)""".toRegex(RegexOption.IGNORE_CASE)
            
            val moveMatch = movePattern.find(responseText)
            val evalMatch = evalPattern.find(responseText)
            
            if (moveMatch != null || evalMatch != null) {
                ChessApiResponse(
                    text = responseText,
                    eval = evalMatch?.groupValues?.get(2)?.toDoubleOrNull() ?: 0.0,
                    move = moveMatch?.groupValues?.get(2),
                    san = moveMatch?.groupValues?.get(2),
                    depth = null,
                    winChance = null,
                    continuationArr = null,
                    mate = null
                )
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error in fallback parsing: ${e.message}")
            null
        }
    }
} 