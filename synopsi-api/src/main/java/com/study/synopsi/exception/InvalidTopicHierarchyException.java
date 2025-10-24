package com.study.synopsi.exception;

public class InvalidTopicHierarchyException extends RuntimeException {
    
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