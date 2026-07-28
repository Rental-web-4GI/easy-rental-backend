package com.yowyob.easyrental.modules.inspection.dto;

/**
 * API I/O representation of a single checklist item.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public record InspectionItemDTO(String itemCode, String status, String note) {
}
