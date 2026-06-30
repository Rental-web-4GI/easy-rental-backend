package com.yowyob.easyrental.modules.rental.application;

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
        return "Client comptoir";
    }
}
