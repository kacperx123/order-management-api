package com.example.order_management_api.controller;

import com.example.order_management_api.api.UserResponse;
import com.example.order_management_api.mapper.UserMapper;
import com.example.order_management_api.repository.UserRepository;
import com.example.order_management_api.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        CurrentUser currentUser = CurrentUser.from(jwt);
        return userRepository.findById(currentUser.id())
                .map(userMapper::toResponse)
                .orElseThrow();
    }
}
