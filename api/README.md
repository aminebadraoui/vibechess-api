# VibechessAPI Chess Coaching Engine

This project provides AI-powered chess coaching by analyzing chess positions from screenshots and game moves using Spring AI's tool calling capability and structured outputs.

## Architecture Overview

The `ChessCoachingService` implements a comprehensive coaching system with the following flow:

1. **User Info Extraction** - Analyze screenshot to determine user's ELO and color using Spring AI structured outputs
2. **Structured Coaching Analysis** - AI uses chess analysis tool automatically to provide comprehensive coaching
3. **Adaptive Depth Analysis** - ELO-based analysis depth for appropriate complexity
4. **Comprehensive Response** - Returns both best move and detailed coaching advice

## Key Features

### Spring AI Structured Outputs & Tool Integration
- Uses Spring AI's structured output capabilities for reliable `CoachingResult` parsing
- Automatic chess analysis tool calling with `@Tool` annotations
- Seamless integration between AI reasoning and chess engine analysis
- Fixed tool registration conflicts for consistent performance

### Enhanced Coaching Response Format
The API now returns structured coaching data:
```kotlin
data class CoachingResult(
    val bestMove: String?,      // Recommended move in algebraic notation
    val advice: String          // Comprehensive coaching explanation
)
```

### ELO-Based Analysis Depth
- **Beginners (< 800)**: Depth 8 - Simple analysis for basic concepts
- **Intermediate (800-1200)**: Depth 10 - Moderate analysis 
- **Club Level (1200-1600)**: Depth 12 - Standard analysis
- **Advanced (1600-2000)**: Depth 15 - Deep analysis
- **Expert (2000+)**: Depth 18 - Maximum depth analysis

### Comprehensive AI Coaching
The system provides detailed, educational coaching that includes:

#### **Position Evaluation**
- Chess engine evaluation scores and win percentages
- Position characteristics (opening/middlegame/endgame)
- Strategic and tactical assessment

#### **Educational Content**
- Multiple strategic concepts explained per response
- Chess terminology and opening names when applicable
- Skill-level appropriate explanations with examples
- Threat awareness and tactical considerations

#### **Game Phase Adaptive Advice**
- **Opening Phase** (Moves 1-10): Development, center control, king safety
- **Middlegame** (Moves 11-25): Tactical patterns, piece improvement, threats
- **Endgame** (Move 25+): King activity, pawn promotion, simplified tactics

### Robust Tool Configuration
- **Fixed Tool Registration**: Resolved "Multiple tools with the same name" conflicts
- **Explicit Bean Configuration**: `ToolConfiguration` class manages tool registration
- **Consistent Performance**: Tools work reliably across multiple requests

## API Endpoints

### Chess Coaching (Main Endpoint)

```
POST /api/coach
```

Analyzes a chess position from a screenshot and move history to provide personalized coaching with move recommendations.

**Request:**
- `screenshot` (multipart file): Screenshot of the chess position
- `moves` (string): Move history in standard algebraic notation

**Response Format:**
```json
{
  "bestMove": "Nf3",
  "advice": "Comprehensive coaching explanation with multiple strategic concepts..."
}
```

**Example Response (350 ELO, Opening):**
```json
{
  "bestMove": "Nf3",
  "advice": "Let's break down the current position and analyze it step by step, keeping in mind that you are a beginner with an ELO rating of 350.\n\n### Current Position Evaluation\n- **Score:** +0.25 (This indicates a roughly equal position with a slight advantage for White)\n- **Your Move Number:** 3\n- **Turn:** It's White's turn to move.\n\n### Best Move for White\nThe best move for you in this situation is **Nf3** (moving the knight from g1 to f3).\n\n### Why is Nf3 a Good Move?\n1. **Development**: Moving the knight to f3 helps to develop your pieces, which is crucial in the opening. Development means bringing your pieces out from their starting positions to more active squares.\n\n2. **Center Control**: The knight on f3 helps control the central squares e5 and d4, which are important in chess. Controlling the center gives you more options and space for your pieces.\n\n3. **King Safety**: By developing the knight, you're also preparing to castle kingside, which will keep your king safe.\n\n### Opening Principles for Beginners\n- **Control the center** with pawns (e4, d4) and pieces\n- **Develop knights before bishops**\n- **Castle early** to keep your king safe\n- **Don't move the same piece twice** in the opening without a good reason\n\nKeep developing your pieces and following these opening principles!"
}
```

