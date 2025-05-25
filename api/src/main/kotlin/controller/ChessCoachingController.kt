package com.vibechess.api.controller

import com.vibechess.api.service.ChessCoachingService
import com.vibechess.api.service.ChessApiTool
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

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

    @GetMapping("/test")
    fun test(): String {
        return "This is a test"
    }
}

data class CoachingResponse(
    val advice: String
)

