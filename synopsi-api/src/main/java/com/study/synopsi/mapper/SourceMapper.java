package com.study.synopsi.mapper;

import com.study.synopsi.dto.SourceRequestDto;
import com.study.synopsi.dto.SourceResponseDto;
import com.study.synopsi.model.Source;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class SourceMapper {

    // Entity → DTO (for returning source in responses)
    @Mapping(target = "sourceType", source = "sourceType", qualifiedByName = "sourceTypeToString")
    @Mapping(target = "feedCount", expression = "java(getFeedCount(source))")
    public abstract SourceResponseDto toDto(Source source);

    // DTO → Entity (for creating new sources)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "feeds", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract Source toEntity(SourceRequestDto dto);

    // Update existing entity from DTO (for PATCH/PUT operations)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "feeds", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract void updateEntityFromDto(SourceRequestDto dto, @MappingTarget Source source);

    // Custom mapping methods

    @Named("sourceTypeToString")
    protected String sourceTypeToString(Source.SourceType sourceType) {
        return sourceType != null ? sourceType.name() : null;
    }

    protected Integer getFeedCount(Source source) {
        return source.getFeeds() != null ? source.getFeeds().size() : 0;
    }

    // Helper method for batch conversions
    public abstract List<SourceResponseDto> toDtoList(List<Source> sources);
}