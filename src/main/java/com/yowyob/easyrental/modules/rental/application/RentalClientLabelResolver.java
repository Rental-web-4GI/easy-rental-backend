package com.yowyob.easyrental.modules.rental.application;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.rental.domain.RentalEntity;

/**
 * Resolves a human-readable client label for notifications (walk-in vs registered client).
 */
final class RentalClientLabelResolver {

    private RentalClientLabelResolver() {
    }

    static String resolve(RentalEntity rental) {
        if (rental.getClientName() != null && !rental.getClientName().isBlank()) {
            return rental.getClientName().trim();
        }
        if (rental.getClientId() != null) {
            return "Client en ligne";
        }
        return "Client comptoir";
    }

    static String resolveFromUser(UserEntity user) {
        if (user.getFullname() != null && !user.getFullname().isBlank()) {
            return user.getFullname().trim();
        }
        String first = user.getFirstname() != null ? user.getFirstname().trim() : "";
        String last = user.getLastname() != null ? user.getLastname().trim() : "";
        String joined = (first + " " + last).trim();
        if (!joined.isEmpty()) {
            return joined;
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail().trim();
        }
        return "Client en ligne";
    }
}
