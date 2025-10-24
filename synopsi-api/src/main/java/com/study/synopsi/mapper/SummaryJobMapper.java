package com.study.synopsi.mapper;

import com.study.synopsi.dto.SummaryJobResponseDto;
import com.study.synopsi.model.Summary;
import com.study.synopsi.model.SummaryJob;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class SummaryJobMapper {

    // Entity → DTO (for returning job status in responses)
    @Mapping(target = "articleId", source = "article.id")
    @Mapping(target = "articleTitle", source = "article.title")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "summaryType", source = "summaryType", qualifiedByName = "summaryTypeToString")
    @Mapping(target = "summaryLength", source = "summaryLength", qualifiedByName = "summaryLengthToString")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    public abstract SummaryJobResponseDto toDto(SummaryJob job);

    // Custom mapping methods

    @Named("summaryTypeToString")
    protected String summaryTypeToString(Summary.SummaryType summaryType) {
        return summaryType != null ? summaryType.name() : null;
    }

    @Named("summaryLengthToString")
    protected String summaryLengthToString(Summary.SummaryLength summaryLength) {
        return summaryLength != null ? summaryLength.name() : null;
    }

    @Named("statusToString")
    protected String statusToString(SummaryJob.JobStatus status) {
        return status != null ? status.name() : null;
    }

    // Helper method for batch conversions
    public abstract List<SummaryJobResponseDto> toDtoList(List<SummaryJob> jobs);
}