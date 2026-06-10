package com.example.cvmanager.user.service;

import com.example.cvmanager.common.exception.BadRequestException;
import com.example.cvmanager.common.exception.NotFoundException;
import com.example.cvmanager.user.dto.UserCreateRequest;
import com.example.cvmanager.user.dto.UserResponse;
import com.example.cvmanager.user.dto.UserUpdateRequest;
import com.example.cvmanager.user.model.UserAccount;
import com.example.cvmanager.user.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll(Sort.by("id")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email, null);

        UserAccount user = new UserAccount(
                email,
                request.displayName().trim(),
                passwordEncoder.encode(request.password()),
                false);

        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND"));
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        UserAccount user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND"));

        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email, id);
        ensureAnAdminRemains(user, request.admin());

        user.setEmail(email);
        user.setDisplayName(request.displayName().trim());
        user.setAdmin(request.admin());
        return toResponse(userRepository.save(user));
    }

    private void ensureEmailAvailable(String email, Long currentUserId) {
        userRepository.findByEmailIgnoreCase(email)
                .filter(existing -> currentUserId == null || !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new BadRequestException("User with this email already exists", "USER_EMAIL_EXISTS");
                });
    }

    private void ensureAnAdminRemains(UserAccount user, boolean requestedAdmin) {
        if (user.isAdmin() && !requestedAdmin && userRepository.countByAdminTrue() <= 1) {
            throw new BadRequestException("At least one admin user is required", "USER_LAST_ADMIN");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toResponse(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isAdmin(),
                user.getCreatedAt());
    }
}
