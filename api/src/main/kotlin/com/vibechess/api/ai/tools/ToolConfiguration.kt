package com.vibechess.api.ai.tools

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ToolConfiguration {
    
    @Bean
    fun chessApiTool(): ChessApiTool {
        return ChessApiTool()
    }
} 