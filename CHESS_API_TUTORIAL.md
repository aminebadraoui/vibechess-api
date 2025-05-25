# 🚀 **Complete Spring Boot + Kotlin + Spring AI Tutorial**
## *Building a Chess Coaching API from Scratch*

---

## 📚 **Table of Contents**

1. [Introduction & Technology Stack](#introduction--technology-stack)
2. [Spring Boot Fundamentals](#spring-boot-fundamentals)
3. [Kotlin with Spring](#kotlin-with-spring)
4. [Spring AI Deep Dive](#spring-ai-deep-dive)
5. [Building the Chess API](#building-the-chess-api)
6. [Advanced Spring Features](#advanced-spring-features)
7. [Testing Strategies](#testing-strategies)
8. [Production Deployment](#production-deployment)
9. [Best Practices](#best-practices)
10. [Troubleshooting Guide](#troubleshooting-guide)

---

## 🎯 **Introduction & Technology Stack**

This tutorial teaches **Spring Boot**, **Kotlin**, and **Spring AI** by building a real-world chess coaching API that:
- Analyzes chess positions from screenshots
- Provides AI-powered coaching advice
- Integrates with chess engines
- Adapts responses based on player skill level

### **What You'll Learn**

**Spring Boot:**
- Auto-configuration and starters
- REST API development
- Dependency injection
- Configuration management
- File upload handling

**Kotlin:**
- Kotlin syntax with Spring
- Data classes and coroutines
- Null safety in Spring context
- Extension functions for Spring

**Spring AI:**
- OpenAI integration
- Structured output with AI
- Function calling (tools)
- Prompt engineering
- Vision AI capabilities

### **Technology Stack**
```yaml
Framework: Spring Boot 3.4.5
Language: Kotlin 1.9+
AI: Spring AI with OpenAI GPT-4
Build Tool: Gradle with Kotlin DSL
JVM: Java 17+
Testing: JUnit 5, Spring Boot Test
```

---

## 🔰 **Spring Framework Fundamentals (For Complete Beginners)**

### **What is Spring Framework?**

Think of **Spring** as a powerful toolkit that helps you build Java/Kotlin applications without writing tons of repetitive code. It's like having a smart assistant that:

- **Creates and manages objects** for you (no more `new MyClass()` everywhere)
- **Connects different parts** of your application automatically
- **Provides ready-made solutions** for common tasks (web APIs, databases, security)
- **Follows best practices** automatically

### **Understanding Spring Architecture**

Spring applications follow a **layered architecture** pattern:

```
┌─────────────────────────────────────────────────────┐
│                  🌐 WEB LAYER                        │
│              (Controllers, REST APIs)               │
│  @RestController, @RequestMapping, @PostMapping     │
└─────────────────────────────────────────────────────┘
                            ↕️
┌─────────────────────────────────────────────────────┐
│                 ⚙️ BUSINESS LAYER                    │
│              (Services, Business Logic)             │
│     @Service, @Component, your main application     │
│                    logic lives here                 │
└─────────────────────────────────────────────────────┘
                            ↕️
┌─────────────────────────────────────────────────────┐
│                 💾 DATA LAYER                       │
│          (Repositories, External APIs)              │
│    @Repository, database access, API calls          │
└─────────────────────────────────────────────────────┘
```

**In our chess API:**
- **Web Layer**: `ChessCoachingController` (handles HTTP requests)
- **Business Layer**: `ChessCoachingService` (analyzes positions, generates advice)
- **Data Layer**: `ChessApiTool` (calls external chess engines)

### **What is a Spring Component?**

A **Component** is simply a class that Spring manages for you. Instead of you creating objects manually, Spring creates them and keeps track of them.

```kotlin
// ❌ Without Spring - You manage everything manually
class ChessGame {
    fun startGame() {
        val engine = ChessEngine()        // You create objects
        val analyzer = Analyzer()         // You create more objects
        val coach = Coach(engine, analyzer) // You wire them together
        // ... lots of manual work
    }
}

// ✅ With Spring - Spring manages everything
@Component
class ChessCoachingService {
    // Spring automatically creates this object
    // Spring automatically injects dependencies
    fun analyzePosition() {
        // Your business logic here
    }
}
```

**Common Component Types:**
- `@Component` - Generic component (any Spring-managed class)
- `@Service` - Business logic layer
- `@Repository` - Data access layer  
- `@Controller` / `@RestController` - Web layer
- `@Configuration` - Configuration classes

### **What is a Spring Bean?**

A **Bean** is just a fancy name for "an object that Spring manages." When you mark a class with `@Component` (or `@Service`, `@Controller`, etc.), Spring:

1. **Creates an instance** of that class
2. **Stores it in memory** (called the "Spring Container")
3. **Injects it wherever needed** (Dependency Injection)
4. **Manages its lifecycle** (creation, destruction)

```kotlin
// This becomes a "Bean" - Spring manages it
@Service
class ChessCoachingService {
    fun analyzePosition() = "Great move!"
}

// Spring automatically provides the bean here
@RestController
class ChessController(
    private val coachingService: ChessCoachingService  // ✨ Magic! Spring injects this
) {
    @PostMapping("/advice")
    fun getAdvice(): String {
        return coachingService.analyzePosition()  // Use the injected service
    }
}
```

### **How Spring Works - The Magic Behind the Scenes**

1. **Component Scanning**: Spring scans your code looking for `@Component` annotations
2. **Bean Creation**: Spring creates instances of all annotated classes
3. **Dependency Injection**: Spring automatically connects objects that need each other
4. **Container Management**: Spring stores everything in its "container" (like a smart object storage)

```kotlin
// Spring does this automatically for you:
// 1. Finds @Service class
@Service
class ChessCoachingService

// 2. Finds @RestController class  
@RestController
class ChessController(
    private val service: ChessCoachingService  // 3. Connects them automatically!
)

// You just write your business logic, Spring handles the plumbing! 🎉
```

### **Dependency Injection - Why It's Amazing**

**Without Dependency Injection** (the old way):
```kotlin
class ChessController {
    private val service = ChessCoachingService()  // Tightly coupled
    
    fun getAdvice(): String {
        return service.analyze()
    }
}
```

**Problems:**
- Hard to test (can't mock the service)
- Hard to change implementations
- Lots of manual object creation

**With Dependency Injection** (the Spring way):
```kotlin
@RestController
class ChessController(
    private val service: ChessCoachingService  // Spring injects this
) {
    fun getAdvice(): String {
        return service.analyze()
    }
}
```

**Benefits:**
- ✅ Easy to test (mock the service)
- ✅ Easy to swap implementations
- ✅ Spring handles all object creation
- ✅ Loose coupling between classes

### **Configuration - How Spring Knows What To Do**

Spring uses **configuration** to understand your application:

```kotlin
// Main application class - tells Spring where to start
@SpringBootApplication
class VibechessApiApplication

fun main(args: Array<String>) {
    runApplication<VibechessApiApplication>(*args)  // Starts Spring
}
```

```yaml
# application.yml - external configuration
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}  # Environment variable
server:
  port: 8080                     # Run on port 8080
```

### **Real Example - How Our Chess API Uses These Concepts**

```kotlin
// 1. SERVICE BEAN - Business logic
@Service  // ← This makes it a Spring component/bean
class ChessCoachingService(
    private val chatClient: ChatClient,     // ← Spring injects this
    private val chessApiTool: ChessApiTool  // ← And this
) {
    suspend fun analyzePosition(screenshot: MultipartFile, moves: String): String? {
        // Business logic for chess coaching
        val userInfo = extractUserInfo(screenshot)
        val analysis = chessApiTool.analyze(moves)
        return generateAdvice(userInfo, analysis)
    }
}

// 2. CONTROLLER BEAN - Web layer
@RestController  // ← This makes it a web component
@RequestMapping("/api")
class ChessCoachingController(
    private val chessCoachingService: ChessCoachingService  // ← Spring injects the service
) {
    @PostMapping("/coach")
    suspend fun getCoaching(
        @RequestPart("screenshot") screenshot: MultipartFile,
        @RequestPart("moves") moves: String
    ): CoachingResponse {
        // Delegate to service layer
        val advice = chessCoachingService.analyzePosition(screenshot, moves)
        return CoachingResponse(advice = advice ?: "No advice available")
    }
}

// 3. TOOL BEAN - External integration
@Component  // ← This makes it a Spring component
class ChessApiTool {
    @Description("Analyze chess moves")  // ← Spring AI annotation
    fun getAnalysisResponseFromMoves(moves: String): ChessApiResponse? {
        // Call external chess engine API
        return callExternalChessEngine(moves)
    }
}
```

**What Spring Does Automatically:**
1. Creates `ChessApiTool` object
2. Creates `ChessCoachingService` object and injects `ChessApiTool` into it
3. Creates `ChessCoachingController` object and injects `ChessCoachingService` into it
4. Sets up web routing so `/api/coach` calls the controller method
5. Handles HTTP request/response conversion
6. Manages object lifecycles

### **Key Benefits for Beginners**

**🎯 Less Code**: No manual object creation and wiring  
**🛡️ Best Practices**: Spring enforces good architecture patterns  
**🧪 Easy Testing**: Dependency injection makes testing simple  
**📦 Rich Ecosystem**: Thousands of ready-made integrations  
**🔧 Configuration**: External configuration without code changes  
**🚀 Rapid Development**: Focus on business logic, not plumbing  

### **Summary - The Spring Way vs Traditional Way**

| **Traditional Java/Kotlin** | **Spring Way** |
|------------------------------|----------------|
| Manual object creation | Automatic bean management |
| Hard-coded dependencies | Dependency injection |
| Scattered configuration | Centralized configuration |
| Lots of boilerplate code | Annotation-driven |
| Manual lifecycle management | Container-managed |
| Tightly coupled classes | Loosely coupled components |

**🎉 Result**: You write 70% less code and get a more maintainable, testable application!

---

## 🏗️ **Spring Boot Fundamentals**

### **1. Project Structure**

```
vibechess-api/
├── api/
│   ├── build.gradle.kts              # Gradle build script
│   ├── src/main/kotlin/
│   │   ├── VibechessApiApplication.kt # Main application class
│   │   ├── controller/               # REST controllers
│   │   ├── service/                  # Business logic
│   │   └── config/                   # Configuration classes
│   ├── src/main/resources/
│   │   ├── application.yml           # Application configuration
│   │   └── static/                   # Static resources
│   └── src/test/kotlin/              # Test classes
```

### **2. Main Application Class**

```kotlin
// VibechessApiApplication.kt
package com.vibechess.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class VibechessApiApplication

fun main(args: Array<String>) {
    runApplication<VibechessApiApplication>(*args)
}
```

**Key Spring Boot Concepts:**
- `@SpringBootApplication`: Combines `@Configuration`, `@EnableAutoConfiguration`, and `@ComponentScan`
- `runApplication`: Kotlin extension function for starting Spring Boot apps
- Auto-configuration: Spring Boot automatically configures beans based on classpath

### **3. Build Configuration (Gradle with Kotlin DSL)**

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.spring") version "1.9.20"
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.4"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}
```

**Key Dependencies:**
- `spring-boot-starter-web`: Web applications with embedded Tomcat
- `spring-ai-openai-spring-boot-starter`: Spring AI with OpenAI integration
- `jackson-module-kotlin`: JSON serialization for Kotlin data classes
- `kotlinx-coroutines-reactor`: Reactive programming with coroutines

### **4. Application Configuration**

```yaml
# application.yml
server:
  port: 8080

spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
          temperature: 0.3
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

logging:
  level:
    com.vibechess: DEBUG
    org.springframework.ai: DEBUG
```

**Configuration Concepts:**
- External configuration with environment variables
- Profile-specific configuration (dev, prod)
- Property binding to configuration classes
- Spring Boot's configuration hierarchy

---

## 🎯 **Kotlin with Spring**

### **1. Data Classes for DTOs**

```kotlin
// Request/Response DTOs
data class CoachingResponse(
    val advice: String
)

data class AnalyzeRequest(
    val fen: String,
    val depth: Int? = null,        // Nullable with default
    val variants: Int? = null,
    val maxThinkingTime: Int? = null
)

data class UserInfo(
    val color: String?,            // Nullable properties
    val elo: Int?,
    val colorConfidence: String? = null,
    val eloConfidence: String? = null
)
```

**Kotlin Benefits:**
- Concise data class syntax
- Built-in `equals()`, `hashCode()`, `toString()`
- Null safety with `?` operator
- Default parameter values
- Automatic JSON serialization with Jackson

### **2. REST Controllers with Kotlin**

```kotlin
@RestController
@RequestMapping("/api")
class ChessCoachingController(
    private val chessCoachingService: ChessCoachingService,  // Constructor injection
    private val chessApiTool: ChessApiTool
) {
    
    @CrossOrigin(origins = ["*"])
    @PostMapping("/coach")
    suspend fun getCoaching(                              // Suspend function for coroutines
        @RequestPart("screenshot") screenshot: MultipartFile,
        @RequestPart("moves") moves: String
    ): CoachingResponse {
        println("File size: ${screenshot.size} bytes")   // String interpolation
        println("Moves: $moves")

        val advice = chessCoachingService.analyzePosition(screenshot, moves) 
            ?: "No response from AI"                      // Elvis operator
        
        return CoachingResponse(advice = advice)
    }

    @PostMapping("/analyze")
    fun analyzePosition(@RequestBody request: AnalyzeRequest): AnalyzeResponse {
        return try {
            val result = chessApiTool.getAnalysisResponse(
                fen = request.fen,
                depth = request.depth ?: 12,             // Elvis operator with default
                variants = request.variants ?: 1,
                maxThinkingTime = request.maxThinkingTime ?: 50
            )
            
            if (result != null) {                         // Null safety check
                AnalyzeResponse(
                    success = true,
                    eval = result.eval,
                    bestMove = result.move,
                    // ... other properties
                )
            } else {
                AnalyzeResponse(success = false, error = "Failed to analyze position")
            }
        } catch (e: Exception) {                          // Exception handling
            AnalyzeResponse(success = false, error = "Error: ${e.message}")
        }
    }
}
```

**Key Kotlin Features:**
- Constructor injection without `@Autowired`
- Suspend functions for async operations
- Elvis operator (`?:`) for null handling
- String interpolation with `$`
- Smart casting after null checks
- Named parameters for clarity

### **3. Service Layer with Coroutines**

```kotlin
@Service
class ChessCoachingService(
    private val builder: ChatClient.Builder,
    private val chessApiTool: ChessApiTool
) {
    private val chatClient = builder.build()
    private val objectMapper = jacksonObjectMapper()  // Kotlin extension

    suspend fun analyzePosition(
        screenshot: MultipartFile, 
        moves: String
    ): String? {
        // Step 1: Extract user info with structured output
        val userInfo = getUserInfoStructured(screenshot)
        println("User Info: $userInfo")

        // Step 2: Determine appropriate analysis depth based on ELO
        val analysisDepth = determineAnalysisDepth(userInfo.elo)
        
        // Step 3: Get engine analysis
        val engineAnalysis = chessApiTool.getAnalysisResponseFromMoves(
            moves = moves,
            depth = analysisDepth,
            variants = if (userInfo.elo != null && userInfo.elo > 1500) 3 else 1
        )

        // Step 4: Generate coaching advice
        val coachingAdvice = if (engineAnalysis != null) {
            generateStructuredCoachingAdvice(userInfo, engineAnalysis)
        } else {
            generateFallbackAdvice(userInfo)
        }
        
        return formatFinalCoachingMessage(coachingAdvice, userInfo)
    }

    private fun determineAnalysisDepth(elo: Int?): Int {
        return when {                                     // Kotlin when expression
            elo == null -> 12
            elo < 800 -> 8
            elo < 1200 -> 10
            elo < 1600 -> 12
            elo < 2000 -> 15
            else -> 18
        }
    }
}
```

**Kotlin Coroutines Benefits:**
- `suspend` functions for non-blocking operations
- Structured concurrency
- Easy async/await patterns
- Integration with Spring WebFlux

---

## 🤖 **Spring AI Deep Dive**

### **1. Spring AI Configuration**

```kotlin
@Configuration
class AIConfiguration {
    
    @Bean
    fun chatClient(chatClientBuilder: ChatClient.Builder): ChatClient {
        return chatClientBuilder
            .defaultSystem("You are a helpful chess coach")
            .build()
    }
}
```

### **2. Basic Chat Client Usage**

```kotlin
@Service
class ChessCoachingService(
    private val builder: ChatClient.Builder
) {
    private val chatClient = builder.build()

    suspend fun generateCoachingAdvice(moves: String, userInfo: String?): String? {
        val promptText = """
            You are a chess coach. Here is information about the user: $userInfo
            Here are the moves played so far: $moves
            Give advice that is appropriate for the user's ELO and color.
            Respond as a real coach would: be conversational, encouraging, and specific.
        """.trimIndent()

        return try {
            chatClient.prompt()
                .user(promptText)
                .call()
                .content()
        } catch (e: Exception) {
            "Unable to generate advice at this time."
        }
    }
}
```

### **3. Vision AI with Multipart Files**

```kotlin
suspend fun extractUserInfoFromScreenshot(screenshot: MultipartFile): String? {
    val promptText = """
        Analyze the attached chess screenshot. 
        - What is the ELO rating of the user (bottom player)?
        - Is the user playing as white or black?
        Respond in a short, clear sentence.
    """.trimIndent()

    // Convert MultipartFile to Spring AI Media
    val mimeType = MimeTypeUtils.parseMimeType(screenshot.contentType ?: "image/png")
    val resource = ByteArrayResource(screenshot.bytes)
    val media = Media(mimeType, resource)
    
    // Create user message with text and image
    val userMessage = UserMessage.builder()
        .text(promptText)
        .media(listOf(media))
        .build()
    
    val options = OpenAiChatOptions.builder()
        .model("gpt-4o")
        .build()
    
    val prompt = Prompt(userMessage, options)

    return try {
        chatClient.prompt(prompt).call().content()
    } catch (e: Exception) {
        null
    }
}
```

**Key Spring AI Concepts:**
- `ChatClient.Builder` for configuration
- `Media` objects for images/files
- `UserMessage` and `Prompt` construction
- Model-specific options

### **4. Structured Output with Spring AI**

```kotlin
// Define output structure
data class UserInfo(
    val color: String?,
    val elo: Int?,
    val colorConfidence: String? = null,
    val eloConfidence: String? = null
)

suspend fun getUserInfoStructured(screenshot: MultipartFile): UserInfo {
    val promptText = """
        You are a vision system specialized in parsing Chess.com interfaces.
        Analyze the screenshot and extract user information.
        
        Rules:
        - Determine user color from board orientation
        - Extract ELO rating from username area
        - If unclear, set to null
        - Assess confidence levels
    """.trimIndent()

    val mimeType = MimeTypeUtils.parseMimeType(screenshot.contentType ?: "image/png")
    val resource = ByteArrayResource(screenshot.bytes)
    val media = Media(mimeType, resource)
    
    val options = OpenAiChatOptions.builder()
        .model("gpt-4o")
        .temperature(0.1)
        .build()

    return try {
        // Spring AI's structured output capability
        chatClient.prompt()
            .user { userMessageBuilder ->
                userMessageBuilder.text(promptText).media(media)
            }
            .options(options)
            .call()
            .entity(UserInfo::class.java)  // Automatic JSON to Kotlin data class
        
    } catch (e: Exception) {
        println("Error parsing user info: ${e.message}")
        UserInfo(color = "Uncertain", elo = null)
    } ?: UserInfo(color = "Uncertain", elo = null)
}
```

**Structured Output Benefits:**
- Type-safe AI responses
- Automatic JSON deserialization
- Validation and error handling
- No manual parsing required

### **5. Function Calling (Tools) with Spring AI**

Spring AI supports two main approaches for defining tools:

#### **Approach 1: Simple Tools (Direct Instantiation)**

For simple, stateless tools that are used directly with `ChatClient`:

```kotlin
class ChessAnalysisTools {
    
    @Tool(description = "Analyze a chess position given a FEN string")
    fun analyzePosition(
        @Description("FEN notation of the chess position") fen: String,
        @Description("Analysis depth (default: 12)") depth: Int = 12
    ): String {
        // Simple analysis logic
        return "Position analysis: evaluation = 0.5, best move = e4"
    }
    
    @Tool(description = "Get opening name from moves")
    fun getOpeningName(
        @Description("Space-separated list of moves") moves: String
    ): String {
        // Opening identification logic
        return if (moves.startsWith("e2e4 e7e5")) "Italian Game" else "Unknown opening"
    }
}

// Usage - Direct with ChatClient
val response = chatClient.prompt()
    .user("What's the best move in this position: rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    .tools(ChessAnalysisTools())  // ← Direct instantiation
    .call()
    .content()
```

#### **Approach 2: Spring Components (For Complex Applications)**

For tools that need dependency injection or are used in business logic:

```kotlin
@Component  // ← Spring component for dependency injection
class ChessApiTool {
    
    @Autowired
    private lateinit var restTemplate: RestTemplate  // ← Can inject dependencies
    
    fun getAnalysisResponseFromMoves(
        moves: String,
        depth: Int = 12,
        variants: Int = 1,
        maxThinkingTime: Int = 50
    ): ChessApiResponse? {
        // Complex implementation with external API calls
        return callExternalChessEngine(moves, depth, variants, maxThinkingTime)
    }
    
    private fun callExternalChessEngine(moves: String, depth: Int, variants: Int, maxThinkingTime: Int): ChessApiResponse? {
        // Implementation using injected RestTemplate
        return restTemplate.postForObject("/analyze", request, ChessApiResponse::class.java)
    }
}

// Example 1: Using simple tools directly with ChatClient
@Service
class SimpleChessCoachingService(
    private val builder: ChatClient.Builder
) {
    
    suspend fun getAdviceWithSimpleTools(userInfo: String?, fen: String?): String? {
        val promptText = """
            You are a chess coach helping a player.
            
            Player Info: $userInfo
            Current Position FEN: $fen
            
            Analyze the position and give advice.
        """.trimIndent()

        val chatClient = builder.build()

        return try {
            chatClient.prompt()
                .user(promptText)
                .tools(ChessAnalysisTools())  // ← Direct instantiation (Spring AI approach)
                .call()
                .content()
        } catch (e: Exception) {
            "Unable to generate advice: ${e.message}"
        }
    }
}

// Example 2: Using Spring component tools in business logic
@Service
class ChessCoachingService(
    private val builder: ChatClient.Builder,
    private val chessApiTool: ChessApiTool  // ← Injected Spring component
) {
    
    suspend fun getAdviceWithComplexLogic(userInfo: String?, moves: String): String? {
        // Use tool in business logic first
        val engineAnalysis = chessApiTool.getAnalysisResponseFromMoves(moves)
        
        val promptText = """
            You are a chess coach. Here's the engine analysis: ${engineAnalysis?.text}
            
            Player Info: $userInfo
            Engine evaluation: ${engineAnalysis?.eval}
            Best move: ${engineAnalysis?.san}
            
            Give coaching advice based on this analysis.
        """.trimIndent()

        val chatClient = builder.build()

        return try {
            chatClient.prompt()
                .user(promptText)
                .call()
                .content()
        } catch (e: Exception) {
            "Unable to generate advice: ${e.message}"
        }
    }
}
```

**Key Differences:**

| **Direct Instantiation** | **Spring Component** |
|---------------------------|----------------------|
| `@Tool` annotation | No special annotations needed |
| Used directly with ChatClient | Used in business logic, then results passed to AI |
| Simple, stateless functions | Complex logic with dependencies |
| AI calls tool automatically | Manual tool execution + AI analysis |

**When to Use Each:**
- **Direct Instantiation**: When you want the AI to call tools automatically during conversation
- **Spring Component**: When you need complex business logic, external API integration, or dependency injection

**Function Calling Features:**
- `@Tool` and `@Description` annotations for AI understanding
- Automatic parameter binding
- Type-safe tool definitions
- Integration with external APIs

### **4. Tool Configuration Management**

**Important: Avoiding Tool Registration Conflicts**

When using Spring AI tools, you may encounter "Multiple tools with the same name" errors. This happens when tools are registered multiple times through different mechanisms.

**Problem**: Spring AI uses both automatic discovery (`@Component`) and explicit registration (`.defaultTools()`):

```kotlin
// ❌ This causes duplicate registration
@Component  // ← Spring automatically discovers this
class ChessApiTool {
    @Tool
    fun analyzeFromMoves(...): String { ... }
}

@Service
class ChessCoachingService(builder: ChatClient.Builder, chessApiTool: ChessApiTool) {
    private val clientWithTool = builder
        .defaultTools(chessApiTool)  // ← Explicitly registered again!
        .build()
}
```

**Solution**: Use explicit bean configuration without `@Component`:

```kotlin
// ✅ Configuration class manages tool registration
@Configuration
class ToolConfiguration {
    
    @Bean
    fun chessApiTool(): ChessApiTool {
        return ChessApiTool()
    }
}

// ✅ Tool class without @Component annotation
class ChessApiTool {
    @Tool(description = "Analyze chess position from moves using Stockfish engine")
    fun analyzeFromMoves(
        @ToolParam(description = "Sequence of moves in standard algebraic notation") 
        moves: String,
        @ToolParam(description = "Analysis depth (max: 18, default: 12)")
        depth: Int = 12
    ): String {
        // Tool implementation
    }
}

// ✅ Service uses tool without conflicts
@Service
class ChessCoachingService(
    private val builder: ChatClient.Builder,
    private val chessApiTool: ChessApiTool
) {
    private val clientWithTool = builder
        .defaultTools(chessApiTool)  // Only registration path
        .build()
}
```

**Key Points:**
- Remove `@Component` from tool classes
- Use explicit `@Bean` configuration
- Only register tools through `.defaultTools()` OR automatic discovery, not both
- This ensures consistent tool availability across multiple requests

### **5. Application Configuration Properties**

```kotlin
@ConfigurationProperties(prefix = "chess.coaching")
@ConstructorBinding
data class ChessCoachingProperties(
    val maxAnalysisDepth: Int = 18,
    val defaultDepth: Int = 12,
    val beginnerDepth: Int = 8,
    val advancedDepth: Int = 15,
    val eloThresholds: EloThresholds = EloThresholds()
)

data class EloThresholds(
    val beginner: Int = 800,
    val intermediate: Int = 1200,
    val advanced: Int = 1600,
    val expert: Int = 2000
)

@Service
class ChessCoachingService(
    private val properties: ChessCoachingProperties,
    private val builder: ChatClient.Builder,
    private val chessApiTool: ChessApiTool
) {
    
    private fun determineAnalysisDepth(elo: Int?): Int {
        return when {
            elo == null -> properties.defaultDepth
            elo < properties.eloThresholds.beginner -> properties.beginnerDepth
            elo < properties.eloThresholds.intermediate -> properties.defaultDepth
            elo < properties.eloThresholds.advanced -> properties.defaultDepth
            elo < properties.eloThresholds.expert -> properties.advancedDepth
            else -> properties.maxAnalysisDepth
        }
    }
}
```

---

## ♟️ **Building the Chess API**

### **1. Core Architecture**

```
┌─────────────────────┐
│   ChessCoaching     │  ← REST Layer
│     Controller      │
└─────────────────────┘
           │
           ▼
┌─────────────────────┐
│   ChessCoaching     │  ← Business Logic
│      Service        │
└─────────────────────┘
           │
           ▼
┌─────────────────────┐
│    ChessApiTool     │  ← External Integration
│   (Function Call)   │
└─────────────────────┘
```

### **2. Request Flow**

```kotlin
// 1. Controller receives request and returns structured response
@PostMapping("/coach")
suspend fun getCoaching(
    @RequestPart("screenshot") screenshot: MultipartFile,
    @RequestPart("moves") moves: String
): CoachingResponse {
    val result = chessCoachingService.analyzePosition(screenshot, moves)
    return CoachingResponse(
        bestMove = result.bestMove,
        advice = result.advice
    )
}

// 2. Service uses structured output to get comprehensive coaching
suspend fun analyzePosition(screenshot: MultipartFile, moves: String): CoachingResult {
    // Extract user info from screenshot
    val userInfo = getUserInfoStructured(screenshot)
    
    // Determine analysis depth based on skill
    val analysisDepth = determineAnalysisDepth(userInfo.elo)
    
    // Calculate move number for coaching context
    val moveNumber = extractMoveNumber(moves)
    
    // Use AI with chess analysis tool to get structured coaching result
    val result = clientWithTool.prompt()
        .system(comprehensiveSystemPrompt)
        .user(detailedUserPrompt)
        .call()
        .entity(CoachingResult::class.java)
    
    return result ?: CoachingResult(
        bestMove = null,
        advice = generateBasicAdvice(userInfo, moves, moveNumber)
    )
}

// 3. Tool provides chess engine integration automatically when AI needs it
@Tool(description = "Analyze a chess position from a sequence of moves using Stockfish engine")
fun analyzeFromMoves(
    @ToolParam(description = "Sequence of moves in standard algebraic notation") 
    moves: String,
    @ToolParam(description = "Analysis depth (max: 18, default: 12)")
    depth: Int = 12,
    @ToolParam(description = "Number of variants to consider (max: 5, default: 1)")
    variants: Int = 1,
    @ToolParam(description = "Maximum thinking time in ms (max: 100, default: 50)")
    maxThinkingTime: Int = 50
): String {
    // Convert moves to chess engine analysis
    val requestBody = mapOf(
        "input" to moves,
        "depth" to depth,
        "variants" to variants,
        "maxThinkingTime" to maxThinkingTime
    )
    
    val response = webClient.post()
        .body(BodyInserters.fromValue(requestBody))
        .retrieve()
        .bodyToMono(String::class.java)
        .block()
    
    val chessResponse = objectMapper.readValue<ChessApiResponse>(response)
    
    return """
    Analysis Result:
    Evaluation: ${chessResponse.eval} (negative means Black is winning)
    Best Move: ${chessResponse.san ?: chessResponse.move}
    Move Description: ${chessResponse.text}
    Depth: ${chessResponse.depth}
    Win Chance: ${chessResponse.winChance}%
    Position FEN: ${chessResponse.fen}
    Turn: ${if (chessResponse.turn == "w") "White" else "Black"} to move
    ${if (chessResponse.mate != null) "Mate in: ${chessResponse.mate}" else ""}
    """.trimIndent()
}

// 4. Structured output data models
data class CoachingResult(
    val bestMove: String?,      // Recommended move in algebraic notation
    val advice: String          // Comprehensive coaching explanation
)

data class CoachingResponse(
    val bestMove: String?,
    val advice: String
)
```

### **3. Error Handling Strategy**

```kotlin
suspend fun analyzePosition(screenshot: MultipartFile, moves: String): CoachingResult {
    return try {
        // Main structured output analysis
        val userInfo = getUserInfoStructured(screenshot)
        val analysisDepth = determineAnalysisDepth(userInfo.elo)
        val moveNumber = extractMoveNumber(moves)
        
        // Comprehensive system and user prompts
        val systemPrompt = createComprehensiveSystemPrompt()
        val userPrompt = createDetailedUserPrompt(userInfo, moves, analysisDepth, moveNumber)
        
        // Get structured coaching result from AI with tool integration
        val result = clientWithTool.prompt()
            .system(systemPrompt)
            .user(userPrompt)
            .call()
            .entity(CoachingResult::class.java)
        
        result ?: CoachingResult(
            bestMove = null,
            advice = generateBasicAdvice(userInfo, moves, moveNumber)
        )
        
    } catch (e: OpenAIException) {
        log.error("OpenAI API error: ${e.message}", e)
        CoachingResult(
            bestMove = null,
            advice = generateFallbackAdvice("OpenAI service temporarily unavailable", moves)
        )
        
    } catch (e: JsonProcessingException) {
        log.error("Structured output parsing error: ${e.message}", e)
        CoachingResult(
            bestMove = null,
            advice = generateFallbackAdvice("Response formatting issue", moves)
        )
        
    } catch (e: IllegalArgumentException) {
        log.error("Invalid input: ${e.message}", e)
        CoachingResult(
            bestMove = null,
            advice = "Please check your move notation and try again. Moves should be in standard algebraic notation (e.g., 'e4 e5 Nf3')."
        )
        
    } catch (e: Exception) {
        log.error("Unexpected error: ${e.message}", e)
        CoachingResult(
            bestMove = null,
            advice = generateGenericAdvice(moves)
        )
    }
}

private fun generateFallbackAdvice(reason: String, moves: String): String {
    val moveNumber = extractMoveNumber(moves)
    
    return when {
        moveNumber <= 10 -> """
            I'm having trouble with detailed analysis right now ($reason), 
            but here are key opening principles:
            
            • **Control the center** with pawns (e4, d4) and pieces
            • **Develop knights before bishops** - knights are more flexible early
            • **Castle early** to keep your king safe
            • **Don't move the same piece twice** without good reason
            
            Try again in a few moments for detailed position analysis!
        """.trimIndent()
        
        moveNumber <= 25 -> """
            I'm having trouble with detailed analysis right now ($reason), 
            but here are middlegame principles:
            
            • **Look for tactical opportunities** - forks, pins, skewers
            • **Improve your worst-placed piece** - find better squares
            • **Create threats** while maintaining piece safety
            • **Control key squares** and outposts
            
            Try again in a few moments for detailed position analysis!
        """.trimIndent()
        
        else -> """
            I'm having trouble with detailed analysis right now ($reason), 
            but here are endgame principles:
            
            • **Activate your king** - it's a strong piece in the endgame
            • **Push passed pawns** toward promotion
            • **Stop opponent's dangerous pawns** with your pieces
            • **Use your pieces actively** - centralize and coordinate
            
            Try again in a few moments for detailed position analysis!
        """.trimIndent()
    }
}

private fun generateGenericAdvice(moves: String): String {
    val moveNumber = extractMoveNumber(moves)
    val gamePhase = when {
        moveNumber <= 10 -> "opening"
        moveNumber <= 25 -> "middlegame"
        else -> "endgame"
    }
    
    return """
        I'm experiencing technical difficulties with position analysis, but I can offer some general $gamePhase advice:
        
        Keep focusing on fundamental chess principles, calculate your moves carefully, 
        and look for your opponent's threats. Every position offers learning opportunities!
        
        Please try again shortly for detailed analysis.
    """.trimIndent()
}

### **4. Configuration Management**

```kotlin
@ConfigurationProperties(prefix = "chess.coaching")
@ConstructorBinding
data class ChessCoachingProperties(
    val maxAnalysisDepth: Int = 18,
    val defaultDepth: Int = 12,
    val beginnerDepth: Int = 8,
    val advancedDepth: Int = 15,
    val eloThresholds: EloThresholds = EloThresholds()
)

data class EloThresholds(
    val beginner: Int = 800,
    val intermediate: Int = 1200,
    val advanced: Int = 1600,
    val expert: Int = 2000
)

@Service
class ChessCoachingService(
    private val properties: ChessCoachingProperties,
    private val builder: ChatClient.Builder
) {
    
    private fun determineAnalysisDepth(elo: Int?): Int {
        return when {
            elo == null -> properties.defaultDepth
            elo < properties.eloThresholds.beginner -> properties.beginnerDepth
            elo < properties.eloThresholds.intermediate -> properties.defaultDepth
            elo < properties.eloThresholds.advanced -> properties.defaultDepth
            elo < properties.eloThresholds.expert -> properties.advancedDepth
            else -> properties.maxAnalysisDepth
        }
    }
}
```

---

## 🔧 **Advanced Spring Features**

### **1. Async Processing with @Async**

```kotlin
@Configuration
@EnableAsync
class AsyncConfiguration {
    
    @Bean
    fun taskExecutor(): TaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 5
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("chess-coaching-")
        executor.initialize()
        return executor
    }
}

@Service
class ChessCoachingService {
    
    @Async
    fun generateAdviceAsync(
        screenshot: MultipartFile, 
        moves: String
    ): CompletableFuture<CoachingAdvice> {
        val advice = analyzePosition(screenshot, moves)
        return CompletableFuture.completedFuture(advice)
    }
}
```

### **2. Caching with Spring Cache**

```kotlin
@Configuration
@EnableCaching
class CacheConfiguration {
    
    @Bean
    fun cacheManager(): CacheManager {
        return ConcurrentMapCacheManager("positions", "openings", "userInfo")
    }
}

@Service
class ChessCoachingService {
    
    @Cacheable(value = ["positions"], key = "#moves")
    suspend fun analyzePosition(moves: String): ChessAnalysis {
        // Expensive chess engine call
        return chessApiTool.getAnalysisResponseFromMoves(moves)
    }
    
    @Cacheable(value = ["openings"], key = "#moves.take(20)")
    fun identifyOpening(moves: String): String? {
        val openingMoves = moves.split(" ").take(6).joinToString(" ")
        return openingDatabase.findOpening(openingMoves)
    }
}
```

### **3. Validation and Error Responses**

```kotlin
data class AnalyzeRequest(
    @field:NotBlank(message = "FEN notation is required")
    @field:Pattern(
        regexp = "^[rnbqkpRNBQKP1-8/]+ [wb] [KQkq-]+ [a-h3-6-] \\d+ \\d+$",
        message = "Invalid FEN notation format"
    )
    val fen: String,
    
    @field:Min(value = 1, message = "Depth must be at least 1")
    @field:Max(value = 25, message = "Depth cannot exceed 25")
    val depth: Int? = null,
    
    @field:Min(value = 1, message = "Variants must be at least 1")
    @field:Max(value = 5, message = "Too many variants requested")
    val variants: Int? = null
)

@RestController
class ChessCoachingController {
    
    @PostMapping("/analyze")
    fun analyzePosition(
        @Valid @RequestBody request: AnalyzeRequest
    ): ResponseEntity<AnalyzeResponse> {
        return try {
            val result = chessCoachingService.analyzePosition(request)
            ResponseEntity.ok(result)
        } catch (e: ValidationException) {
            ResponseEntity.badRequest().body(
                AnalyzeResponse(success = false, error = e.message)
            )
        }
    }
}

@ControllerAdvice
class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException
    ): ResponseEntity<Map<String, Any>> {
        val errors = ex.bindingResult.fieldErrors.associate { 
            it.field to (it.defaultMessage ?: "Invalid value")
        }
        
        return ResponseEntity.badRequest().body(
            mapOf(
                "success" to false,
                "message" to "Validation failed",
                "errors" to errors
            )
        )
    }
}
```

### **4. Health Checks and Monitoring**

```kotlin
@Component
class ChessEngineHealthIndicator(
    private val chessApiTool: ChessApiTool
) : HealthIndicator {
    
    override fun health(): Health {
        return try {
            val testResponse = chessApiTool.analyzePosition(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
            )
            
            if (testResponse != null) {
                Health.up()
                    .withDetail("engine", "responsive")
                    .withDetail("lastCheck", System.currentTimeMillis())
                    .build()
            } else {
                Health.down()
                    .withDetail("engine", "not responding")
                    .build()
            }
        } catch (e: Exception) {
            Health.down(e)
                .withDetail("engine", "error")
                .build()
        }
    }
}

@Component
class ChessCoachingMetrics {
    
    private val requestCounter = Counter.build()
        .name("coaching_requests_total")
        .help("Total coaching requests")
        .labelNames("status", "elo_range")
        .register()
    
    private val responseTimes = Histogram.build()
        .name("coaching_response_duration_seconds")
        .help("Response time for coaching requests")
        .register()
    
    fun recordRequest(status: String, eloRange: String, duration: Double) {
        requestCounter.labels(status, eloRange).inc()
        responseTimes.observe(duration)
    }
}
```

---

## 🧪 **Testing Strategies**

### **1. Unit Testing Services**

```kotlin
@ExtendWith(MockitoExtension::class)
class ChessCoachingServiceTest {
    
    @Mock
    private lateinit var chatClientBuilder: ChatClient.Builder
    
    @Mock
    private lateinit var chatClient: ChatClient
    
    @Mock
    private lateinit var chessApiTool: ChessApiTool
    
    @InjectMocks
    private lateinit var chessCoachingService: ChessCoachingService
    
    @BeforeEach
    fun setup() {
        whenever(chatClientBuilder.build()).thenReturn(chatClient)
    }
    
    @Test
    fun `should determine correct analysis depth for beginner`() {
        // Given
        val beginnerElo = 500
        
        // When
        val depth = chessCoachingService.determineAnalysisDepth(beginnerElo)
        
        // Then
        assertThat(depth).isEqualTo(8)
    }
    
    @Test
    fun `should generate coaching advice with engine analysis`() = runBlocking {
        // Given
        val userInfo = UserInfo(color = "white", elo = 1200)
        val engineAnalysis = ChessApiResponse(
            text = "Best move: e4",
            eval = 0.5,
            move = "e2e4",
            san = "e4"
        )
        
        // When
        val advice = chessCoachingService.generateStructuredCoachingAdvice(
            userInfo, 
            FenAnalysis("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", true),
            engineAnalysis
        )
        
        // Then
        assertThat(advice.suggestedMove).isEqualTo("e4")
        assertThat(advice.difficulty).isEqualTo("intermediate")
    }
}
```

### **2. Integration Testing**

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = [
    "spring.ai.openai.api-key=test-key"
])
class ChessCoachingControllerIntegrationTest {
    
    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate
    
    @MockBean
    private lateinit var chessCoachingService: ChessCoachingService
    
    @Test
    fun `should return coaching advice for valid request`() {
        // Given
        val mockResponse = "Great opening! Try developing your knight next."
        whenever(chessCoachingService.analyzePosition(any(), any()))
            .thenReturn(mockResponse)
        
        val request = LinkedMultiValueMap<String, Any>().apply {
            add("screenshot", FileSystemResource("src/test/resources/test-board.jpg"))
            add("moves", "e4 e5 Nf3")
        }
        
        val headers = HttpHeaders().apply {
            contentType = MediaType.MULTIPART_FORM_DATA
        }
        
        // When
        val response = testRestTemplate.exchange(
            "/api/coach",
            HttpMethod.POST,
            HttpEntity(request, headers),
            CoachingResponse::class.java
        )
        
        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.advice).isEqualTo(mockResponse)
    }
}
```

### **3. Spring AI Testing**

```kotlin
@SpringBootTest
class SpringAIIntegrationTest {
    
    @Autowired
    private lateinit var chatClient: ChatClient
    
    @Test
    @Disabled("Requires OpenAI API key")
    fun `should generate chess coaching advice`() {
        // Given
        val prompt = """
            You are a chess coach. A beginner player just played e4.
            Give them encouraging advice for their next move.
        """.trimIndent()
        
        // When
        val response = chatClient.prompt()
            .user(prompt)
            .call()
            .content()
        
        // Then
        assertThat(response).isNotBlank()
        assertThat(response.lowercase()).contains("develop")
    }
    
    @Test
    fun `should handle structured output`() {
        // Mock ChatClient for testing without API calls
        val mockChatClient = mockk<ChatClient>()
        val mockPrompt = mockk<ChatClient.ChatClientRequest.CallPromptRequestSpec>()
        val mockCall = mockk<ChatClient.ChatClientRequest.CallResponseSpec>()
        
        every { mockChatClient.prompt() } returns mockPrompt
        every { mockPrompt.user(any<String>()) } returns mockPrompt
        every { mockPrompt.call() } returns mockCall
        every { mockCall.entity(UserInfo::class.java) } returns UserInfo("white", 1200)
        
        // Test the structured output functionality
        val result = mockCall.entity(UserInfo::class.java)
        assertThat(result.color).isEqualTo("white")
        assertThat(result.elo).isEqualTo(1200)
    }
}
```

### **4. Test Configuration**

```kotlin
@TestConfiguration
class TestConfiguration {
    
    @Bean
    @Primary
    fun mockChessApiTool(): ChessApiTool {
        return mockk<ChessApiTool>().apply {
            every { getAnalysisResponseFromMoves(any(), any(), any(), any()) } returns
                ChessApiResponse(
                    text = "Test analysis",
                    eval = 0.0,
                    move = "e2e4",
                    san = "e4"
                )
        }
    }
    
    @Bean
    @Primary
    fun testChatClientBuilder(): ChatClient.Builder {
        val mockBuilder = mockk<ChatClient.Builder>()
        val mockChatClient = mockk<ChatClient>()
        
        every { mockBuilder.build() } returns mockChatClient
        every { mockBuilder.defaultTools(any()) } returns mockBuilder
        
        return mockBuilder
    }
}
```

---

## 🚀 **Production Deployment**

### **1. Docker Configuration**

```dockerfile
# Dockerfile
FROM openjdk:17-jdk-slim

# Create app directory
WORKDIR /app

# Copy built jar
COPY api/build/libs/vibechess-api-*.jar app.jar

# Create non-root user
RUN useradd -r -s /bin/false appuser
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Environment variables
ENV SPRING_PROFILES_ACTIVE=production
ENV JVM_OPTS="-Xmx512m -Xms256m"

# Run application
ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'
services:
  vibechess-api:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - CHESS_ENGINE_URL=${CHESS_ENGINE_URL}
    volumes:
      - ./logs:/app/logs
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s
```

### **2. Production Configuration**

```yaml
# application-production.yml
server:
  port: 8080
  compression:
    enabled: true
  http2:
    enabled: true

spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
          temperature: 0.2
          max-tokens: 1000
      
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    com.vibechess: INFO
    org.springframework.ai: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /app/logs/vibechess-api.log
    max-size: 100MB
    max-history: 30
```

### **3. Kubernetes Deployment**

```yaml
# k8s-deployment.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: vibechess-api
  labels:
    app: vibechess-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: vibechess-api
  template:
    metadata:
      labels:
        app: vibechess-api
    spec:
      containers:
      - name: vibechess-api
        image: vibechess/api:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: api-secrets
              key: openai-api-key
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5

---
apiVersion: v1
kind: Service
metadata:
  name: vibechess-api-service
spec:
  selector:
    app: vibechess-api
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer
```

---

## 📋 **Best Practices**

### **1. Code Organization**

```kotlin
// Use package-by-feature instead of package-by-layer
com.vibechess.api/
├── coaching/
│   ├── CoachingController.kt
│   ├── CoachingService.kt
│   ├── CoachingRequest.kt
│   └── CoachingResponse.kt
├── analysis/
│   ├── AnalysisController.kt
│   ├── AnalysisService.kt
│   └── ChessApiTool.kt
└── config/
    ├── AIConfiguration.kt
    └── SecurityConfiguration.kt
```

### **2. Error Handling**

```kotlin
// Custom exception hierarchy
sealed class ChessApiException(message: String) : Exception(message)

class InvalidMoveException(moves: String) : 
    ChessApiException("Invalid move sequence: $moves")

class ChessEngineException(cause: Throwable) : 
    ChessApiException("Chess engine error: ${cause.message}")

class AIServiceException(service: String, cause: Throwable) : 
    ChessApiException("AI service '$service' error: ${cause.message}")

// Global exception handler
@ControllerAdvice
class ChessApiExceptionHandler {
    
    @ExceptionHandler(InvalidMoveException::class)
    fun handleInvalidMoves(ex: InvalidMoveException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.badRequest().body(
            ErrorResponse(
                error = "INVALID_MOVES",
                message = ex.message,
                timestamp = Instant.now()
            )
        )
    }
}
```

### **3. Configuration Management**

```kotlin
// Environment-specific configuration
@ConfigurationProperties(prefix = "chess.coaching")
data class ChessCoachingConfig(
    val ai: AIConfig = AIConfig(),
    val engine: EngineConfig = EngineConfig(),
    val analysis: AnalysisConfig = AnalysisConfig()
)

data class AIConfig(
    val model: String = "gpt-4o",
    val temperature: Double = 0.3,
    val maxTokens: Int = 1000,
    val timeoutSeconds: Long = 30
)

data class EngineConfig(
    val url: String = "http://localhost:8081",
    val timeoutSeconds: Long = 60,
    val maxDepth: Int = 20
)

// Usage in service
@Service
class ChessCoachingService(
    private val config: ChessCoachingConfig,
    private val builder: ChatClient.Builder
) {
    
    private val chatClient = builder
        .defaultOptions(OpenAiChatOptions.builder()
            .model(config.ai.model)
            .temperature(config.ai.temperature.toFloat())
            .maxTokens(config.ai.maxTokens)
            .build())
        .build()
}
```

### **4. Security Considerations**

```kotlin
@Configuration
@EnableWebSecurity
class SecurityConfiguration {
    
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .cors { cors ->
                cors.configurationSource(corsConfigurationSource())
            }
            .csrf { csrf -> csrf.disable() }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/api/test").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt(Customizer.withDefaults())
            }
            .build()
    }
    
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = listOf("https://*.chess.com")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/api/**", configuration)
        return source
    }
}
```

---

## 🔍 **Troubleshooting Guide**

### **1. Common Spring Boot Issues**

```kotlin
// Issue: Bean not found
// Solution: Check component scanning
@SpringBootApplication(scanBasePackages = ["com.vibechess.api"])
class VibechessApiApplication

// Issue: Configuration not loaded
// Solution: Enable configuration properties
@EnableConfigurationProperties(ChessCoachingConfig::class)
@Configuration
class ApplicationConfiguration

// Issue: Async not working
// Solution: Enable async processing
@EnableAsync
@Configuration
class AsyncConfiguration
```

### **2. Spring AI Troubleshooting**

```kotlin
// Issue: OpenAI API key not recognized
// Solution: Verify configuration
@Component
class OpenAIHealthCheck(
    @Value("\${spring.ai.openai.api-key:}") private val apiKey: String
) {
    @PostConstruct
    fun checkApiKey() {
        if (apiKey.isBlank() || apiKey == "your-api-key-here") {
            throw IllegalStateException("OpenAI API key not configured")
        }
        log.info("OpenAI API key configured: ${apiKey.take(7)}...")
    }
}

// Issue: Structured output not working
// Solution: Check data class structure
data class UserInfo(
    val color: String?,     // Make sure all fields are nullable or have defaults
    val elo: Int? = null,   // Use appropriate types
    val confidence: String? = null
) {
    // Jackson needs a no-arg constructor
    constructor() : this(null, null, null)
}
```

### **3. Performance Issues**

```kotlin
// Issue: Slow AI responses
// Solution: Implement timeouts and caching
@Service
class ChessCoachingService {
    
    @Cacheable("coaching-advice")
    @TimeLimiter(name = "coaching", fallbackMethod = "fallbackAdvice")
    suspend fun generateAdvice(moves: String): String {
        return withTimeout(30.seconds) {
            chatClient.prompt()
                .user("Analyze: $moves")
                .call()
                .content()
        }
    }
    
    fun fallbackAdvice(moves: String, ex: Exception): String {
        return "Analysis is taking longer than expected. Here are general principles..."
    }
}
```

### **4. Deployment Issues**

```bash
# Issue: Application won't start in Docker
# Solution: Check Java version and memory
docker run -e JAVA_OPTS="-Xmx512m" vibechess/api:latest

# Issue: Health check fails
# Solution: Verify actuator endpoints
curl http://localhost:8080/actuator/health

# Issue: Cannot connect to OpenAI
# Solution: Check network and environment variables
docker run -e OPENAI_API_KEY="sk-..." vibechess/api:latest
```

---

## 🎓 **Learning Exercises**

### **Exercise 1: Add New Endpoint**
Create a `/api/opening-name` endpoint that identifies chess openings:

```kotlin
@PostMapping("/opening-name")
fun identifyOpening(@RequestBody request: OpeningRequest): OpeningResponse {
    // TODO: Implement opening identification
    // Hint: Use Spring AI to analyze the first few moves
}

data class OpeningRequest(val moves: String)
data class OpeningResponse(val name: String?, val description: String?)
```

### **Exercise 2: Add Caching**
Implement Redis caching for position analysis:

```kotlin
// Add Redis dependency to build.gradle.kts
implementation("org.springframework.boot:spring-boot-starter-data-redis")

// Configure caching
@Cacheable(value = ["positions"], key = "#moves.hashCode()")
suspend fun analyzePosition(moves: String): AnalysisResult {
    // Your implementation
}
```

### **Exercise 3: Add Metrics**
Implement custom metrics for coaching quality:

```kotlin
@Component
class CoachingMetrics {
    
    private val adviceCounter = Counter.build()
        .name("coaching_advice_total")
        .labelNames("elo_range", "opening_type")
        .register()
    
    fun recordAdvice(elo: Int?, opening: String?) {
        val eloRange = categorizeElo(elo)
        adviceCounter.labels(eloRange, opening ?: "unknown").inc()
    }
}
```

---

## 📚 **Additional Resources**

### **Documentation**
- [Spring Boot Reference Guide](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Kotlin Spring Guide](https://spring.io/guides/tutorials/spring-boot-kotlin/)

### **Examples and Tutorials**
- [Spring AI Examples](https://github.com/spring-projects/spring-ai)
- [Kotlin Spring Boot Tutorial](https://kotlinlang.org/docs/spring-boot-restful.html)
- [Building REST APIs with Spring Boot](https://spring.io/guides/tutorials/rest/)

### **Best Practices**
- [Spring Boot Best Practices](https://www.baeldung.com/spring-boot-best-practices)
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [REST API Design Guidelines](https://restfulapi.net/)

---

## 🎯 **Current Application State & Key Improvements**

### **✅ Final Architecture Overview**

Our chess coaching API has evolved into a sophisticated, production-ready application with the following architecture:

```
┌─────────────────────────────────────────────────────┐
│                🌐 REST API LAYER                     │
│  ChessCoachingController → CoachingResponse         │
│  (bestMove + advice)                                │
└─────────────────────────────────────────────────────┘
                            ↕️
┌─────────────────────────────────────────────────────┐
│                ⚙️ BUSINESS LAYER                     │
│  ChessCoachingService → CoachingResult              │
│  (Structured Output + Tool Integration)             │
└─────────────────────────────────────────────────────┘
                            ↕️
┌─────────────────────────────────────────────────────┐
│                🔧 TOOL LAYER                        │
│  ChessApiTool → Chess Engine Analysis               │
│  (Automatic AI Integration)                         │
└─────────────────────────────────────────────────────┘
```

### **🚀 Key Features Implemented**

#### **1. Structured Output Integration**
- **Before**: Manual text parsing and move extraction
- **After**: Spring AI structured outputs with `CoachingResult`
- **Benefit**: Reliable data format, type safety, consistent responses

```kotlin
// Current approach - Clean and reliable
val result = clientWithTool.prompt()
    .system(systemPrompt)
    .user(userPrompt)
    .call()
    .entity(CoachingResult::class.java)

return CoachingResponse(
    bestMove = result.bestMove,
    advice = result.advice
)
```

#### **2. Tool Registration Conflict Resolution**
- **Problem**: "Multiple tools with the same name" errors
- **Root Cause**: Dual registration via `@Component` and `.defaultTools()`
- **Solution**: Explicit bean configuration with `ToolConfiguration`
- **Result**: Consistent tool availability across all requests

```kotlin
// Fixed configuration approach
@Configuration
class ToolConfiguration {
    @Bean
    fun chessApiTool(): ChessApiTool {
        return ChessApiTool()
    }
}

// Clean tool class without @Component
class ChessApiTool {
    @Tool(description = "Analyze chess position from moves using Stockfish engine")
    fun analyzeFromMoves(
        @ToolParam(description = "Sequence of moves in standard algebraic notation") 
        moves: String,
        @ToolParam(description = "Analysis depth (max: 18, default: 12)")
        depth: Int = 12
    ): String {
        // Tool implementation
    }
}
```

#### **3. Comprehensive AI Coaching**
- **Enhanced Prompts**: Multi-section system prompts with coaching principles
- **Educational Focus**: Multiple strategic concepts per response
- **Skill-Level Adaptation**: ELO-based depth and explanation complexity
- **Game Phase Awareness**: Opening/middlegame/endgame specific advice

#### **4. Robust Error Handling**
- **Structured Fallbacks**: All errors return `CoachingResult` format
- **Game Phase Fallbacks**: Contextual advice based on move number
- **Graceful Degradation**: Never fails completely, always provides value

### **🎓 Spring AI Lessons Learned**

#### **Tool Calling Best Practices**
1. **Choose One Registration Method**: Either `@Component` OR explicit bean configuration
2. **Use Descriptive Tool Names**: Help AI understand when to use tools
3. **Handle Tool Failures**: Always provide fallback logic
4. **Cache Tool Results**: Avoid redundant expensive calls

#### **Structured Output Guidelines**
1. **Make Fields Nullable**: Handle AI response variations gracefully
2. **Provide Clear Instructions**: Explicit format requirements in prompts
3. **Use Appropriate Data Types**: Match AI capabilities with Kotlin types
4. **Validate Responses**: Always check for null results

#### **Prompt Engineering Insights**
1. **Separate System/User Prompts**: Clear role definition vs. specific requests
2. **Be Specific About Format**: Explicit instructions for structured outputs
3. **Provide Examples**: Help AI understand expected response patterns
4. **Use Progressive Enhancement**: Basic → Advanced concepts based on user level

### **🔧 Technical Architecture Benefits**

#### **Maintainable Design**
- **Single Responsibility**: Each class has one clear purpose
- **Dependency Injection**: Spring manages all component lifecycles
- **Configuration Management**: Centralized tool and service configuration
- **Type Safety**: Kotlin's null safety prevents runtime errors

#### **Scalable AI Integration**
- **Tool-Based Architecture**: AI decides when/how to use chess analysis
- **Adaptive Analysis**: ELO-based depth adjustment
- **Structured Communication**: Reliable data flow between components

#### **Production Ready Features**
- **Error Recovery**: Multiple fallback layers
- **Performance Optimization**: Appropriate analysis depth by skill level
- **Consistent Responses**: Structured output ensures API reliability
- **Educational Value**: Every response teaches chess concepts

### **📊 Current API Response Example**

```json
{
  "bestMove": "Nf3",
  "advice": "Let's break down the current position and analyze it step by step, keeping in mind that you are a beginner with an ELO rating of 350.\n\n### Current Position Evaluation\n- **Score:** +0.25 (This indicates a roughly equal position with a slight advantage for White)\n- **Your Move Number:** 3\n- **Turn:** It's White's turn to move.\n\n### Best Move for White\nThe best move for you in this situation is **Nf3** (moving the knight from g1 to f3).\n\n### Why is Nf3 a Good Move?\n1. **Development**: Moving the knight to f3 helps to develop your pieces, which is crucial in the opening. Development means bringing your pieces out from their starting positions to more active squares.\n\n2. **Center Control**: The knight on f3 helps control the central squares e5 and d4, which are important in chess. Controlling the center gives you more options and space for your pieces.\n\n3. **King Safety**: By developing the knight, you're also preparing to castle kingside, which will keep your king safe.\n\n### Opening Principles for Beginners\n- **Control the center** with pawns (e4, d4) and pieces\n- **Develop knights before bishops**\n- **Castle early** to keep your king safe\n- **Don't move the same piece twice** in the opening without a good reason\n\nKeep developing your pieces and following these opening principles!"
}
```

### **🎯 Next Steps for Enhancement**

#### **Potential Improvements**
1. **Opening Database Integration**: Identify specific openings by name
2. **Game Analysis**: Analyze complete games for learning
3. **Tactical Puzzle Generation**: Create custom puzzles from positions
4. **Progress Tracking**: Track user improvement over time
5. **Multiplayer Features**: Coach both sides of a game

#### **Advanced Spring AI Features to Explore**
1. **RAG Integration**: Add chess book knowledge base
2. **Memory Management**: Remember user preferences and weaknesses
3. **Multi-Modal Input**: Accept game notation files or board images
4. **Streaming Responses**: Real-time coaching as moves are played

### **🏆 Project Success Metrics**

This tutorial successfully demonstrates:
- ✅ **Spring Boot** fundamentals with real-world application
- ✅ **Kotlin** integration with Spring ecosystem
- ✅ **Spring AI** advanced features (tools, structured output, prompting)
- ✅ **Production-ready** error handling and configuration
- ✅ **Educational value** with comprehensive chess coaching
- ✅ **Problem-solving** approach to complex integration challenges

**You now have a fully functional, production-ready chess coaching API that showcases modern Spring development practices with AI integration!**

---

## 📚 **Additional Resources** 