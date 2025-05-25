package com.vibechess.api.model.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

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