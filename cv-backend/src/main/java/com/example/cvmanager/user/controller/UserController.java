package com.example.cvmanager.user.controller;

import com.example.cvmanager.common.security.AdminAccessService;
import com.example.cvmanager.user.dto.UserCreateRequest;
import com.example.cvmanager.user.dto.UserResponse;
import com.example.cvmanager.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AdminAccessService adminAccessService;
    private final UserService userService;

    public UserController(AdminAccessService adminAccessService, UserService userService) {
        this.adminAccessService = adminAccessService;
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> listUsers() {
        return userService.listUsers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody UserCreateRequest request) {
        adminAccessService.requireAdmin(authorizationHeader);
        return userService.createUser(request);
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }
}
