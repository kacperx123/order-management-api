package com.example.order_management_api.controller;

import com.example.order_management_api.api.UserResponse;
import com.example.order_management_api.mapper.UserMapper;
import com.example.order_management_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }
}
