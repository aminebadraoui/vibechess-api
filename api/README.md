# VibechessAPI Chess Engine Integration

This project integrates with [chess-api.com](https://chess-api.com/) to provide chess position analysis using Spring AI's tool calling capability and structured outputs.

## Architecture Overview

The `ChessCoachingService` implements a comprehensive coaching system with the following flow:

1. **User Info Extraction** - Analyze screenshot to determine user's ELO and color using Spring AI structured outputs
2. **FEN Analysis** - Convert move notation to FEN with validation using structured outputs  
3. **Engine Analysis** - Get chess engine analysis via chess-api.com with ELO-appropriate depth
4. **Coaching Generation** - Create tailored coaching advice using the engine analysis
5. **Response Formatting** - Format the final coaching message based on user skill level

## Key Features

### Spring AI Structured Outputs
- Uses Spring AI's `BeanOutputConverter` for reliable JSON parsing
- Structured data models for `UserInfo`, `FenAnalysis`, and `CoachingAdvice`
- Automatic format instruction generation and validation

### ELO-Based Analysis Depth
- **Beginners (< 800)**: Depth 8 - Simple analysis for basic concepts
- **Intermediate (800-1200)**: Depth 10 - Moderate analysis 
- **Club Level (1200-1600)**: Depth 12 - Standard analysis
- **Advanced (1600-2000)**: Depth 15 - Deep analysis
- **Expert (2000+)**: Depth 18 - Maximum depth analysis

### Game Phase Detection & Adaptive Coaching
The system automatically detects the game phase and provides appropriate advice:

#### **Opening Phase** (Moves 1-10, 24+ pieces)
- Focus on development, center control, king safety
- Specific opening principles and piece development

#### **Middlegame** (Moves 11-25, 12+ pieces)
- Tactical awareness and piece safety
- Looking for hanging pieces and simple tactics
- Piece improvement and coordination

#### **Endgame** (Move 25+, <12 pieces)
- King activity and pawn promotion
- Passed pawn creation and advancement
- Simplified tactical themes

### Robust Error Handling
- **FEN Validation Override**: If AI incorrectly marks valid FEN as invalid, basic validation override ensures continuity
- **Chess API Resilience**: Handles missing fields in API responses with fallback parsing
- **Graceful Degradation**: Falls back to appropriate coaching based on game phase when engine analysis fails

### Chess Tool Integration
- Spring AI tool calling with `@Tool` annotations
- Automatic function discovery and execution
- Seamless integration with ChatGPT for move validation and coaching

## API Endpoints

### Chess Coaching (Main Endpoint)

```
POST /api/coach
```

Analyzes a chess position from a screenshot and move history to provide personalized coaching.

**Request:**
- `screenshot` (multipart file): Screenshot of the chess position
- `moves` (string): Move history in standard algebraic notation

**Response Examples:**

**Opening (350 ELO):**
```json
{
  "advice": "Hi there! Suggested Move: Nf3\n\nGreat job opening with d4! That's the Queen's pawn opening - a solid choice...\n\n(Advice tailored for ~350 ELO)"
}
```

**Endgame (350 ELO):**
```json
{
  "advice": "Hi there! Suggested Move: Push passed pawns\n\nYou're in the endgame! This is where games are won and lost. Key endgame principles for beginners:\n\n1. King activity - Bring your king into the fight!\n2. Push passed pawns...\n\n(Advice tailored for ~350 ELO)"
}
```

### Chess Position Analysis (Direct Engine Access)

```
POST /api/analyze
```

Direct access to the chess engine via chess-api.com.

**Request Body:**
```json
{
  "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
  "depth": 15,
  "variants": 3,
  "maxThinkingTime": 50
}
```

**Response:**
```json
{
  "success": true,
  "eval": 0.25,
  "bestMove": "e2e4",
  "san": "e4",
  "text": "Move e2 → e4: [+0.25]. White is slightly better.",
  "winChance": 53.65,
  "depth": 15,
  "mate": null,
  "continuation": ["e2e4", "e7e5", "g1f3"]
}
```

### Tool-Based Analysis

```
GET /api/analyze-with-tool?fen={fen}&depth={depth}
```

Uses the Spring AI tool to analyze a position and return a natural language explanation.

## Data Models

### UserInfo (Structured Output)
```kotlin
data class UserInfo(
    val color: String?, // "White", "Black", or "Uncertain"
    val elo: Int?, // null if uncertain
    val colorConfidence: String? = null, // "high", "medium", "low"
    val eloConfidence: String? = null // "high", "medium", "low"
)
```

### FenAnalysis (Structured Output)
```kotlin
data class FenAnalysis(
    val fen: String,
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val moveNumber: Int? = null,
    val activeColor: String? = null, // "w" or "b"
    val castlingRights: String? = null // "KQkq", partial, or "-"
)
```

### CoachingAdvice (Structured Output)
```kotlin
data class CoachingAdvice(
    val suggestedMove: String,
    val explanation: String,
    val reasoning: String,
    val difficulty: String, // "beginner", "intermediate", "advanced"
    val confidence: String // "high", "medium", "low"
)
```

### ChessApiResponse (Enhanced Error Handling)
```kotlin
data class ChessApiResponse(
    val success: Boolean,
    val eval: Double,
    val move: String?, // Now nullable to handle API inconsistencies
    val san: String?,
    val text: String,
    val winChance: Double?,
    val depth: Int?,
    val mate: Int?,
    val continuationArr: List<String>?
)
```

## Spring AI Integration

### Tool Calling
The `ChessApiTool` class uses Spring AI's `@Tool` annotation to enable automatic function calling:

```kotlin
@Tool(description = "Analyze a chess position using Stockfish engine via chess-api.com")
fun analyzePosition(
    @ToolParam(description = "FEN notation of the chess position") 
    fen: String,
    @ToolParam(description = "Analysis depth (max: 18, default: 12)")
    depth: Int = 12
): String
```

### Structured Outputs
The service uses Spring AI's structured output capabilities to ensure reliable data parsing:

```kotlin
// Example usage
val result = chatClient.prompt()
    .user(promptText)
    .options(options)
    .call()
    .entity(UserInfo::class.java)
```

## Configuration

### Dependencies
- Spring AI 1.0.0-RC1
- Spring Boot 3.4.5
- Spring WebFlux (for reactive WebClient)
- Jackson Kotlin Module
- Kotlin 1.9.25

### Environment Variables
Set the following environment variables:
- `OPENAI_API_KEY`: Your OpenAI API key for GPT-4o
- `CHESS_API_URL`: chess-api.com base URL (optional, defaults to https://chess-api.com/v1)

## Testing

The project includes comprehensive tests for:
- Structured output data models
- ELO-based analysis depth logic
- User categorization and skill level determination
- Game phase detection (opening, middlegame, endgame)
- FEN validation and override mechanisms
- Integration with chess-api.com (disabled by default)

Run tests with:
```bash
./gradlew test
```

## Error Handling & Resilience

### FEN Validation
- AI-based FEN analysis with structured outputs
- Basic validation override for incorrectly marked invalid FENs
- Detailed error reporting for genuinely invalid positions

### Chess API Resilience
- Nullable field handling for inconsistent API responses
- Fallback text parsing when JSON parsing fails
- Graceful degradation to game-phase appropriate advice

### Coaching Fallbacks
- **Very Low ELO (<600)**: Game-phase specific advice without complex engine analysis
- **Engine Failure**: Tool-based analysis with AI reasoning
- **Complete Failure**: Structured advice based on basic chess principles

## Usage Example

```kotlin
// Inject the service
@Autowired
private lateinit var chessCoachingService: ChessCoachingService

// Analyze a position
val advice = chessCoachingService.analyzePosition(screenshot, "e4 e5 Nf3 Nc6")
```

The service will:
1. Extract user ELO and color from the screenshot
2. Convert moves to valid FEN notation
3. Determine appropriate analysis depth based on ELO
4. Get engine analysis from chess-api.com
5. Generate personalized coaching advice based on game phase
6. Format the response for the user's skill level

## Recent Improvements

### ✅ **Fixed Issues**
- **FEN Validation**: Added override mechanism for AI incorrectly marking valid FENs as invalid
- **Chess API Endpoint**: Fixed 404 error by using correct endpoint `POST /v1` instead of `/v1/analyze`
- **Chess API Errors**: Made response fields nullable and added fallback parsing
- **Game Phase Awareness**: Coaching now adapts to opening/middlegame/endgame
- **Low ELO Coaching**: Specialized advice for very low ELO players (under 600)

### ✅ **Enhanced Features**
- **Piece Count Detection**: Automatically detects game phase based on remaining pieces
- **Move Number Awareness**: Uses move number to determine appropriate coaching style
- **Robust Error Recovery**: Multiple fallback layers ensure coaching always provides value
- **Context-Appropriate Advice**: No more opening advice in endgames! 