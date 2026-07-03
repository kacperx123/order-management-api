package com.example.order_management_api.mapper;

import com.example.order_management_api.api.UserResponse;
import com.example.order_management_api.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
