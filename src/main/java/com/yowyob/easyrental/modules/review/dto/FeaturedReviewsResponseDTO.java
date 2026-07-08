package com.yowyob.easyrental.modules.review.dto;

import java.util.List;

public record FeaturedReviewsResponseDTO(
    List<ReviewResponseDTO> reviews,
    double averageRating,
    long totalCount
) {}
