package com.yowyob.easyrental.modules.subscription.dto;

public record SubscriptionRemainingTimeDTO(
    long days,
    long hours,
    long minutes,
    String formattedTime,
    boolean isInfinite
) {}