package com.example.order_management_api.service;

import com.example.order_management_api.api.LoginRequest;
import com.example.order_management_api.api.RegisterRequest;
import com.example.order_management_api.api.TokenResponse;
import com.example.order_management_api.api.UserResponse;
import com.example.order_management_api.exception.EmailAlreadyUsedException;
import com.example.order_management_api.exception.InvalidCredentialsException;
import com.example.order_management_api.mapper.UserMapper;
import com.example.order_management_api.model.Role;
import com.example.order_management_api.model.User;
import com.example.order_management_api.repository.UserRepository;
import com.example.order_management_api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyUsedException(request.email());
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.USER
        );

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return jwtService.issueToken(user);
    }
}
