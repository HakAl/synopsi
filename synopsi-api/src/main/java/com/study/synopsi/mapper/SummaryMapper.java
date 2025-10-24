package com.study.synopsi.mapper;

import com.study.synopsi.dto.SummaryResponseDto;
import com.study.synopsi.model.Summary;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class SummaryMapper {

    // Entity → DTO (for returning summary in responses)
    @Mapping(target = "articleId", source = "article.id")
    @Mapping(target = "articleTitle", source = "article.title")
    @Mapping(target = "summaryType", source = "summaryType", qualifiedByName = "summaryTypeToString")
    @Mapping(target = "summaryLength", source = "summaryLength", qualifiedByName = "summaryLengthToString")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "isPersonalized", expression = "java(!summary.isDefaultSummary())")
    public abstract SummaryResponseDto toDto(Summary summary);

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
    protected String statusToString(Summary.SummaryStatus status) {
        return status != null ? status.name() : null;
    }

    // Helper method for batch conversions
    public abstract List<SummaryResponseDto> toDtoList(List<Summary> summaries);
}