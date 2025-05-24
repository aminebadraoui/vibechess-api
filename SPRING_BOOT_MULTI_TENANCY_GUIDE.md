# 🏢 **Complete Multi-Tenancy Guide for Spring Boot**
## *Building Scalable SaaS Applications with Tenant Isolation*

---

## 📚 **Table of Contents**

1. [What is Multi-Tenancy?](#what-is-multi-tenancy)
2. [Multi-Tenancy Strategies](#multi-tenancy-strategies)
3. [Choosing the Right Strategy](#choosing-the-right-strategy)
4. [Database Per Tenant Implementation](#database-per-tenant-implementation)
5. [Schema Per Tenant Implementation](#schema-per-tenant-implementation)
6. [Shared Database Implementation](#shared-database-implementation)
7. [Tenant Context Management](#tenant-context-management)
8. [Security & Authentication](#security--authentication)
9. [Configuration Management](#configuration-management)
10. [Real-World Chess API Example](#real-world-chess-api-example)
11. [Testing Multi-Tenant Applications](#testing-multi-tenant-applications)
12. [Deployment & Operations](#deployment--operations)
13. [Best Practices](#best-practices)
14. [Troubleshooting](#troubleshooting)

---

## 🌟 **What is Multi-Tenancy?**

**Multi-tenancy** is an architecture pattern where a single application instance serves multiple tenants (customers/organizations), with each tenant's data and configuration completely isolated from others.

### **Real-World Examples**
- **Slack**: Different workspaces (tenants) using the same Slack application
- **Salesforce**: Multiple companies using the same CRM platform
- **GitHub**: Organizations have isolated repositories and settings
- **Our Chess API**: Different chess clubs/schools using the same coaching platform

### **Why Multi-Tenancy Matters**

**🎯 Cost Efficiency**: One application serves many customers  
**🚀 Scalability**: Easier to scale horizontally  
**🔧 Maintenance**: Single codebase to maintain  
**💰 SaaS Business Model**: Essential for profitable SaaS products  
**⚡ Resource Sharing**: Efficient use of infrastructure  

### **Single vs Multi-Tenant Architecture**

```
🔴 Single-Tenant (Traditional)
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│   Customer A    │ │   Customer B    │ │   Customer C    │
│                 │ │                 │ │                 │
│ ┌─────────────┐ │ │ ┌─────────────┐ │ │ ┌─────────────┐ │
│ │     App     │ │ │ │     App     │ │ │ │     App     │ │
│ │ ┌─────────┐ │ │ │ │ ┌─────────┐ │ │ │ │ ┌─────────┐ │ │
│ │ │Database │ │ │ │ │ │Database │ │ │ │ │ │Database │ │ │
│ │ └─────────┘ │ │ │ │ └─────────┘ │ │ │ │ └─────────┘ │ │
│ └─────────────┘ │ │ └─────────────┘ │ │ └─────────────┘ │
└─────────────────┘ └─────────────────┘ └─────────────────┘

🟢 Multi-Tenant (SaaS)
                ┌─────────────────────────────────────┐
                │          Shared Application         │
                │                                     │
                │  Customer A  │  Customer B  │  Customer C
                │     Data     │     Data     │     Data   
                │              │              │            
                └─────────────────────────────────────┘
```

---

## 🏗️ **Multi-Tenancy Strategies**

### **1. Database Per Tenant (Highest Isolation)**

Each tenant gets their own dedicated database.

```kotlin
// Tenant-specific database configuration
@Configuration
class DatabasePerTenantConfig {
    
    @Bean
    @Primary
    fun tenantDataSourceProvider(): TenantDataSourceProvider {
        return DatabasePerTenantDataSourceProvider()
    }
}

class DatabasePerTenantDataSourceProvider : TenantDataSourceProvider {
    private val dataSources = mutableMapOf<String, DataSource>()
    
    override fun getDataSource(tenantId: String): DataSource {
        return dataSources.computeIfAbsent(tenantId) { tenant ->
            HikariDataSource().apply {
                jdbcUrl = "jdbc:postgresql://localhost:5432/chess_${tenant}"
                username = "chess_user"
                password = "chess_pass"
                maximumPoolSize = 10
            }
        }
    }
}
```

**Pros:**
- ✅ Complete data isolation
- ✅ Easy to backup/restore per tenant
- ✅ Custom performance tuning per tenant
- ✅ Regulatory compliance friendly

**Cons:**
- ❌ High resource usage
- ❌ Complex maintenance
- ❌ Expensive at scale

### **2. Schema Per Tenant (Medium Isolation)**

Single database with separate schemas for each tenant.

```kotlin
@Configuration
class SchemaPerTenantConfig {
    
    @Bean
    fun tenantSchemaResolver(): TenantSchemaResolver {
        return DefaultTenantSchemaResolver()
    }
}

class DefaultTenantSchemaResolver : TenantSchemaResolver {
    
    fun getCurrentSchema(): String {
        val tenantId = TenantContext.getCurrentTenant()
        return "chess_${tenantId}"
    }
    
    fun switchToTenantSchema(tenantId: String) {
        val sql = "SET search_path = chess_${tenantId}, public"
        // Execute schema switch
    }
}
```

**Pros:**
- ✅ Good data isolation
- ✅ Shared database resources
- ✅ Easier maintenance than separate DBs
- ✅ Cross-tenant analytics possible

**Cons:**
- ❌ More complex than shared tables
- ❌ Schema management overhead
- ❌ Limited by database schema limits

### **3. Shared Database with Tenant ID (Lowest Isolation)**

All tenants share the same tables with a tenant identifier column.

```kotlin
@Entity
@Table(name = "coaching_sessions")
class CoachingSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "tenant_id", nullable = false)
    val tenantId: String,  // 🔑 Tenant isolation key
    
    @Column(name = "player_name")
    val playerName: String,
    
    @Column(name = "session_data", columnDefinition = "jsonb")
    val sessionData: String,
    
    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Repository
interface CoachingSessionRepository : JpaRepository<CoachingSession, Long> {
    
    // All queries automatically filtered by tenant
    fun findByTenantIdAndPlayerName(tenantId: String, playerName: String): List<CoachingSession>
    
    @Query("SELECT c FROM CoachingSession c WHERE c.tenantId = :tenantId AND c.createdAt >= :date")
    fun findRecentSessions(tenantId: String, date: LocalDateTime): List<CoachingSession>
}
```

**Pros:**
- ✅ Most cost-effective
- ✅ Easiest to implement
- ✅ Best resource utilization
- ✅ Cross-tenant analytics easy

**Cons:**
- ❌ Risk of data leakage
- ❌ Complex queries with large datasets
- ❌ Backup/restore affects all tenants

---

## 🎯 **Choosing the Right Strategy**

| **Factor** | **Database Per Tenant** | **Schema Per Tenant** | **Shared Database** |
|------------|-------------------------|----------------------|-------------------|
| **Cost** | High | Medium | Low |
| **Isolation** | Highest | High | Medium |
| **Scalability** | Limited | Good | Excellent |
| **Maintenance** | Complex | Medium | Simple |
| **Compliance** | Best | Good | Requires care |
| **Analytics** | Difficult | Possible | Easy |

### **Decision Matrix for Chess API**

```kotlin
// Example tenant requirements
data class TenantRequirements(
    val tenantType: TenantType,
    val expectedUsers: Int,
    val dataCompliance: ComplianceLevel,
    val budget: BudgetLevel
)

enum class TenantType { INDIVIDUAL, CHESS_CLUB, SCHOOL_DISTRICT, ENTERPRISE }
enum class ComplianceLevel { BASIC, GDPR, HIPAA, SOC2 }
enum class BudgetLevel { STARTUP, GROWTH, ENTERPRISE }

class MultiTenancyStrategySelector {
    
    fun recommendStrategy(requirements: TenantRequirements): TenancyStrategy {
        return when {
            requirements.dataCompliance == ComplianceLevel.HIPAA -> 
                TenancyStrategy.DATABASE_PER_TENANT
                
            requirements.tenantType == TenantType.ENTERPRISE && 
            requirements.budget == BudgetLevel.ENTERPRISE -> 
                TenancyStrategy.DATABASE_PER_TENANT
                
            requirements.expectedUsers > 10000 -> 
                TenancyStrategy.SCHEMA_PER_TENANT
                
            else -> 
                TenancyStrategy.SHARED_DATABASE
        }
    }
}
```

---

## 🛡️ **Database Per Tenant Implementation**

### **1. Tenant-Aware Data Sources**

```kotlin
@Configuration
@EnableJpaRepositories(
    basePackages = ["com.vibechess.api.repository"],
    repositoryFactoryBeanClass = TenantAwareJpaRepositoryFactoryBean::class
)
class DatabasePerTenantConfiguration {
    
    @Bean
    @Primary
    fun tenantDataSourceRouter(): DataSource {
        return TenantRoutingDataSource()
    }
    
    @Bean
    fun entityManagerFactory(
        @Qualifier("tenantDataSourceRouter") dataSource: DataSource
    ): LocalContainerEntityManagerFactoryBean {
        val factory = LocalContainerEntityManagerFactoryBean()
        factory.dataSource = dataSource
        factory.setPackagesToScan("com.vibechess.api.entity")
        factory.jpaVendorAdapter = HibernateJpaVendorAdapter()
        
        val properties = Properties()
        properties["hibernate.dialect"] = "org.hibernate.dialect.PostgreSQLDialect"
        properties["hibernate.hbm2ddl.auto"] = "validate"
        properties["hibernate.show_sql"] = "false"
        factory.setJpaProperties(properties)
        
        return factory
    }
}

class TenantRoutingDataSource : AbstractRoutingDataSource() {
    
    override fun determineCurrentLookupKey(): Any? {
        return TenantContext.getCurrentTenant()
    }
    
    override fun afterPropertiesSet() {
        // Initialize with default data sources
        val dataSources = loadTenantDataSources()
        setTargetDataSources(dataSources)
        setDefaultTargetDataSource(dataSources["default"])
        super.afterPropertiesSet()
    }
    
    private fun loadTenantDataSources(): Map<Any, Any> {
        val tenants = tenantConfigurationService.getAllTenants()
        return tenants.associate { tenant ->
            tenant.id to createDataSource(tenant)
        }
    }
    
    private fun createDataSource(tenant: TenantConfiguration): DataSource {
        return HikariDataSource().apply {
            jdbcUrl = "jdbc:postgresql://${tenant.dbHost}:${tenant.dbPort}/${tenant.dbName}"
            username = tenant.dbUsername
            password = tenant.dbPassword
            maximumPoolSize = tenant.maxConnections
            connectionTimeout = 30000
            idleTimeout = 600000
            maxLifetime = 1800000
        }
    }
}
```

### **2. Dynamic Tenant Data Source Management**

```kotlin
@Service
@Transactional
class TenantManagementService(
    private val tenantRepository: TenantRepository,
    private val tenantDataSourceRouter: TenantRoutingDataSource
) {
    
    suspend fun createNewTenant(tenantRequest: CreateTenantRequest): TenantConfiguration {
        // 1. Create tenant configuration
        val tenant = TenantConfiguration(
            id = generateTenantId(),
            name = tenantRequest.name,
            dbHost = selectOptimalDbHost(),
            dbPort = 5432,
            dbName = "chess_${tenantRequest.subdomain}",
            dbUsername = generateTenantDbUser(),
            dbPassword = generateSecurePassword(),
            maxConnections = calculateMaxConnections(tenantRequest.expectedUsers),
            subscriptionPlan = tenantRequest.plan,
            createdAt = LocalDateTime.now()
        )
        
        // 2. Create physical database
        createTenantDatabase(tenant)
        
        // 3. Run migrations
        runTenantMigrations(tenant)
        
        // 4. Add to data source router
        tenantDataSourceRouter.addTenantDataSource(tenant.id, createDataSource(tenant))
        
        // 5. Save tenant configuration
        return tenantRepository.save(tenant)
    }
    
    private suspend fun createTenantDatabase(tenant: TenantConfiguration) {
        val adminDataSource = getAdminDataSource(tenant.dbHost)
        adminDataSource.connection.use { connection ->
            val statement = connection.createStatement()
            
            // Create database
            statement.execute("""
                CREATE DATABASE ${tenant.dbName}
                WITH OWNER = ${tenant.dbUsername}
                ENCODING = 'UTF8'
                CONNECTION LIMIT = ${tenant.maxConnections}
            """.trimIndent())
            
            // Create user
            statement.execute("""
                CREATE USER ${tenant.dbUsername} 
                WITH PASSWORD '${tenant.dbPassword}'
                CREATEDB
            """.trimIndent())
            
            // Grant permissions
            statement.execute("""
                GRANT ALL PRIVILEGES ON DATABASE ${tenant.dbName} 
                TO ${tenant.dbUsername}
            """.trimIndent())
        }
    }
    
    private suspend fun runTenantMigrations(tenant: TenantConfiguration) {
        val flyway = Flyway.configure()
            .dataSource(
                "jdbc:postgresql://${tenant.dbHost}:${tenant.dbPort}/${tenant.dbName}",
                tenant.dbUsername,
                tenant.dbPassword
            )
            .locations("classpath:db/migration")
            .load()
        
        flyway.migrate()
    }
}
```

### **3. Tenant Context Management**

```kotlin
object TenantContext {
    private val currentTenant = ThreadLocal<String>()
    
    fun setCurrentTenant(tenantId: String) {
        currentTenant.set(tenantId)
    }
    
    fun getCurrentTenant(): String? {
        return currentTenant.get()
    }
    
    fun clear() {
        currentTenant.remove()
    }
}

@Component
class TenantInterceptor : HandlerInterceptor {
    
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val tenantId = extractTenantId(request)
        
        if (tenantId == null) {
            response.status = HttpStatus.BAD_REQUEST.value()
            response.writer.write("""{"error": "Tenant ID is required"}""")
            return false
        }
        
        TenantContext.setCurrentTenant(tenantId)
        return true
    }
    
    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        TenantContext.clear()
    }
    
    private fun extractTenantId(request: HttpServletRequest): String? {
        // Method 1: From subdomain
        val host = request.serverName
        if (host.contains(".")) {
            val subdomain = host.split(".")[0]
            if (subdomain != "www" && subdomain != "api") {
                return subdomain
            }
        }
        
        // Method 2: From header
        request.getHeader("X-Tenant-ID")?.let { return it }
        
        // Method 3: From JWT token
        val token = request.getHeader("Authorization")
            ?.removePrefix("Bearer ")
        
        return extractTenantFromJWT(token)
    }
}
```

---

## 📊 **Schema Per Tenant Implementation**

### **1. Dynamic Schema Switching**

```kotlin
@Configuration
class SchemaPerTenantConfiguration {
    
    @Bean
    @Primary
    fun schemaAwareDataSource(): DataSource {
        return SchemaRoutingDataSource()
    }
}

class SchemaRoutingDataSource : AbstractRoutingDataSource() {
    
    @Autowired
    private lateinit var tenantSchemaManager: TenantSchemaManager
    
    override fun getConnection(): Connection {
        val connection = super.getConnection()
        val tenantId = TenantContext.getCurrentTenant()
        
        if (tenantId != null) {
            tenantSchemaManager.switchToTenantSchema(connection, tenantId)
        }
        
        return connection
    }
    
    override fun determineCurrentLookupKey(): Any? {
        // Always use the same database, schema switching happens in connection
        return "main"
    }
}

@Service
class TenantSchemaManager {
    
    private val logger = LoggerFactory.getLogger(TenantSchemaManager::class.java)
    
    fun switchToTenantSchema(connection: Connection, tenantId: String) {
        val schemaName = "chess_$tenantId"
        
        try {
            connection.createStatement().use { statement ->
                statement.execute("SET search_path = $schemaName, public")
            }
            logger.debug("Switched to schema: $schemaName")
        } catch (e: SQLException) {
            logger.error("Failed to switch to schema: $schemaName", e)
            throw TenantSchemaException("Failed to switch to tenant schema", e)
        }
    }
    
    suspend fun createTenantSchema(tenantId: String): Boolean {
        val schemaName = "chess_$tenantId"
        
        return try {
            dataSource.connection.use { connection ->
                // Create schema
                connection.createStatement().execute("CREATE SCHEMA IF NOT EXISTS $schemaName")
                
                // Run tenant-specific migrations
                runSchemaSpecificMigrations(schemaName)
                
                // Set up initial data
                setupInitialTenantData(connection, schemaName)
                
                true
            }
        } catch (e: Exception) {
            logger.error("Failed to create schema for tenant: $tenantId", e)
            false
        }
    }
    
    private fun runSchemaSpecificMigrations(schemaName: String) {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .defaultSchema(schemaName)
            .load()
        
        flyway.migrate()
    }
    
    private fun setupInitialTenantData(connection: Connection, schemaName: String) {
        connection.createStatement().use { statement ->
            statement.execute("SET search_path = $schemaName, public")
            
            // Create default chess coaching configuration
            statement.execute("""
                INSERT INTO coaching_configuration (
                    max_analysis_depth, 
                    default_time_control, 
                    available_engines
                ) VALUES (
                    15, 
                    '10+0', 
                    '["stockfish", "leela"]'
                )
            """.trimIndent())
        }
    }
}
```

### **2. Schema-Aware Repositories**

```kotlin
@Repository
class SchemaAwareCoachingSessionRepository(
    @Qualifier("schemaAwareEntityManager") 
    private val entityManager: EntityManager
) {
    
    fun findPlayerSessions(playerName: String): List<CoachingSession> {
        val tenantId = TenantContext.getCurrentTenant()
            ?: throw TenantContextException("No tenant context available")
        
        // Query automatically executes in current schema
        return entityManager.createQuery(
            "SELECT c FROM CoachingSession c WHERE c.playerName = :playerName",
            CoachingSession::class.java
        )
        .setParameter("playerName", playerName)
        .resultList
    }
    
    fun saveSession(session: CoachingSession): CoachingSession {
        entityManager.persist(session)
        return session
    }
    
    @Transactional
    fun deleteOldSessions(cutoffDate: LocalDateTime): Int {
        return entityManager.createQuery(
            "DELETE FROM CoachingSession c WHERE c.createdAt < :cutoffDate"
        )
        .setParameter("cutoffDate", cutoffDate)
        .executeUpdate()
    }
}
```

---

## 🔄 **Shared Database Implementation**

### **1. Tenant-Aware Entities**

```kotlin
@Entity
@Table(name = "coaching_sessions")
@FilterDef(name = "tenantFilter", parameters = [ParamDef(name = "tenantId", type = "string")])
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
class CoachingSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "tenant_id", nullable = false)
    val tenantId: String,
    
    @Column(name = "player_name")
    val playerName: String,
    
    @Column(name = "moves", columnDefinition = "text")
    val moves: String,
    
    @Column(name = "ai_advice", columnDefinition = "text")
    val aiAdvice: String?,
    
    @Column(name = "player_elo")
    val playerElo: Int?,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "session_status")
    val status: SessionStatus = SessionStatus.ACTIVE,
    
    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @UpdateTimestamp
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // Ensure tenant ID is always set
    @PrePersist
    @PreUpdate
    fun validateTenant() {
        val currentTenant = TenantContext.getCurrentTenant()
        if (tenantId != currentTenant) {
            throw TenantViolationException(
                "Entity tenant ID ($tenantId) doesn't match current tenant ($currentTenant)"
            )
        }
    }
}

enum class SessionStatus { ACTIVE, COMPLETED, CANCELLED }

@Entity
@Table(name = "player_profiles")
@FilterDef(name = "tenantFilter", parameters = [ParamDef(name = "tenantId", type = "string")])
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
class PlayerProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "tenant_id", nullable = false)
    val tenantId: String,
    
    @Column(name = "username", unique = true)
    val username: String,
    
    @Column(name = "email")
    val email: String?,
    
    @Column(name = "current_elo")
    val currentElo: Int = 1200,
    
    @Column(name = "preferred_time_control")
    val preferredTimeControl: String = "10+0",
    
    @Column(name = "coaching_preferences", columnDefinition = "jsonb")
    val coachingPreferences: String = "{}",
    
    @OneToMany(mappedBy = "playerName", fetch = FetchType.LAZY)
    val coachingSessions: List<CoachingSession> = emptyList(),
    
    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

### **2. Automatic Tenant Filtering**

```kotlin
@Component
class TenantFilterInterceptor : Interceptor {
    
    override fun onLoad(
        entity: Any,
        id: Serializable,
        state: Array<Any?>,
        propertyNames: Array<String>,
        types: Array<Type>
    ): Boolean {
        enableTenantFilter()
        return false
    }
    
    private fun enableTenantFilter() {
        val currentTenant = TenantContext.getCurrentTenant()
        if (currentTenant != null) {
            val session = entityManager.unwrap(Session::class.java)
            session.enableFilter("tenantFilter")
                .setParameter("tenantId", currentTenant)
        }
    }
}

@Service
@Transactional
class TenantAwareCoachingService(
    private val coachingSessionRepository: CoachingSessionRepository,
    private val playerProfileRepository: PlayerProfileRepository,
    private val chessCoachingService: ChessCoachingService
) {
    
    suspend fun createCoachingSession(request: CoachingSessionRequest): CoachingSession {
        val tenantId = TenantContext.getCurrentTenant()
            ?: throw TenantContextException("No tenant context")
        
        // Automatically include tenant ID
        val session = CoachingSession(
            tenantId = tenantId,
            playerName = request.playerName,
            moves = request.moves,
            aiAdvice = null,  // Will be filled after AI analysis
            playerElo = request.playerElo
        )
        
        // Generate AI coaching advice
        val advice = chessCoachingService.analyzePosition(
            screenshot = request.screenshot,
            moves = request.moves
        )
        
        val sessionWithAdvice = session.copy(aiAdvice = advice)
        
        // Save with automatic tenant filtering
        return coachingSessionRepository.save(sessionWithAdvice)
    }
    
    fun getPlayerSessions(playerName: String): List<CoachingSession> {
        // Repository automatically filters by current tenant
        return coachingSessionRepository.findByPlayerName(playerName)
    }
    
    fun getTenantStatistics(): TenantStatistics {
        val tenantId = TenantContext.getCurrentTenant()
            ?: throw TenantContextException("No tenant context")
        
        // All queries automatically filtered by tenant
        val totalSessions = coachingSessionRepository.count()
        val activePlayers = playerProfileRepository.countByCreatedAtAfter(
            LocalDateTime.now().minusDays(30)
        )
        val averageElo = playerProfileRepository.findAverageElo()
        
        return TenantStatistics(
            tenantId = tenantId,
            totalSessions = totalSessions,
            activePlayers = activePlayers,
            averageElo = averageElo,
            generatedAt = LocalDateTime.now()
        )
    }
}
```

### **3. Tenant-Safe Repository Layer**

```kotlin
@Repository
interface CoachingSessionRepository : JpaRepository<CoachingSession, Long> {
    
    // These methods automatically filtered by @Filter annotation
    fun findByPlayerName(playerName: String): List<CoachingSession>
    
    fun findByStatus(status: SessionStatus): List<CoachingSession>
    
    fun findByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): List<CoachingSession>
    
    @Query("SELECT COUNT(c) FROM CoachingSession c WHERE c.status = 'COMPLETED'")
    fun countCompletedSessions(): Long
    
    // Custom query with explicit tenant check (extra safety)
    @Query("""
        SELECT c FROM CoachingSession c 
        WHERE c.tenantId = :tenantId 
        AND c.playerName = :playerName 
        AND c.createdAt >= :since
    """)
    fun findRecentPlayerSessions(
        @Param("tenantId") tenantId: String,
        @Param("playerName") playerName: String,
        @Param("since") since: LocalDateTime
    ): List<CoachingSession>
}

@Repository
interface PlayerProfileRepository : JpaRepository<PlayerProfile, Long> {
    
    fun findByUsername(username: String): PlayerProfile?
    
    fun findByEmail(email: String): PlayerProfile?
    
    fun countByCreatedAtAfter(date: LocalDateTime): Long
    
    @Query("SELECT AVG(p.currentElo) FROM PlayerProfile p")
    fun findAverageElo(): Double
    
    @Query("SELECT p FROM PlayerProfile p WHERE p.currentElo >= :minElo")
    fun findPlayersAboveElo(@Param("minElo") minElo: Int): List<PlayerProfile>
}
```

---

## 🔐 **Security & Authentication**

### **1. Tenant-Aware JWT Authentication**

```kotlin
data class TenantAwareJwtToken(
    val userId: String,
    val tenantId: String,
    val roles: List<String>,
    val permissions: List<String>,
    val iat: Long,
    val exp: Long
)

@Service
class TenantAwareJwtService(
    @Value("\${app.jwt.secret}") private val jwtSecret: String,
    @Value("\${app.jwt.expiration}") private val jwtExpiration: Long
) {
    
    fun generateToken(user: User, tenant: TenantConfiguration): String {
        val claims = mapOf(
            "userId" to user.id,
            "tenantId" to tenant.id,
            "tenantName" to tenant.name,
            "roles" to user.roles.map { it.name },
            "permissions" to user.getPermissionsForTenant(tenant.id)
        )
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(user.username)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact()
    }
    
    fun validateTokenAndExtractTenant(token: String): TenantAwareJwtToken? {
        return try {
            val claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .body
            
            TenantAwareJwtToken(
                userId = claims["userId"] as String,
                tenantId = claims["tenantId"] as String,
                roles = claims["roles"] as List<String>,
                permissions = claims["permissions"] as List<String>,
                iat = claims.issuedAt.time,
                exp = claims.expiration.time
            )
        } catch (e: Exception) {
            null
        }
    }
}

@Component
class TenantAuthenticationFilter : OncePerRequestFilter() {
    
    @Autowired
    private lateinit var jwtService: TenantAwareJwtService
    
    @Autowired
    private lateinit var tenantService: TenantService
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractTokenFromRequest(request)
        
        if (token != null) {
            val jwtToken = jwtService.validateTokenAndExtractTenant(token)
            
            if (jwtToken != null && tenantService.isTenantActive(jwtToken.tenantId)) {
                // Set tenant context
                TenantContext.setCurrentTenant(jwtToken.tenantId)
                
                // Set security context
                val authentication = TenantAwareAuthenticationToken(
                    principal = jwtToken.userId,
                    credentials = null,
                    authorities = jwtToken.roles.map { SimpleGrantedAuthority("ROLE_$it") },
                    tenantId = jwtToken.tenantId
                )
                
                SecurityContextHolder.getContext().authentication = authentication
            }
        }
        
        filterChain.doFilter(request, response)
    }
    
    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
    }
}

class TenantAwareAuthenticationToken(
    private val principal: Any,
    private val credentials: Any?,
    authorities: Collection<GrantedAuthority>,
    val tenantId: String
) : AbstractAuthenticationToken(authorities) {
    
    init {
        isAuthenticated = true
    }
    
    override fun getCredentials(): Any? = credentials
    override fun getPrincipal(): Any = principal
}
```

### **2. Role-Based Access Control (RBAC) per Tenant**

```kotlin
@Entity
@Table(name = "tenant_users")
class TenantUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "tenant_id")
    val tenantId: String,
    
    @Column(name = "user_id")
    val userId: String,
    
    @Column(name = "email")
    val email: String,
    
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    val roles: Set<TenantRole> = setOf(TenantRole.PLAYER),
    
    @Column(name = "is_active")
    val isActive: Boolean = true,
    
    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class TenantRole(val permissions: Set<Permission>) {
    PLAYER(setOf(
        Permission.VIEW_OWN_SESSIONS,
        Permission.CREATE_SESSION
    )),
    COACH(setOf(
        Permission.VIEW_OWN_SESSIONS,
        Permission.CREATE_SESSION,
        Permission.VIEW_PLAYER_SESSIONS,
        Permission.PROVIDE_COACHING
    )),
    ADMIN(setOf(
        Permission.VIEW_ALL_SESSIONS,
        Permission.MANAGE_USERS,
        Permission.VIEW_ANALYTICS,
        Permission.MANAGE_TENANT_SETTINGS
    )),
    SUPER_ADMIN(Permission.values().toSet())
}

enum class Permission {
    VIEW_OWN_SESSIONS,
    CREATE_SESSION,
    VIEW_PLAYER_SESSIONS,
    PROVIDE_COACHING,
    VIEW_ALL_SESSIONS,
    MANAGE_USERS,
    VIEW_ANALYTICS,
    MANAGE_TENANT_SETTINGS,
    EXPORT_DATA,
    DELETE_SESSIONS
}

@PreAuthorize("hasPermission(#playerName, 'VIEW_PLAYER_SESSIONS')")
@GetMapping("/sessions/{playerName}")
fun getPlayerSessions(@PathVariable playerName: String): List<CoachingSessionDto> {
    return coachingService.getPlayerSessions(playerName)
        .map { it.toDto() }
}

@Service
class TenantPermissionEvaluator : PermissionEvaluator {
    
    override fun hasPermission(
        authentication: Authentication,
        targetDomainObject: Any?,
        permission: Any
    ): Boolean {
        if (authentication !is TenantAwareAuthenticationToken) {
            return false
        }
        
        val userPermissions = getUserPermissions(authentication.tenantId, authentication.principal as String)
        val requiredPermission = Permission.valueOf(permission as String)
        
        return when (requiredPermission) {
            Permission.VIEW_PLAYER_SESSIONS -> {
                userPermissions.contains(Permission.VIEW_PLAYER_SESSIONS) ||
                userPermissions.contains(Permission.VIEW_ALL_SESSIONS)
            }
            else -> userPermissions.contains(requiredPermission)
        }
    }
    
    override fun hasPermission(
        authentication: Authentication,
        targetId: Serializable,
        targetType: String,
        permission: Any
    ): Boolean {
        // Implementation for object-level security
        return hasPermission(authentication, null, permission)
    }
    
    private fun getUserPermissions(tenantId: String, userId: String): Set<Permission> {
        // Fetch user roles and permissions for this specific tenant
        return tenantUserRepository.findByTenantIdAndUserId(tenantId, userId)
            ?.roles
            ?.flatMap { it.permissions }
            ?.toSet()
            ?: emptySet()
    }
}
```

---

## ⚙️ **Configuration Management**

### **1. Tenant-Specific Configuration**

```kotlin
@Entity
@Table(name = "tenant_configurations")
class TenantConfiguration(
    @Id
    val id: String,
    
    @Column(name = "name")
    val name: String,
    
    @Column(name = "subdomain")
    val subdomain: String,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan")
    val subscriptionPlan: SubscriptionPlan,
    
    @Column(name = "max_users")
    val maxUsers: Int,
    
    @Column(name = "max_sessions_per_month")
    val maxSessionsPerMonth: Int,
    
    @Column(name = "ai_analysis_depth")
    val aiAnalysisDepth: Int = 12,
    
    @Column(name = "available_engines", columnDefinition = "jsonb")
    val availableEngines: String = """["stockfish"]""",
    
    @Column(name = "custom_branding", columnDefinition = "jsonb")
    val customBranding: String = "{}",
    
    @Column(name = "feature_flags", columnDefinition = "jsonb")
    val featureFlags: String = "{}",
    
    @Column(name = "is_active")
    val isActive: Boolean = true,
    
    // Database connection details
    @Column(name = "db_host")
    val dbHost: String = "localhost",
    
    @Column(name = "db_port")
    val dbPort: Int = 5432,
    
    @Column(name = "db_name")
    val dbName: String,
    
    @Column(name = "db_username")
    val dbUsername: String,
    
    @Column(name = "db_password")
    val dbPassword: String,
    
    @Column(name = "max_connections")
    val maxConnections: Int = 10,
    
    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @UpdateTimestamp
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class SubscriptionPlan(
    val maxUsers: Int,
    val maxSessionsPerMonth: Int,
    val aiAnalysisDepth: Int,
    val features: Set<Feature>
) {
    FREE(
        maxUsers = 5,
        maxSessionsPerMonth = 50,
        aiAnalysisDepth = 8,
        features = setOf(Feature.BASIC_ANALYSIS)
    ),
    BASIC(
        maxUsers = 25,
        maxSessionsPerMonth = 500,
        aiAnalysisDepth = 12,
        features = setOf(Feature.BASIC_ANALYSIS, Feature.OPENING_ANALYSIS)
    ),
    PREMIUM(
        maxUsers = 100,
        maxSessionsPerMonth = 2000,
        aiAnalysisDepth = 18,
        features = setOf(
            Feature.BASIC_ANALYSIS,
            Feature.OPENING_ANALYSIS,
            Feature.ENDGAME_ANALYSIS,
            Feature.CUSTOM_BRANDING
        )
    ),
    ENTERPRISE(
        maxUsers = Int.MAX_VALUE,
        maxSessionsPerMonth = Int.MAX_VALUE,
        aiAnalysisDepth = 25,
        features = Feature.values().toSet()
    )
}

enum class Feature {
    BASIC_ANALYSIS,
    OPENING_ANALYSIS,
    ENDGAME_ANALYSIS,
    CUSTOM_BRANDING,
    ADVANCED_STATISTICS,
    API_ACCESS,
    BULK_ANALYSIS,
    TOURNAMENT_MANAGEMENT
}
```

### **2. Dynamic Configuration Loading**

```kotlin
@Service
class TenantConfigurationService(
    private val tenantConfigRepository: TenantConfigurationRepository,
    private val redisTemplate: RedisTemplate<String, String>
) {
    
    private val configCache = ConcurrentHashMap<String, TenantConfiguration>()
    private val logger = LoggerFactory.getLogger(TenantConfigurationService::class.java)
    
    fun getTenantConfiguration(tenantId: String): TenantConfiguration? {
        // Try cache first
        configCache[tenantId]?.let { return it }
        
        // Try Redis cache
        val cachedConfig = redisTemplate.opsForValue().get("tenant:config:$tenantId")
        if (cachedConfig != null) {
            val config = objectMapper.readValue(cachedConfig, TenantConfiguration::class.java)
            configCache[tenantId] = config
            return config
        }
        
        // Load from database
        val config = tenantConfigRepository.findByIdAndIsActive(tenantId, true)
        if (config != null) {
            // Cache for future requests
            configCache[tenantId] = config
            redisTemplate.opsForValue().set(
                "tenant:config:$tenantId",
                objectMapper.writeValueAsString(config),
                Duration.ofMinutes(30)
            )
        }
        
        return config
    }
    
    fun isFeatureEnabled(tenantId: String, feature: Feature): Boolean {
        val config = getTenantConfiguration(tenantId) ?: return false
        return config.subscriptionPlan.features.contains(feature)
    }
    
    fun getAnalysisDepthForTenant(tenantId: String): Int {
        val config = getTenantConfiguration(tenantId) ?: return 8
        return config.aiAnalysisDepth
    }
    
    fun validateUsageLimits(tenantId: String): UsageLimitResult {
        val config = getTenantConfiguration(tenantId)
            ?: return UsageLimitResult.TENANT_NOT_FOUND
        
        val currentUsers = tenantUserRepository.countByTenantIdAndIsActive(tenantId, true)
        val currentMonthSessions = coachingSessionRepository.countByTenantIdAndCreatedAtAfter(
            tenantId,
            LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
        )
        
        return when {
            currentUsers >= config.maxUsers -> UsageLimitResult.USER_LIMIT_EXCEEDED
            currentMonthSessions >= config.maxSessionsPerMonth -> UsageLimitResult.SESSION_LIMIT_EXCEEDED
            else -> UsageLimitResult.WITHIN_LIMITS
        }
    }
    
    @EventListener
    fun onTenantConfigurationChanged(event: TenantConfigurationChangedEvent) {
        // Invalidate cache when configuration changes
        configCache.remove(event.tenantId)
        redisTemplate.delete("tenant:config:${event.tenantId}")
        
        logger.info("Configuration cache invalidated for tenant: ${event.tenantId}")
    }
}

enum class UsageLimitResult {
    WITHIN_LIMITS,
    USER_LIMIT_EXCEEDED,
    SESSION_LIMIT_EXCEEDED,
    TENANT_NOT_FOUND
}

data class TenantConfigurationChangedEvent(val tenantId: String)
```

---

## ♟️ **Real-World Chess API Example**

### **1. Tenant-Aware Chess Coaching Controller**

```kotlin
@RestController
@RequestMapping("/api/v1/coaching")
@CrossOrigin(origins = ["*"])
class TenantAwareChessCoachingController(
    private val tenantAwareCoachingService: TenantAwareCoachingService,
    private val tenantConfigService: TenantConfigurationService,
    private val usageLimitService: UsageLimitService
) {
    
    @PostMapping("/analyze")
    @PreAuthorize("hasPermission('CREATE_SESSION')")
    suspend fun analyzePosition(
        @RequestPart("screenshot") screenshot: MultipartFile,
        @RequestPart("moves") moves: String,
        @RequestParam("playerName") playerName: String
    ): ResponseEntity<CoachingResponse> {
        val tenantId = TenantContext.getCurrentTenant()
            ?: return ResponseEntity.badRequest()
                .body(CoachingResponse.error("Tenant context required"))
        
        // Check usage limits
        val limitCheck = usageLimitService.checkSessionLimit(tenantId)
        if (limitCheck != UsageLimitResult.WITHIN_LIMITS) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(CoachingResponse.error("Usage limit exceeded: $limitCheck"))
        }
        
        // Get tenant-specific analysis depth
        val analysisDepth = tenantConfigService.getAnalysisDepthForTenant(tenantId)
        
        try {
            val session = tenantAwareCoachingService.createCoachingSession(
                CoachingSessionRequest(
                    screenshot = screenshot,
                    moves = moves,
                    playerName = playerName,
                    analysisDepth = analysisDepth
                )
            )
            
            return ResponseEntity.ok(
                CoachingResponse.success(
                    advice = session.aiAdvice ?: "Analysis completed",
                    sessionId = session.id,
                    analysisDepth = analysisDepth
                )
            )
            
        } catch (e: Exception) {
            return ResponseEntity.internalServerError()
                .body(CoachingResponse.error("Analysis failed: ${e.message}"))
        }
    }
    
    @GetMapping("/sessions")
    @PreAuthorize("hasPermission('VIEW_OWN_SESSIONS')")
    fun getPlayerSessions(
        @RequestParam("playerName") playerName: String,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "20") size: Int
    ): ResponseEntity<PagedResponse<CoachingSessionDto>> {
        val sessions = tenantAwareCoachingService.getPlayerSessions(
            playerName = playerName,
            pageable = PageRequest.of(page, size)
        )
        
        return ResponseEntity.ok(
            PagedResponse(
                content = sessions.content.map { it.toDto() },
                page = sessions.number,
                size = sessions.size,
                totalElements = sessions.totalElements,
                totalPages = sessions.totalPages
            )
        )
    }
    
    @GetMapping("/analytics")
    @PreAuthorize("hasPermission('VIEW_ANALYTICS')")
    fun getTenantAnalytics(
        @RequestParam("startDate") startDate: LocalDate,
        @RequestParam("endDate") endDate: LocalDate
    ): ResponseEntity<TenantAnalyticsDto> {
        val analytics = tenantAwareCoachingService.getTenantAnalytics(
            startDate.atStartOfDay(),
            endDate.atTime(23, 59, 59)
        )
        
        return ResponseEntity.ok(analytics.toDto())
    }
    
    @PostMapping("/bulk-analyze")
    @PreAuthorize("hasPermission('BULK_ANALYSIS')")
    suspend fun bulkAnalyzeGames(
        @RequestBody request: BulkAnalysisRequest
    ): ResponseEntity<BulkAnalysisResponse> {
        val tenantId = TenantContext.getCurrentTenant()
            ?: return ResponseEntity.badRequest()
                .body(BulkAnalysisResponse.error("Tenant context required"))
        
        // Check if tenant has bulk analysis feature
        if (!tenantConfigService.isFeatureEnabled(tenantId, Feature.BULK_ANALYSIS)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(BulkAnalysisResponse.error("Bulk analysis not available in your plan"))
        }
        
        val result = tenantAwareCoachingService.bulkAnalyzeGames(request)
        return ResponseEntity.ok(result)
    }
}

data class CoachingResponse(
    val success: Boolean,
    val advice: String?,
    val sessionId: Long?,
    val analysisDepth: Int?,
    val error: String?
) {
    companion object {
        fun success(advice: String, sessionId: Long, analysisDepth: Int) = 
            CoachingResponse(true, advice, sessionId, analysisDepth, null)
        
        fun error(message: String) = 
            CoachingResponse(false, null, null, null, message)
    }
}

data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
```

### **2. Tenant-Aware Business Logic**

```kotlin
@Service
@Transactional
class TenantAwareCoachingService(
    private val coachingSessionRepository: CoachingSessionRepository,
    private val playerProfileRepository: PlayerProfileRepository,
    private val chessCoachingService: ChessCoachingService,
    private val tenantConfigService: TenantConfigurationService,
    private val aiUsageTracker: AIUsageTracker
) {
    
    suspend fun createCoachingSession(request: CoachingSessionRequest): CoachingSession {
        val tenantId = TenantContext.getCurrentTenant()
            ?: throw TenantContextException("No tenant context available")
        
        // Validate tenant limits
        validateTenantLimits(tenantId)
        
        // Get or create player profile
        val playerProfile = getOrCreatePlayerProfile(tenantId, request.playerName)
        
        // Create session record
        val session = CoachingSession(
            tenantId = tenantId,
            playerName = request.playerName,
            moves = request.moves,
            aiAdvice = null,
            playerElo = playerProfile.currentElo,
            status = SessionStatus.ACTIVE
        )
        
        val savedSession = coachingSessionRepository.save(session)
        
        // Generate AI coaching advice with tenant-specific settings
        val advice = generateTenantSpecificAdvice(
            screenshot = request.screenshot,
            moves = request.moves,
            playerElo = playerProfile.currentElo,
            analysisDepth = request.analysisDepth
        )
        
        // Update session with advice
        val updatedSession = savedSession.copy(
            aiAdvice = advice,
            status = SessionStatus.COMPLETED
        )
        
        // Track AI usage for billing
        aiUsageTracker.recordUsage(
            tenantId = tenantId,
            analysisDepth = request.analysisDepth,
            tokens = advice?.length ?: 0
        )
        
        return coachingSessionRepository.save(updatedSession)
    }
    
    private suspend fun generateTenantSpecificAdvice(
        screenshot: MultipartFile,
        moves: String,
        playerElo: Int?,
        analysisDepth: Int
    ): String? {
        val tenantId = TenantContext.getCurrentTenant()!!
        
        // Get tenant-specific coaching configuration
        val config = tenantConfigService.getTenantConfiguration(tenantId)
        
        // Customize analysis based on tenant features
        val analysisOptions = AnalysisOptions(
            depth = analysisDepth,
            includeOpeningAnalysis = tenantConfigService.isFeatureEnabled(tenantId, Feature.OPENING_ANALYSIS),
            includeEndgameAnalysis = tenantConfigService.isFeatureEnabled(tenantId, Feature.ENDGAME_ANALYSIS),
            playerElo = playerElo,
            customPrompt = config?.customBranding?.let { extractCustomPrompt(it) }
        )
        
        return chessCoachingService.analyzePositionWithOptions(
            screenshot = screenshot,
            moves = moves,
            options = analysisOptions
        )
    }
    
    private fun getOrCreatePlayerProfile(tenantId: String, playerName: String): PlayerProfile {
        return playerProfileRepository.findByTenantIdAndUsername(tenantId, playerName)
            ?: run {
                val newProfile = PlayerProfile(
                    tenantId = tenantId,
                    username = playerName,
                    email = null,
                    currentElo = 1200,
                    preferredTimeControl = "10+0",
                    coachingPreferences = "{}"
                )
                playerProfileRepository.save(newProfile)
            }
    }
    
    fun getPlayerSessions(playerName: String, pageable: Pageable): Page<CoachingSession> {
        val tenantId = TenantContext.getCurrentTenant()
            ?: throw TenantContextException("No tenant context available")
        
        return coachingSessionRepository.findByTenantIdAndPlayerName(
            tenantId, playerName, pageable
        )
    }
    
    fun getTenantAnalytics(startDate: LocalDateTime, endDate: LocalDateTime): TenantAnalytics {
        val tenantId = TenantContext.getCurrentTenant()
            ?: throw TenantContextException("No tenant context available")
        
        val totalSessions = coachingSessionRepository.countByTenantIdAndCreatedAtBetween(
            tenantId, startDate, endDate
        )
        
        val uniquePlayers = coachingSessionRepository.countDistinctPlayersByTenantIdAndCreatedAtBetween(
            tenantId, startDate, endDate
        )
        
        val averageSessionsPerPlayer = if (uniquePlayers > 0) totalSessions.toDouble() / uniquePlayers else 0.0
        
        val popularTimeControls = coachingSessionRepository.findPopularTimeControlsByTenant(tenantId)
        
        val eloDistribution = playerProfileRepository.getEloDistributionByTenant(tenantId)
        
        return TenantAnalytics(
            tenantId = tenantId,
            periodStart = startDate,
            periodEnd = endDate,
            totalSessions = totalSessions,
            uniquePlayers = uniquePlayers,
            averageSessionsPerPlayer = averageSessionsPerPlayer,
            popularTimeControls = popularTimeControls,
            eloDistribution = eloDistribution,
            generatedAt = LocalDateTime.now()
        )
    }
    
    suspend fun bulkAnalyzeGames(request: BulkAnalysisRequest): BulkAnalysisResponse {
        val tenantId = TenantContext.getCurrentTenant()
            ?: throw TenantContextException("No tenant context available")
        
        val results = mutableListOf<GameAnalysisResult>()
        var successCount = 0
        var errorCount = 0
        
        for (game in request.games) {
            try {
                val analysis = chessCoachingService.analyzeGameMoves(
                    moves = game.moves,
                    playerName = game.playerName,
                    timeControl = game.timeControl
                )
                
                results.add(
                    GameAnalysisResult(
                        gameId = game.id,
                        success = true,
                        analysis = analysis,
                        error = null
                    )
                )
                successCount++
                
            } catch (e: Exception) {
                results.add(
                    GameAnalysisResult(
                        gameId = game.id,
                        success = false,
                        analysis = null,
                        error = e.message
                    )
                )
                errorCount++
            }
        }
        
        return BulkAnalysisResponse(
            success = true,
            totalGames = request.games.size,
            successCount = successCount,
            errorCount = errorCount,
            results = results,
            processingTimeMs = System.currentTimeMillis() - request.startTime
        )
    }
    
    private fun validateTenantLimits(tenantId: String) {
        val limitResult = tenantConfigService.validateUsageLimits(tenantId)
        
        when (limitResult) {
            UsageLimitResult.USER_LIMIT_EXCEEDED -> 
                throw UsageLimitException("User limit exceeded for tenant: $tenantId")
            UsageLimitResult.SESSION_LIMIT_EXCEEDED -> 
                throw UsageLimitException("Session limit exceeded for tenant: $tenantId")
            UsageLimitResult.TENANT_NOT_FOUND -> 
                throw TenantNotFoundException("Tenant not found: $tenantId")
            UsageLimitResult.WITHIN_LIMITS -> 
                Unit // OK to proceed
        }
    }
}
```

---

## 🧪 **Testing Multi-Tenant Applications**

### **1. Tenant Isolation Tests**

```kotlin
@SpringBootTest
@Testcontainers
class TenantIsolationTest {
    
    @Container
    companion object {
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:13")
            .apply {
                withDatabaseName("chess_test")
                withUsername("test")
                withPassword("test")
            }
    }
    
    @Autowired
    private lateinit var tenantAwareCoachingService: TenantAwareCoachingService
    
    @Autowired
    private lateinit var coachingSessionRepository: CoachingSessionRepository
    
    @Test
    fun `should isolate data between tenants`() = runTest {
        // Setup: Create test data for two different tenants
        val tenant1 = "tenant1"
        val tenant2 = "tenant2"
        
        // Create session for tenant1
        withTenantContext(tenant1) {
            val session1 = CoachingSession(
                tenantId = tenant1,
                playerName = "Alice",
                moves = "e4 e5",
                aiAdvice = "Good opening!"
            )
            coachingSessionRepository.save(session1)
        }
        
        // Create session for tenant2
        withTenantContext(tenant2) {
            val session2 = CoachingSession(
                tenantId = tenant2,
                playerName = "Bob",
                moves = "d4 d5",
                aiAdvice = "Solid move!"
            )
            coachingSessionRepository.save(session2)
        }
        
        // Test: Verify tenant1 can only see their data
        withTenantContext(tenant1) {
            val tenant1Sessions = coachingSessionRepository.findAll()
            assertThat(tenant1Sessions).hasSize(1)
            assertThat(tenant1Sessions[0].playerName).isEqualTo("Alice")
            assertThat(tenant1Sessions[0].tenantId).isEqualTo(tenant1)
        }
        
        // Test: Verify tenant2 can only see their data
        withTenantContext(tenant2) {
            val tenant2Sessions = coachingSessionRepository.findAll()
            assertThat(tenant2Sessions).hasSize(1)
            assertThat(tenant2Sessions[0].playerName).isEqualTo("Bob")
            assertThat(tenant2Sessions[0].tenantId).isEqualTo(tenant2)
        }
    }
    
    @Test
    fun `should prevent cross-tenant data access`() = runTest {
        val tenant1 = "tenant1"
        val tenant2 = "tenant2"
        
        // Setup: Create session in tenant1
        val sessionId = withTenantContext(tenant1) {
            val session = CoachingSession(
                tenantId = tenant1,
                playerName = "Alice",
                moves = "e4 e5"
            )
            coachingSessionRepository.save(session).id
        }
        
        // Test: Try to access tenant1's session from tenant2 context
        withTenantContext(tenant2) {
            val session = coachingSessionRepository.findById(sessionId)
            assertThat(session.isPresent).isFalse()
        }
    }
    
    @Test
    fun `should enforce tenant-specific configuration limits`() = runTest {
        val freeTenant = "free_tenant"
        val premiumTenant = "premium_tenant"
        
        // Setup tenant configurations
        setupTenantConfiguration(freeTenant, SubscriptionPlan.FREE)
        setupTenantConfiguration(premiumTenant, SubscriptionPlan.PREMIUM)
        
        // Test: Free tenant should have limited analysis depth
        withTenantContext(freeTenant) {
            val request = createMockCoachingRequest()
            val session = tenantAwareCoachingService.createCoachingSession(request)
            
            // Verify analysis was limited to free tier depth
            assertThat(extractAnalysisDepth(session.aiAdvice)).isLessThanOrEqualTo(8)
        }
        
        // Test: Premium tenant should have full analysis depth
        withTenantContext(premiumTenant) {
            val request = createMockCoachingRequest()
            val session = tenantAwareCoachingService.createCoachingSession(request)
            
            // Verify analysis used premium depth
            assertThat(extractAnalysisDepth(session.aiAdvice)).isGreaterThan(8)
        }
    }
    
    private suspend fun withTenantContext(tenantId: String, block: suspend () -> Unit) {
        TenantContext.setCurrentTenant(tenantId)
        try {
            block()
        } finally {
            TenantContext.clear()
        }
    }
    
    private fun setupTenantConfiguration(tenantId: String, plan: SubscriptionPlan) {
        val config = TenantConfiguration(
            id = tenantId,
            name = "Test Tenant $tenantId",
            subdomain = tenantId,
            subscriptionPlan = plan,
            maxUsers = plan.maxUsers,
            maxSessionsPerMonth = plan.maxSessionsPerMonth,
            aiAnalysisDepth = plan.aiAnalysisDepth,
            dbName = "chess_$tenantId"
        )
        tenantConfigRepository.save(config)
    }
}
```

### **2. Multi-Tenant Integration Tests**

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MultiTenantIntegrationTest {
    
    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate
    
    @Autowired
    private lateinit var jwtService: TenantAwareJwtService
    
    @Test
    fun `should handle tenant routing via subdomain`() {
        val tenant1Host = "tenant1.chesscoach.com"
        val tenant2Host = "tenant2.chesscoach.com"
        
        // Create test data for each tenant
        val user1Token = createUserTokenForTenant("user1", "tenant1")
        val user2Token = createUserTokenForTenant("user2", "tenant2")
        
        // Test tenant1 request
        val headers1 = HttpHeaders()
        headers1.set("Authorization", "Bearer $user1Token")
        headers1.set("Host", tenant1Host)
        
        val response1 = testRestTemplate.exchange(
            "/api/v1/coaching/sessions?playerName=user1",
            HttpMethod.GET,
            HttpEntity<Any>(headers1),
            PagedResponse::class.java
        )
        
        assertThat(response1.statusCode).isEqualTo(HttpStatus.OK)
        
        // Test tenant2 request
        val headers2 = HttpHeaders()
        headers2.set("Authorization", "Bearer $user2Token")
        headers2.set("Host", tenant2Host)
        
        val response2 = testRestTemplate.exchange(
            "/api/v1/coaching/sessions?playerName=user2",
            HttpMethod.GET,
            HttpEntity<Any>(headers2),
            PagedResponse::class.java
        )
        
        assertThat(response2.statusCode).isEqualTo(HttpStatus.OK)
    }
    
    @Test
    fun `should reject requests without proper tenant context`() {
        val invalidToken = "invalid.jwt.token"
        
        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $invalidToken")
        
        val response = testRestTemplate.exchange(
            "/api/v1/coaching/sessions",
            HttpMethod.GET,
            HttpEntity<Any>(headers),
            String::class.java
        )
        
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
    
    @Test
    fun `should enforce tenant-specific feature access`() {
        // Setup: Create free tier tenant
        val freeTierToken = createUserTokenForTenant("user1", "free_tenant", SubscriptionPlan.FREE)
        
        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $freeTierToken")
        
        // Test: Try to access premium feature
        val response = testRestTemplate.exchange(
            "/api/v1/coaching/bulk-analyze",
            HttpMethod.POST,
            HttpEntity(createBulkAnalysisRequest(), headers),
            String::class.java
        )
        
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.body).contains("Bulk analysis not available in your plan")
    }
}
```

### **3. Performance Tests for Multi-Tenancy**

```kotlin
@SpringBootTest
class MultiTenantPerformanceTest {
    
    @Test
    fun `should handle concurrent requests from multiple tenants`() = runTest {
        val tenantCount = 10
        val requestsPerTenant = 100
        
        val results = (1..tenantCount).map { tenantId ->
            async {
                val tenant = "tenant$tenantId"
                measureTime {
                    repeat(requestsPerTenant) {
                        withTenantContext(tenant) {
                            tenantAwareCoachingService.getPlayerSessions("player$it")
                        }
                    }
                }
            }
        }.awaitAll()
        
        // All tenants should complete within reasonable time
        val maxDuration = results.maxOf { it.inMilliseconds }
        assertThat(maxDuration).isLessThan(5000) // 5 seconds
        
        // Verify no data leakage occurred
        verifyTenantDataIntegrity()
    }
    
    @Test
    fun `should maintain performance with large number of tenants`() = runTest {
        val largeDataSet = setupLargeMultiTenantDataSet(
            tenantCount = 1000,
            sessionsPerTenant = 1000
        )
        
        val randomTenant = "tenant${Random.nextInt(1, 1001)}"
        
        val duration = measureTime {
            withTenantContext(randomTenant) {
                val sessions = coachingSessionRepository.findAll()
                assertThat(sessions).hasSize(1000) // Each tenant should see only their data
            }
        }
        
        // Should complete within reasonable time even with large dataset
        assertThat(duration.inMilliseconds).isLessThan(1000)
    }
}
```

---

## 🚀 **Deployment & Operations**

### **1. Docker Configuration for Multi-Tenancy**

```dockerfile
# Multi-tenant Docker setup
FROM openjdk:17-jdk-slim

# Create app directory
WORKDIR /app

# Copy application
COPY build/libs/chess-coaching-api-*.jar app.jar

# Environment variables for multi-tenancy
ENV SPRING_PROFILES_ACTIVE=production
ENV TENANT_DATASOURCE_STRATEGY=schema-per-tenant
ENV REDIS_HOST=redis
ENV POSTGRES_HOST=postgres

# Health check with tenant awareness
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```yaml
# docker-compose.yml for multi-tenant setup
version: '3.8'
services:
  
  chess-api:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/chess_main
      - SPRING_REDIS_HOST=redis
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    depends_on:
      - postgres
      - redis
    volumes:
      - ./logs:/app/logs
    networks:
      - chess-network
  
  postgres:
    image: postgres:13
    environment:
      - POSTGRES_DB=chess_main
      - POSTGRES_USER=chess_user
      - POSTGRES_PASSWORD=chess_pass
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    networks:
      - chess-network
    ports:
      - "5432:5432"
  
  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data
    networks:
      - chess-network
    ports:
      - "6379:6379"
  
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/nginx/ssl
    depends_on:
      - chess-api
    networks:
      - chess-network

volumes:
  postgres_data:
  redis_data:

networks:
  chess-network:
    driver: bridge
```

### **2. Kubernetes Deployment**

```yaml
# k8s-multi-tenant-deployment.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: chess-coaching-api
  labels:
    app: chess-coaching-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: chess-coaching-api
  template:
    metadata:
      labels:
        app: chess-coaching-api
    spec:
      containers:
      - name: chess-api
        image: chess-coaching/api:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: url
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: ai-secret
              key: openai-key
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5

---
apiVersion: v1
kind: Service
metadata:
  name: chess-api-service
spec:
  selector:
    app: chess-coaching-api
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer

---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: chess-api-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
  - hosts:
    - "*.chesscoach.com"
    secretName: wildcard-tls
  rules:
  - host: "*.chesscoach.com"
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: chess-api-service
            port:
              number: 80
```

### **3. Monitoring & Observability**

```kotlin
@Component
class MultiTenantMetrics {
    
    private val tenantRequestCounter = Counter.build()
        .name("tenant_requests_total")
        .help("Total requests per tenant")
        .labelNames("tenant_id", "endpoint", "status")
        .register()
    
    private val tenantDatabaseConnectionsGauge = Gauge.build()
        .name("tenant_database_connections")
        .help("Active database connections per tenant")
        .labelNames("tenant_id")
        .register()
    
    private val tenantUserCountGauge = Gauge.build()
        .name("tenant_active_users")
        .help("Active users per tenant")
        .labelNames("tenant_id")
        .register()
    
    private val aiUsageCounter = Counter.build()
        .name("tenant_ai_usage_total")
        .help("AI usage per tenant")
        .labelNames("tenant_id", "analysis_type")
        .register()
    
    fun recordRequest(tenantId: String, endpoint: String, status: String) {
        tenantRequestCounter.labels(tenantId, endpoint, status).inc()
    }
    
    fun updateDatabaseConnections(tenantId: String, connections: Double) {
        tenantDatabaseConnectionsGauge.labels(tenantId).set(connections)
    }
    
    fun updateActiveUsers(tenantId: String, users: Double) {
        tenantUserCountGauge.labels(tenantId).set(users)
    }
    
    fun recordAIUsage(tenantId: String, analysisType: String) {
        aiUsageCounter.labels(tenantId, analysisType).inc()
    }
}

@Component
class TenantHealthIndicator(
    private val tenantConfigService: TenantConfigurationService,
    private val tenantDataSourceProvider: TenantDataSourceProvider
) : HealthIndicator {
    
    override fun health(): Health {
        val healthBuilder = Health.up()
        
        try {
            val activeTenants = tenantConfigService.getActiveTenants()
            val unhealthyTenants = mutableListOf<String>()
            
            for (tenant in activeTenants) {
                try {
                    val dataSource = tenantDataSourceProvider.getDataSource(tenant.id)
                    dataSource.connection.use { connection ->
                        connection.createStatement().execute("SELECT 1")
                    }
                } catch (e: Exception) {
                    unhealthyTenants.add(tenant.id)
                }
            }
            
            healthBuilder
                .withDetail("totalTenants", activeTenants.size)
                .withDetail("healthyTenants", activeTenants.size - unhealthyTenants.size)
                .withDetail("unhealthyTenants", unhealthyTenants)
            
            return if (unhealthyTenants.isEmpty()) {
                healthBuilder.build()
            } else {
                healthBuilder.down().build()
            }
            
        } catch (e: Exception) {
            return Health.down(e).build()
        }
    }
}
```

---

## 📋 **Best Practices**

### **1. Security Best Practices**

```kotlin
// ✅ Always validate tenant context
@Service
class SecureTenantService {
    
    fun performTenantOperation(data: String) {
        val tenantId = TenantContext.getCurrentTenant()
            ?: throw SecurityException("No tenant context - possible security breach")
        
        // Always include tenant in queries
        repository.findByTenantIdAndData(tenantId, data)
    }
}

// ✅ Use prepared statements to prevent SQL injection
@Repository
class SecureRepository {
    
    @Query("SELECT c FROM CoachingSession c WHERE c.tenantId = ?1 AND c.playerName = ?2")
    fun findSessions(tenantId: String, playerName: String): List<CoachingSession>
}

// ✅ Validate tenant access in controllers
@RestController
class SecureController {
    
    @PreAuthorize("@tenantSecurityService.canAccessTenant(authentication, #tenantId)")
    @GetMapping("/tenant/{tenantId}/data")
    fun getTenantData(@PathVariable tenantId: String): ResponseEntity<TenantData> {
        // Implementation
    }
}
```

### **2. Performance Best Practices**

```kotlin
// ✅ Cache tenant configurations
@Service
class OptimizedTenantService {
    
    @Cacheable(value = ["tenantConfig"], key = "#tenantId")
    fun getTenantConfiguration(tenantId: String): TenantConfiguration? {
        return tenantRepository.findById(tenantId).orElse(null)
    }
    
    @CacheEvict(value = ["tenantConfig"], key = "#tenantId")
    fun invalidateTenantCache(tenantId: String) {
        // Cache eviction when tenant config changes
    }
}

// ✅ Use connection pooling per tenant
@Configuration
class OptimizedDataSourceConfig {
    
    @Bean
    fun tenantDataSourceProvider(): TenantDataSourceProvider {
        return CachedTenantDataSourceProvider().apply {
            defaultMaxPoolSize = 10
            defaultMinIdle = 2
            defaultMaxLifetime = Duration.ofMinutes(30)
        }
    }
}

// ✅ Implement database query optimization
@Entity
@Table(
    name = "coaching_sessions",
    indexes = [
        Index(name = "idx_tenant_player", columnList = "tenant_id, player_name"),
        Index(name = "idx_tenant_created", columnList = "tenant_id, created_at")
    ]
)
class OptimizedCoachingSession {
    // Entity definition
}
```

### **3. Data Management Best Practices**

```kotlin
// ✅ Implement tenant data backup strategy
@Service
class TenantBackupService {
    
    @Scheduled(cron = "0 2 * * * *") // Daily at 2 AM
    fun backupTenantData() {
        val activeTenants = tenantService.getActiveTenants()
        
        activeTenants.forEach { tenant ->
            try {
                createTenantBackup(tenant)
                logger.info("Backup completed for tenant: ${tenant.id}")
            } catch (e: Exception) {
                logger.error("Backup failed for tenant: ${tenant.id}", e)
                alertingService.sendBackupFailureAlert(tenant.id, e)
            }
        }
    }
    
    private fun createTenantBackup(tenant: TenantConfiguration) {
        val backupName = "tenant_${tenant.id}_${LocalDate.now()}"
        
        when (multiTenancyStrategy) {
            DATABASE_PER_TENANT -> backupTenantDatabase(tenant, backupName)
            SCHEMA_PER_TENANT -> backupTenantSchema(tenant, backupName)
            SHARED_DATABASE -> backupTenantData(tenant, backupName)
        }
    }
}

// ✅ Implement tenant data migration
@Service
class TenantMigrationService {
    
    suspend fun migrateTenant(
        fromStrategy: TenancyStrategy,
        toStrategy: TenancyStrategy,
        tenantId: String
    ): MigrationResult {
        
        val migrationId = UUID.randomUUID().toString()
        
        return try {
            // 1. Create backup before migration
            val backup = createPreMigrationBackup(tenantId)
            
            // 2. Export data from current strategy
            val exportedData = exportTenantData(tenantId, fromStrategy)
            
            // 3. Set up new tenant structure
            setupNewTenantStructure(tenantId, toStrategy)
            
            // 4. Import data to new structure
            importTenantData(tenantId, toStrategy, exportedData)
            
            // 5. Validate migration
            val validationResult = validateMigration(tenantId, fromStrategy, toStrategy)
            
            if (validationResult.isValid) {
                // 6. Update tenant configuration
                updateTenantStrategy(tenantId, toStrategy)
                
                MigrationResult.success(migrationId, validationResult)
            } else {
                // Rollback on validation failure
                rollbackMigration(tenantId, fromStrategy, backup)
                MigrationResult.failure(migrationId, "Validation failed", validationResult.errors)
            }
            
        } catch (e: Exception) {
            // Rollback on any error
            rollbackMigration(tenantId, fromStrategy, backup)
            MigrationResult.failure(migrationId, "Migration failed: ${e.message}", listOf(e.toString()))
        }
    }
}
```

### **4. Monitoring & Alerting Best Practices**

```kotlin
// ✅ Implement comprehensive tenant monitoring
@Component
class TenantMonitoringService {
    
    @EventListener
    fun handleTenantEvent(event: TenantEvent) {
        when (event) {
            is TenantCreatedEvent -> {
                metricsService.incrementTenantCount()
                alertingService.notifyTenantCreated(event.tenantId)
            }
            is TenantLimitExceededEvent -> {
                alertingService.sendLimitExceededAlert(event.tenantId, event.limitType)
            }
            is TenantDataCorruptionEvent -> {
                alertingService.sendCriticalAlert(event.tenantId, "Data corruption detected")
            }
        }
    }
    
    @Scheduled(fixedDelay = 60000) // Every minute
    fun monitorTenantHealth() {
        val tenants = tenantService.getActiveTenants()
        
        tenants.forEach { tenant ->
            val health = checkTenantHealth(tenant)
            
            if (!health.isHealthy) {
                alertingService.sendHealthAlert(tenant.id, health.issues)
            }
            
            // Update metrics
            metricsService.updateTenantHealth(tenant.id, health.score)
        }
    }
}

// ✅ Implement tenant-specific logging
@Component
class TenantAwareLogger {
    
    private val logger = LoggerFactory.getLogger(TenantAwareLogger::class.java)
    
    fun logTenantActivity(activity: String, level: LogLevel = LogLevel.INFO) {
        val tenantId = TenantContext.getCurrentTenant() ?: "unknown"
        val logMessage = "[$tenantId] $activity"
        
        when (level) {
            LogLevel.DEBUG -> logger.debug(logMessage)
            LogLevel.INFO -> logger.info(logMessage)
            LogLevel.WARN -> logger.warn(logMessage)
            LogLevel.ERROR -> logger.error(logMessage)
        }
        
        // Also send to tenant-specific log stream
        tenantLogStream.send(tenantId, logMessage, level)
    }
}
```

---

## 🐛 **Troubleshooting**

### **Common Issues & Solutions**

#### **1. Tenant Context Lost**

**Problem**: Tenant context disappears in async operations

```kotlin
// ❌ Problematic code
@Async
suspend fun processAsync() {
    val tenant = TenantContext.getCurrentTenant() // Returns null!
    // Process fails
}

// ✅ Solution: Propagate tenant context
@Async
suspend fun processAsyncCorrectly() {
    val tenant = TenantContext.getCurrentTenant()
    
    withContext(TenantContextElement(tenant)) {
        // Tenant context is available here
        performTenantOperation()
    }
}

class TenantContextElement(private val tenantId: String?) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<TenantContextElement>
    override val key: CoroutineContext.Key<*> = Key
    
    override fun toString(): String = "TenantContext($tenantId)"
}
```

#### **2. Database Connection Pool Exhaustion**

**Problem**: Too many connections created for tenants

```kotlin
// ❌ Problematic: Creates new connection for each request
class ProblematicDataSourceProvider {
    fun getDataSource(tenantId: String): DataSource {
        return HikariDataSource().apply {
            // New connection pool created each time!
        }
    }
}

// ✅ Solution: Cache and reuse connection pools
class OptimizedDataSourceProvider {
    private val dataSourceCache = ConcurrentHashMap<String, DataSource>()
    
    fun getDataSource(tenantId: String): DataSource {
        return dataSourceCache.computeIfAbsent(tenantId) { tenant ->
            createDataSourceForTenant(tenant)
        }
    }
    
    @PreDestroy
    fun cleanup() {
        dataSourceCache.values.forEach { dataSource ->
            if (dataSource is HikariDataSource) {
                dataSource.close()
            }
        }
    }
}
```

#### **3. Memory Leaks in Multi-Tenant Applications**

**Problem**: Tenant data accumulates in memory

```kotlin
// ❌ Problematic: Static caches without eviction
object ProblematicCache {
    private val tenantData = mutableMapOf<String, Any>()
    
    fun cache(tenantId: String, data: Any) {
        tenantData[tenantId] = data // Never cleaned up!
    }
}

// ✅ Solution: Use proper cache with eviction policies
@Configuration
class CacheConfiguration {
    
    @Bean
    fun tenantCacheManager(): CacheManager {
        return CaffeineCacheManager().apply {
            setCaffeine(
                Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterAccess(Duration.ofHours(1))
                    .removalListener { key, value, cause ->
                        logger.debug("Cache entry removed: $key, cause: $cause")
                    }
            )
        }
    }
}
```

#### **4. Cross-Tenant Data Leakage**

**Problem**: Data accidentally shared between tenants

```kotlin
// ❌ Dangerous: Global state
class ProblematicService {
    private var currentData: MutableList<String> = mutableListOf()
    
    fun addData(data: String) {
        currentData.add(data) // Shared across all tenants!
    }
}

// ✅ Solution: Always include tenant in operations
@Service
class SecureService {
    
    @Autowired
    private lateinit var repository: TenantAwareRepository
    
    fun addData(data: String) {
        val tenantId = TenantContext.getCurrentTenant()
            ?: throw SecurityException("No tenant context")
        
        repository.saveForTenant(tenantId, data)
    }
}
```

### **Debugging Tools**

```kotlin
@RestController
@RequestMapping("/debug")
class TenantDebugController {
    
    @GetMapping("/tenant-context")
    fun getCurrentTenantContext(): Map<String, Any?> {
        return mapOf(
            "tenantId" to TenantContext.getCurrentTenant(),
            "threadId" to Thread.currentThread().id,
            "authentication" to SecurityContextHolder.getContext().authentication?.name
        )
    }
    
    @GetMapping("/tenant/{tenantId}/health")
    fun checkTenantHealth(@PathVariable tenantId: String): TenantHealthReport {
        return TenantHealthReport(
            tenantId = tenantId,
            databaseConnectable = checkDatabaseConnection(tenantId),
            configurationValid = validateTenantConfig(tenantId),
            usageLimits = checkUsageLimits(tenantId),
            lastActivity = getLastActivity(tenantId)
        )
    }
    
    @GetMapping("/metrics/tenant/{tenantId}")
    fun getTenantMetrics(@PathVariable tenantId: String): TenantMetrics {
        return TenantMetrics(
            tenantId = tenantId,
            activeConnections = getActiveConnections(tenantId),
            requestCount = getRequestCount(tenantId),
            errorRate = getErrorRate(tenantId),
            avgResponseTime = getAverageResponseTime(tenantId)
        )
    }
}
```

---

## 🎓 **Summary**

This comprehensive multi-tenancy guide covered:

✅ **Multi-Tenancy Fundamentals** - What it is and why it matters  
✅ **Three Main Strategies** - Database per tenant, schema per tenant, shared database  
✅ **Implementation Details** - Code examples for each strategy  
✅ **Security & Authentication** - Tenant-aware JWT and RBAC  
✅ **Configuration Management** - Dynamic tenant settings  
✅ **Real Chess API Example** - Practical implementation  
✅ **Testing Strategies** - Ensuring tenant isolation  
✅ **Deployment & Operations** - Production deployment patterns  
✅ **Best Practices** - Security, performance, and monitoring  
✅ **Troubleshooting** - Common issues and solutions  

### **Key Takeaways**

1. **Choose the right strategy** based on your requirements (isolation vs cost)
2. **Always validate tenant context** to prevent data leakage
3. **Implement proper caching** to avoid performance issues
4. **Monitor tenant health** continuously
5. **Test tenant isolation** thoroughly
6. **Plan for tenant migrations** from day one

### **Next Steps**

- Implement your chosen multi-tenancy strategy
- Set up comprehensive monitoring and alerting
- Create tenant onboarding automation
- Plan for scaling to thousands of tenants
- Consider implementing tenant analytics and billing

**Multi-tenancy done right enables you to build scalable SaaS applications that can serve thousands of customers efficiently and securely! 🚀** 