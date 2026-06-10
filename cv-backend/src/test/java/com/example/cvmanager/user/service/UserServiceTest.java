package com.example.cvmanager.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.cvmanager.common.exception.BadRequestException;
import com.example.cvmanager.user.dto.UserCreateRequest;
import com.example.cvmanager.user.model.UserAccount;
import com.example.cvmanager.user.repository.UserRepository;

class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void createUserHashesPasswordAndReturnsSafeResponse() {
        when(userRepository.findByEmailIgnoreCase("carol@example.com")).thenReturn(Optional.empty());
        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userService.createUser(new UserCreateRequest(
                "Carol@example.com",
                "Carol Candidate",
                "carol123"));

        assertEquals("carol@example.com", response.email());
        assertEquals("Carol Candidate", response.displayName());
        assertFalse(response.admin());
        assertFalse(userCaptor.getValue().isAdmin());
        assertNotEquals("carol123", userCaptor.getValue().getPassword());
        assertTrue(passwordEncoder.matches("carol123", userCaptor.getValue().getPassword()));
    }

    @Test
    void createUserRejectsDuplicateEmailIgnoringCase() {
        when(userRepository.findByEmailIgnoreCase("alice@example.com"))
                .thenReturn(Optional.of(new UserAccount("alice@example.com", "Alice Student", "hash", false)));

        assertThrows(
                BadRequestException.class,
                () -> userService.createUser(new UserCreateRequest("Alice@Example.com", "Alice Student", "user123")));
    }
}
