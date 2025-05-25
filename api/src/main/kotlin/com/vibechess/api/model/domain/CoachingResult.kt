package com.vibechess.api.model.domain

/**
 * Structured coaching result from AI analysis
 */
data class CoachingResult(
    val bestMove: String?, // Recommended move in algebraic notation
    val advice: String     // Comprehensive coaching explanation
) 