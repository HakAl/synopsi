package com.study.synopsi.exception;

import java.io.Serial;

public class InvalidTopicHierarchyException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;
    
    public InvalidTopicHierarchyException(String message) {
        super(message);
    }
    
    public static InvalidTopicHierarchyException circularReference(Long topicId, Long parentId) {
        return new InvalidTopicHierarchyException(
            String.format("Circular reference detected: Topic %d cannot have parent %d", topicId, parentId)
        );
    }
    
    public static InvalidTopicHierarchyException maxDepthExceeded(int maxDepth) {
        return new InvalidTopicHierarchyException(
            String.format("Topic hierarchy exceeds maximum depth of %d levels", maxDepth)
        );
    }
}