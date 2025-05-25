package com.vibechess.api.model.response

/**
 * Response model for chess coaching API
 * Includes success/error handling for explicit error reporting
 */
data class CoachingResponse(
    val success: Boolean,           // Whether the request was successful
    val bestMove: String?,          // Recommended move in algebraic notation
    val advice: String?,            // Comprehensive coaching explanation
    val error: String? = null       // Error message if request failed
) 