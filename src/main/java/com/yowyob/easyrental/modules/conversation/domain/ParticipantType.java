package com.yowyob.easyrental.modules.conversation.domain;

/**
 * Identifies which side of a conversation a participant represents.
 * {@code ADMIN}'s identifier is always {@code null} (singleton support pole).
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
public enum ParticipantType {
    CLIENT,
    AGENCY,
    ADMIN
}
