package com.example.zorvynassignment

import jakarta.validation.Valid
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/users")
@Validated
class UserController(
    private val userService: UserService,
) {

    @PostMapping
    @RequireRoles(UserRole.ADMIN)
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request))
    }

    @GetMapping
    @RequireRoles(UserRole.ADMIN)
    fun listUsers(): List<UserResponse> = userService.list()

    @GetMapping("/me")
    fun me(request: HttpServletRequest): UserResponse = request.currentUser().toResponse()

    @GetMapping("/{id}")
    @RequireRoles(UserRole.ADMIN)
    fun getUser(@PathVariable id: UUID): UserResponse = userService.get(id)

    @PutMapping("/{id}")
    @RequireRoles(UserRole.ADMIN)
    fun updateUser(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateUserRequest,
    ): UserResponse = userService.update(id, request)
}

@Service
class UserService(
    private val userRepository: UserRepository,
    private val clock: Clock,
) {

    fun create(request: CreateUserRequest): UserResponse {
        val now = Instant.now(clock)
        val user = AppUser(
            id = UUID.randomUUID(),
            name = request.name.trim(),
            role = request.role,
            active = request.active,
            createdAt = now,
            updatedAt = now,
        )
        return userRepository.save(user.toEntity()).toDomain().toResponse()
    }

    fun list(): List<UserResponse> = userRepository.findAll()
        .map { it.toDomain() }
        .sortedBy { it.createdAt }
        .map { it.toResponse() }

    fun get(id: UUID): UserResponse = userRepository.findById(id)
        .orElse(null)
        ?.toDomain()
        ?.toResponse()
        ?: throw NotFoundException("User $id not found")

    fun update(id: UUID, request: UpdateUserRequest): UserResponse {
        val current = userRepository.findById(id).orElse(null)?.toDomain()
            ?: throw NotFoundException("User $id not found")
        if (request.name == null && request.role == null && request.active == null) {
            throw BadRequestException("At least one field must be provided to update a user")
        }
        val updated = current.copy(
            name = request.name?.trim()?.takeIf { it.isNotBlank() } ?: current.name,
            role = request.role ?: current.role,
            active = request.active ?: current.active,
            updatedAt = Instant.now(clock),
        )
        return userRepository.save(updated.toEntity()).toDomain().toResponse()
    }
}