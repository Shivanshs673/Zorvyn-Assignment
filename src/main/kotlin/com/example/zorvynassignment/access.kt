package com.example.zorvynassignment

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.HandlerInterceptor
import java.time.Clock
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val CURRENT_USER_ATTR = "currentUser"
private const val USER_ID_HEADER = "X-User-Id"
private const val AUTH_HEADER = "Authorization"
private const val BEARER_PREFIX = "Bearer "

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequireRoles(vararg val value: UserRole)

fun HttpServletRequest.currentUser(): AppUser {
    return getAttribute(CURRENT_USER_ATTR) as? AppUser
        ?: throw UnauthorizedException()
}

@Component
class RoleAuthorizationInterceptor(
    private val userRepository: UserRepository,
    private val authService: AuthService,
    @Value("\${app.auth.allow-header-impersonation:false}") private val allowHeaderImpersonation: Boolean,
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (handler !is HandlerMethod) {
            return true
        }

        if (request.requestURI == "/api/auth/login" || request.requestURI == "/api/docs") {
            return true
        }

        val resolvedUser = resolveUserFromBearer(request)
            ?: if (allowHeaderImpersonation) resolveUserFromHeader(request) else null
        val user = resolvedUser ?: throw UnauthorizedException("Missing or invalid bearer token")

        if (!user.active) {
            throw ForbiddenException("User ${user.id} is inactive")
        }

        request.setAttribute(CURRENT_USER_ATTR, user)

        val allowedRoles = handler.method.getAnnotation(RequireRoles::class.java)?.value
            ?: handler.beanType.getAnnotation(RequireRoles::class.java)?.value
            ?: emptyArray()

        if (allowedRoles.isNotEmpty() && user.role !in allowedRoles) {
            throw ForbiddenException("User role ${user.role} is not allowed to access this resource")
        }

        return true
    }

    private fun resolveUserFromBearer(request: HttpServletRequest): AppUser? {
        val authHeader = request.getHeader(AUTH_HEADER)?.trim().orEmpty()
        if (authHeader.isBlank()) {
            return null
        }
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            throw UnauthorizedException("Authorization header must use Bearer token")
        }
        val token = authHeader.removePrefix(BEARER_PREFIX).trim()
        if (token.isBlank()) {
            throw UnauthorizedException("Bearer token is missing")
        }
        return authService.findUserByToken(token)
            ?: throw UnauthorizedException("Invalid or expired token")
    }

    private fun resolveUserFromHeader(request: HttpServletRequest): AppUser? {
        val userIdHeader = request.getHeader(USER_ID_HEADER)?.trim().orEmpty()
        if (userIdHeader.isBlank()) {
            return null
        }

        val userId = runCatching { UUID.fromString(userIdHeader) }
            .getOrElse { throw UnauthorizedException("X-User-Id must be a valid UUID") }

        return userRepository.findById(userId).orElse(null)?.toDomain()
            ?: throw UnauthorizedException("User $userId was not found")
    }
}

private data class RequestCounter(
    var windowStartEpochSec: Long,
    var count: Int,
)

@Component
class RateLimitInterceptor(
    private val clock: Clock,
    @Value("\${app.rate-limit.enabled:true}") private val enabled: Boolean,
    @Value("\${app.rate-limit.max-requests-per-minute:120}") private val maxRequestsPerMinute: Int,
) : HandlerInterceptor {

    private val counters = ConcurrentHashMap<String, RequestCounter>()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (!enabled || handler !is HandlerMethod) {
            return true
        }

        if (!request.requestURI.startsWith("/api/")) {
            return true
        }

        val nowEpochSec = Instant.now(clock).epochSecond
        val minuteStart = nowEpochSec - (nowEpochSec % 60)
        val key = resolveClientKey(request)

        val counter = counters.compute(key) { _, existing ->
            val current = existing ?: RequestCounter(minuteStart, 0)
            if (current.windowStartEpochSec != minuteStart) {
                current.windowStartEpochSec = minuteStart
                current.count = 0
            }
            current.count += 1
            current
        } ?: RequestCounter(minuteStart, 1)

        if (counter.count > maxRequestsPerMinute) {
            response.setHeader("Retry-After", "60")
            throw TooManyRequestsException("Rate limit exceeded: max $maxRequestsPerMinute requests per minute")
        }

        return true
    }

    private fun resolveClientKey(request: HttpServletRequest): String {
        val forwardedFor = request.getHeader("X-Forwarded-For")?.trim().orEmpty()
        if (forwardedFor.isNotBlank()) {
            return forwardedFor.split(",").first().trim()
        }
        return request.remoteAddr ?: "unknown"
    }
}

@RestController
@RequestMapping("/api/docs")
class ApiDocsController {

    @GetMapping
    fun docs(): Map<String, Any> {
        return mapOf(
            "name" to "Finance Data Processing and Access Control Backend API",
            "authentication" to mapOf(
                "type" to "Bearer token",
                "loginEndpoint" to "POST /api/auth/login",
                "demoCredentials" to listOf(
                    mapOf("username" to "admin", "password" to "admin123"),
                    mapOf("username" to "analyst", "password" to "analyst123"),
                    mapOf("username" to "viewer", "password" to "viewer123"),
                ),
            ),
            "endpoints" to listOf(
                mapOf("method" to "POST", "path" to "/api/auth/login"),
                mapOf("method" to "GET", "path" to "/api/users/me"),
                mapOf("method" to "GET", "path" to "/api/users"),
                mapOf("method" to "POST", "path" to "/api/users"),
                mapOf("method" to "PUT", "path" to "/api/users/{id}"),
                mapOf("method" to "GET", "path" to "/api/records"),
                mapOf("method" to "POST", "path" to "/api/records"),
                mapOf("method" to "GET", "path" to "/api/records/{id}"),
                mapOf("method" to "PUT", "path" to "/api/records/{id}"),
                mapOf("method" to "DELETE", "path" to "/api/records/{id}"),
                mapOf("method" to "GET", "path" to "/api/dashboard/summary?months=6"),
            ),
        )
    }
}

@Configuration
class WebConfig(
    private val roleAuthorizationInterceptor: RoleAuthorizationInterceptor,
    private val rateLimitInterceptor: RateLimitInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(rateLimitInterceptor)
        registry.addInterceptor(roleAuthorizationInterceptor)
    }
}