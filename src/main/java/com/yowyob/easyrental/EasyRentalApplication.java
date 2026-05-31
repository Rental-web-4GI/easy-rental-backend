package com.yowyob.easyrental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Easy Rental application entry point.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
@SpringBootApplication
@EnableR2dbcAuditing
@EnableAsync
public class EasyRentalApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyRentalApplication.class, args);
    }

}
