package com.vibechess.api.ai.client

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for AI clients and chat models
 */
@Configuration
class AIClientConfiguration {
    
    /**
     * Primary chat client for general AI interactions
     */
    @Bean
    fun primaryChatClient(builder: ChatClient.Builder): ChatClient {
        return builder
            .defaultOptions(
                OpenAiChatOptions.builder()
                    .model("gpt-4o")
                    .temperature(0.3)
                    .build()
            )
            .build()
    }
    
    /**
     * Vision-specific chat client for image analysis
     */
    @Bean
    fun visionChatClient(builder: ChatClient.Builder): ChatClient {
        return builder
            .defaultOptions(
                OpenAiChatOptions.builder()
                    .model("gpt-4o")
                    .temperature(0.1) // Lower temperature for more consistent vision analysis
                    .build()
            )
            .build()
    }
} 