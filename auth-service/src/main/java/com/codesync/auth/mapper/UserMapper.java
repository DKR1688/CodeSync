package com.codesync.auth.mapper;

import com.codesync.auth.dto.UserResponse;
import com.codesync.auth.entity.User;

public class UserMapper {

	public static UserResponse toDTO(User user) {
		UserResponse dto = new UserResponse();
		dto.setUserId(user.getUserId());
		dto.setUsername(user.getUsername());
		dto.setEmail(user.getEmail());
		dto.setFullName(user.getFullName());
		dto.setRole(user.getRole());
		dto.setAvatarUrl(user.getAvatarUrl());
		dto.setProvider(user.getProvider());
		dto.setBio(user.getBio());
		dto.setActive(user.isActive());
		dto.setCreatedAt(user.getCreatedAt());
		return dto;
	}
}
