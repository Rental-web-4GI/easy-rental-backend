package com.yowyob.easyrental.modules.vehicle.dto;



public record FonctionnalitiesDTO(
    boolean air_condition, boolean usb_input, boolean seat_belt,
    boolean audio_input, boolean child_seat, boolean bluetooth,
    boolean sleeping_bed, boolean onboard_computer, boolean gps,
    boolean luggage, boolean water, boolean additional_covers
) {}