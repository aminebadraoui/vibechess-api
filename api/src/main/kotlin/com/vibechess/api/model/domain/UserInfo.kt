package com.vibechess.api.model.domain

/**
 * User information extracted from chess screenshot analysis
 */
data class UserInfo(
    val color: String?, // "White", "Black", or "Uncertain"
    val elo: Int?, // null if uncertain
    val colorConfidence: String? = null, // "High", "Medium", "Low"
    val eloConfidence: String? = null // "High", "Medium", "Low"
) 