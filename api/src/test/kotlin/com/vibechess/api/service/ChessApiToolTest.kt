package com.vibechess.api.service

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.ai.chat.client.ChatClient

import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatOptions
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
class ChessApiToolTest {

    @Autowired
    private lateinit var chessApiTool: ChessApiTool
    
    @Autowired
    private lateinit var chatClientBuilder: ChatClient.Builder

    @Test
    @Disabled("This test makes an actual API call to an external service, enable only when needed")
    fun `test analyze position via tool method`() {
        // Starting position FEN
        val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        
        val result = chessApiTool.analyzePosition(fen)
        
        println("Analysis result: $result")
        assertNotNull(result)
        assertTrue(result.contains("Best Move"))
    }
    
    @Test
    @Disabled("This test makes actual API calls to external services, enable only when needed")
    fun `test analyze position with AI model integration`() = runBlocking {
        // Sicilian Defense FEN
        val fen = "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2"
        
        // Create a chat client with the tool
        val clientWithTool = chatClientBuilder
            .defaultTools(chessApiTool)
            .build()
        
        val prompt = """
            I'm playing chess with the following position in FEN notation:
            $fen
            
            Can you analyze this position and suggest the best move for White?
        """.trimIndent()
        
        val userMessage = UserMessage.builder().text(prompt).build()
        val options = OpenAiChatOptions.builder()
            .model("gpt-4o")
            .build()
        
        val response = clientWithTool.prompt(Prompt(userMessage, options)).call().content()
        
        println("AI response with tool: $response")
        assertNotNull(response)
    }
} 