package com.yowyob.easyrental.modules.review.dto;

public record ReviewModerationStatsDTO(
    long publishedCount,
    long unpublishedCount
) {}
