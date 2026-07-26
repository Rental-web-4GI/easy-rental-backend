package com.yowyob.easyrental.modules.conversation.domain;

/**
 * Kind of conversation, deduced from the unordered pair of participant types.
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
public enum ConversationType {
    CLIENT_AGENCY,
    CLIENT_ADMIN,
    AGENCY_ADMIN;

    public static ConversationType deduce(ParticipantType x, ParticipantType y) {
        java.util.EnumSet<ParticipantType> s = java.util.EnumSet.of(x, y);
        if (s.equals(java.util.EnumSet.of(ParticipantType.CLIENT, ParticipantType.AGENCY))) {
            return CLIENT_AGENCY;
        }
        if (s.equals(java.util.EnumSet.of(ParticipantType.CLIENT, ParticipantType.ADMIN))) {
            return CLIENT_ADMIN;
        }
        return AGENCY_ADMIN;
    }
}
