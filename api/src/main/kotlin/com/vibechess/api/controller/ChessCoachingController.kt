package com.vibechess.api.controller

import com.vibechess.api.service.ChessCoachingService
import com.vibechess.api.ai.tools.ChessApiTool
import com.vibechess.api.ai.agent.orchestrator.ChessCoachingException
import com.vibechess.api.ai.agent.task.CoachingAdviceException
import com.vibechess.api.model.response.CoachingResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MaxUploadSizeExceededException

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
    ): ResponseEntity<CoachingResponse> {
        println("File size: ${screenshot.size} bytes")
        println("File name: ${screenshot.originalFilename}")
        println("Content type: ${screenshot.contentType}")
        println("Moves: $moves")

        return try {
            val result = chessCoachingService.analyzePosition(screenshot, moves)
            ResponseEntity.ok(
                CoachingResponse(
                    success = true,
                    bestMove = result.bestMove,
                    advice = result.advice,
                    error = null
                )
            )
        } catch (e: Exception) {
            handleException(e)
        }
    }

    @GetMapping("/test")
    fun test(): String {
        return "This is a test"
    }

    /**
     * Handle exceptions and convert them to appropriate HTTP responses
     */
    private fun handleException(e: Exception): ResponseEntity<CoachingResponse> {
        return when (e) {
            is ChessCoachingException -> {
                println("Chess coaching failed: ${e.message}")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CoachingResponse(
                        success = false,
                        bestMove = null,
                        advice = null,
                        error = "Chess coaching analysis failed: ${e.message}"
                    )
                )
            }
            is CoachingAdviceException -> {
                println("Coaching advice generation failed: ${e.message}")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CoachingResponse(
                        success = false,
                        bestMove = null,
                        advice = null,
                        error = "Failed to generate coaching advice: ${e.message}"
                    )
                )
            }
            is MaxUploadSizeExceededException -> {
                println("File too large: ${e.message}")
                ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
                    CoachingResponse(
                        success = false,
                        bestMove = null,
                        advice = null,
                        error = "Screenshot file is too large. Please use a smaller image."
                    )
                )
            }
            is IllegalArgumentException -> {
                println("Invalid input: ${e.message}")
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CoachingResponse(
                        success = false,
                        bestMove = null,
                        advice = null,
                        error = "Invalid input: ${e.message}"
                    )
                )
            }
            else -> {
                println("Unexpected error: ${e.message}")
                e.printStackTrace()
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CoachingResponse(
                        success = false,
                        bestMove = null,
                        advice = null,
                        error = "An unexpected error occurred. Please try again later."
                    )
                )
            }
        }
    }
} 