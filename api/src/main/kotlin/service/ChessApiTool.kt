package com.vibechess.api.service

import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.BodyInserters
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Spring AI Tool for interacting with chess-api.com
 */
@Component
class ChessApiTool {
    private val webClient = WebClient.builder()
        .baseUrl("https://chess-api.com/v1")
        .build()
    
    private val objectMapper = jacksonObjectMapper()
    
    /**
     * Analyze a chess position using the chess-api.com service
     * 
     * @param fen FEN notation of the position to analyze
     * @param depth Analysis depth (max: 18)
     * @param variants Number of variants to consider (max: 5)
     * @param maxThinkingTime Maximum thinking time in milliseconds (max: 100)
     * @return Analysis result in text format with evaluation, best move, and win chance
     */
    @Tool(description = "Analyze a chess position using Stockfish engine via chess-api.com")
    fun analyzePosition(
        @ToolParam(description = "FEN notation of the chess position") 
        fen: String,
        
        @ToolParam(description = "Analysis depth (max: 18, default: 12)")
        depth: Int = 12,
        
        @ToolParam(description = "Number of variants to consider (max: 5, default: 1)")
        variants: Int = 1,
        
        @ToolParam(description = "Maximum thinking time in ms (max: 100, default: 50)")
        maxThinkingTime: Int = 50
    ): String {
        val requestBody = mapOf(
            "fen" to fen,
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
            ${if (response.mate != null) "Mate in: ${response.mate}" else ""}
            """.trimIndent()
        } catch (e: Exception) {
            "Error analyzing position: ${e.message}"
        }
    }
    
    /**
     * Get analysis response using moves directly (bypasses FEN generation)
     */
    fun getAnalysisResponseFromMoves(moves: String, depth: Int = 12, variants: Int = 1, maxThinkingTime: Int = 50): ChessApiResponse? {
        return try {
            val requestBody = mapOf(
                "input" to moves,
                "depth" to depth,
                "variants" to variants,
                "maxThinkingTime" to maxThinkingTime
            )

            val response = webClient.post()
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(String::class.java)
                .block()

            response?.let { 
                try {
                    objectMapper.readValue<ChessApiResponse>(it)
                } catch (e: Exception) {
                    println("Error parsing chess API response: ${e.message}")
                    println("Raw response: $it")
                    
                    // Try to extract basic info from the response text if JSON parsing fails
                    parseTextResponse(it)
                }
            }
        } catch (e: Exception) {
            println("Error calling chess-api.com: ${e.message}")
            null
        }
    }

    /**
     * Get analysis response with better error handling (FEN-based - kept for compatibility)
     */
    fun getAnalysisResponse(fen: String, depth: Int = 12, variants: Int = 1, maxThinkingTime: Int = 50): ChessApiResponse? {
        return try {
            val requestBody = mapOf(
                "fen" to fen,
                "depth" to depth,
                "variants" to variants,
                "maxThinkingTime" to maxThinkingTime
            )

            val response = webClient.post()
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(String::class.java)
                .block()

            response?.let { 
                try {
                    objectMapper.readValue<ChessApiResponse>(it)
                } catch (e: Exception) {
                    println("Error parsing chess API response: ${e.message}")
                    println("Raw response: $it")
                    
                    // Try to extract basic info from the response text if JSON parsing fails
                    parseTextResponse(it)
                }
            }
        } catch (e: Exception) {
            println("Error calling chess-api.com: ${e.message}")
            null
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

@JsonIgnoreProperties(ignoreUnknown = true)
data class ChessApiResponse(
    val text: String,
    val eval: Double,
    val move: String?,
    val fen: String? = null,
    val depth: Int?,
    val winChance: Double?,
    val continuationArr: List<String>? = null,
    val mate: Int? = null,
    val centipawns: String? = null,
    val san: String? = null,
    val lan: String? = null,
    val turn: String? = null,
    val color: String? = null,
    val piece: String? = null,
    val flags: String? = null,
    val isCapture: Boolean? = null,
    val isCastling: Boolean? = null,
    val isPromotion: Boolean? = null,
    val from: String? = null,
    val to: String? = null,
    val fromNumeric: String? = null,
    val toNumeric: String? = null,
    val taskId: String? = null,
    val time: Int? = null,
    val type: String? = null
) 