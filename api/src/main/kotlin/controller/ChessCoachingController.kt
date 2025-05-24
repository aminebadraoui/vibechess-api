package com.vibechess.api.controller

import com.vibechess.api.service.ChessCoachingService
import com.vibechess.api.service.ChessApiTool
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

@RestController
@RequestMapping("/api")
class ChessCoachingController(
    private val chessCoachingService: ChessCoachingService,
    private val chessApiTool: ChessApiTool
) {
    @CrossOrigin(origins = ["https://www.chess.com", "https://chess.com"])
    @PostMapping("/coach")
    suspend fun getCoaching(
        @RequestPart("screenshot") screenshot: MultipartFile,
        @RequestPart("moves") moves: String
    ): CoachingResponse {
        println("File size: ${screenshot.size} bytes")
        println("File name: ${screenshot.originalFilename}")
        println("Content type: ${screenshot.contentType}")
        println("Moves: $moves")

        val advice = chessCoachingService.analyzePosition(screenshot, moves) ?: "No response from AI"
        return CoachingResponse(advice = advice)
    }

    @CrossOrigin(origins = ["*"])
    @PostMapping("/analyze")
    fun analyzePosition(@RequestBody request: AnalyzeRequest): AnalyzeResponse {
        try {
            val result = chessApiTool.getAnalysisResponse(
                fen = request.fen,
                depth = request.depth ?: 12,
                variants = request.variants ?: 1,
                maxThinkingTime = request.maxThinkingTime ?: 50
            )
            
            return if (result != null) {
                AnalyzeResponse(
                    success = true,
                    eval = result.eval,
                    bestMove = result.move,
                    san = result.san ?: "",
                    text = result.text,
                    winChance = result.winChance,
                    depth = result.depth,
                    mate = result.mate,
                    continuation = result.continuationArr ?: emptyList()
                )
            } else {
                AnalyzeResponse(success = false, error = "Failed to analyze position")
            }
        } catch (e: Exception) {
            return AnalyzeResponse(success = false, error = "Error: ${e.message}")
        }
    }

    @CrossOrigin(origins = ["*"])
    @GetMapping("/analyze-with-tool")
    fun analyzeWithTool(@RequestParam fen: String, @RequestParam(required = false) depth: Int?): Map<String, Any> {
        val analysisText = chessApiTool.analyzePosition(
            fen = fen,
            depth = depth ?: 12
        )
        return mapOf("analysis" to analysisText)
    }

    @CrossOrigin(origins = ["*"])
    @PostMapping("/analyze-moves")
    fun analyzeFromMoves(@RequestBody request: MovesAnalyzeRequest): Map<String, Any?> {
        return try {
            val result = chessApiTool.getAnalysisResponseFromMoves(
                moves = request.moves,
                depth = request.depth ?: 12,
                variants = request.variants ?: 1,
                maxThinkingTime = request.maxThinkingTime ?: 50
            )
            
            if (result != null) {
                hashMapOf<String, Any?>(
                    "success" to true,
                    "eval" to result.eval,
                    "bestMove" to result.move,
                    "san" to result.san,
                    "text" to result.text,
                    "winChance" to result.winChance,
                    "depth" to result.depth,
                    "mate" to result.mate,
                    "fen" to result.fen,
                    "turn" to result.turn,
                    "continuation" to result.continuationArr
                )
            } else {
                hashMapOf<String, Any?>("success" to false, "error" to "Failed to analyze moves")
            }
        } catch (e: Exception) {
            hashMapOf<String, Any?>("success" to false, "error" to "Error: ${e.message}")
        }
    }

    @GetMapping("/test")
    fun test(): String {
        return "This is a test"
    }
}

data class CoachingResponse(
    val advice: String
)

data class AnalyzeRequest(
    val fen: String,
    val depth: Int? = null,
    val variants: Int? = null,
    val maxThinkingTime: Int? = null
)

data class MovesAnalyzeRequest(
    val moves: String,
    val depth: Int? = null,
    val variants: Int? = null,
    val maxThinkingTime: Int? = null
)

data class AnalyzeResponse(
    val success: Boolean,
    val eval: Double? = null,
    val bestMove: String? = null,
    val san: String? = null,
    val text: String? = null,
    val winChance: Double? = null,
    val depth: Int? = null,
    val mate: Int? = null,
    val continuation: List<String> = emptyList(),
    val error: String? = null
) 