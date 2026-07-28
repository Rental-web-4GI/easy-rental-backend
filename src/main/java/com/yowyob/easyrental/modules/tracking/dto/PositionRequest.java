package com.yowyob.easyrental.modules.tracking.dto;

/**
 * Incoming payload to record a GPS position for a rental.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public record PositionRequest(Double latitude, Double longitude, String source) {
}