### Test Endpoint

```
GET /api/test
```

Simple health check endpoint that returns "This is a test".

## Data Models

### CoachingResult (Main Response)
```kotlin
data class CoachingResult(
    val bestMove: String?,      // Recommended move in standard algebraic notation
    val advice: String          // Comprehensive coaching explanation
)
```

### UserInfo (Vision Analysis)
```kotlin
data class UserInfo(
    val color: String?,         // "White", "Black", or "Uncertain"
    val elo: Int?,             // Player's ELO rating or null if uncertain
    val colorConfidence: String?, // "High", "Medium", "Low"
    val eloConfidence: String?    // "High", "Medium", "Low"
)
```

### ChessApiResponse (Engine Integration)
```kotlin
data class ChessApiResponse(
    val text: String,           // Engine analysis description
    val eval: Double,           // Position evaluation score
    val move: String?,          // Best move in coordinate notation
    val fen: String?,           // Position in FEN notation
    val depth: Int?,            // Analysis depth used
    val winChance: Double?,     // Win probability percentage
    val continuationArr: List<String>?, // Continuation moves
    val mate: Int?,             // Mate in X moves (if applicable)
    val san: String?,           // Best move in algebraic notation
    val turn: String?           // Whose turn ("w" or "b")
)
```

## Spring AI Integration

### Tool Configuration
The application uses explicit bean configuration to avoid tool registration conflicts:

```kotlin
@Configuration
class ToolConfiguration {
    @Bean
    fun chessApiTool(): ChessApiTool {
        return ChessApiTool()
    }
}
```

### Chess Analysis Tool
The `ChessApiTool` class provides chess engine integration:

```kotlin
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
): String
```

### Structured Output Integration
The service uses Spring AI's structured output capabilities for reliable response parsing:

```kotlin
val result = clientWithTool.prompt()
    .system(systemPrompt)
    .user(userPrompt)
    .call()
    .entity(CoachingResult::class.java)
```

### Enhanced Prompting System
- **Comprehensive System Prompts**: Define coaching principles, chess education guidelines, and analysis requirements
- **Detailed User Prompts**: Specify analysis requirements and educational objectives
- **Structured Output Instructions**: Ensure consistent response format with both move recommendations and advice

## Configuration

### Environment Variables
- `OPENAI_API_KEY`: OpenAI API key for GPT-4 integration

### Application Properties
```properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

## Dependencies

### Core Dependencies
- Spring Boot 3.4.5
- Spring AI with OpenAI integration
- Kotlin coroutines support
- Jackson for JSON processing

### Key Spring AI Features Used
- ChatClient with tool integration
- Structured output with entity conversion
- Vision capabilities for screenshot analysis
- Tool calling with automatic function discovery

## Architecture Benefits

### Maintainable Design
- **Separation of Concerns**: Clear separation between web layer, business logic, and external integrations
- **Dependency Injection**: Spring manages all component lifecycles and dependencies
- **Configuration Management**: Centralized tool and service configuration

### Scalable AI Integration
- **Tool-Based Architecture**: AI decides when and how to use chess analysis
- **Structured Outputs**: Reliable data parsing and response formatting
- **Adaptive Analysis**: ELO-based depth adjustment for appropriate complexity

### User Experience
- **Comprehensive Coaching**: Multiple strategic concepts explained per response
- **Educational Focus**: Skill-level appropriate explanations with chess terminology
- **Consistent Performance**: Fixed tool conflicts ensure reliable operation 