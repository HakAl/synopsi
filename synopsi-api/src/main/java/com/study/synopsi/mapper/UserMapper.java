package com.study.synopsi.mapper;

import com.study.synopsi.dto.UserRequestDto;
import com.study.synopsi.dto.UserResponseDto;
import com.study.synopsi.model.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class UserMapper {

    // Entity → DTO (for returning user in responses)
    @Mapping(target = "role", source = "role", qualifiedByName = "roleToString")
    @Mapping(target = "totalArticlesRead", expression = "java(getTotalArticlesRead(user))")
    @Mapping(target = "totalPreferences", expression = "java(getTotalPreferences(user))")
    public abstract UserResponseDto toDto(User user);

    // DTO → Entity (for creating new users)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", constant = "USER")
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "accountLocked", constant = "false")
    @Mapping(target = "password", ignore = true) // Will be hashed by service
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "preferences", ignore = true)
    @Mapping(target = "userPreferences", ignore = true)
    @Mapping(target = "readingHistory", ignore = true)
    @Mapping(target = "articleFeedback", ignore = true)
    @Mapping(target = "summaries", ignore = true)
    public abstract User toEntity(UserRequestDto dto);

    // Update existing entity from DTO (for PATCH/PUT operations)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true) // Username cannot be changed
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "accountLocked", ignore = true)
    @Mapping(target = "password", ignore = true) // Password updated separately
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "preferences", ignore = true)
    @Mapping(target = "userPreferences", ignore = true)
    @Mapping(target = "readingHistory", ignore = true)
    @Mapping(target = "articleFeedback", ignore = true)
    @Mapping(target = "summaries", ignore = true)
    public abstract void updateEntityFromDto(UserRequestDto dto, @MappingTarget User user);

    // Custom mapping methods

    @Named("roleToString")
    protected String roleToString(User.UserRole role) {
        return role != null ? role.name() : null;
    }

    protected Long getTotalArticlesRead(User user) {
        return user.getReadingHistory() != null ? (long) user.getReadingHistory().size() : 0L;
    }

    protected Long getTotalPreferences(User user) {
        return user.getUserPreferences() != null ? (long) user.getUserPreferences().size() : 0L;
    }

    // Helper method for batch conversions
    public abstract List<UserResponseDto> toDtoList(List<User> users);
}