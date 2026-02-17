package de.pamunda.nexusfin.exchange.ex_world_engine.domain;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

public enum TimeOfDay {
    MARKET_HOURS(1.0),      // 9:30-16:00 EST: 100% execution
    PRE_MARKET(0.3),        // 7:00-9:30 EST: 30% execution
    AFTER_HOURS(0.2),       // 16:00-20:00 EST: 20% execution
    OVERNIGHT(0.1),         // 20:00-7:00 EST: 10% execution
    WEEKEND(0.1);           // Saturday/Sunday: 10% execution

    private final double executionRate;

    TimeOfDay(double executionRate) {
        this.executionRate = executionRate;
    }

    public double getExecutionRate() {
        return executionRate;
    }

    public static TimeOfDay fromTimestamp(LocalDateTime timestamp) {
        DayOfWeek dayOfWeek = timestamp.getDayOfWeek();

        // Check if weekend
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return WEEKEND;
        }

        LocalTime time = timestamp.toLocalTime();

        // Market hours: 9:30-16:00
        if (time.isAfter(LocalTime.of(9, 29)) && time.isBefore(LocalTime.of(16, 1))) {
            return MARKET_HOURS;
        }

        // Pre-market: 7:00-9:30
        if (time.isAfter(LocalTime.of(6, 59)) && time.isBefore(LocalTime.of(9, 30))) {
            return PRE_MARKET;
        }

        // After-hours: 16:00-20:00
        if (time.isAfter(LocalTime.of(15, 59)) && time.isBefore(LocalTime.of(20, 1))) {
            return AFTER_HOURS;
        }

        // Overnight: 20:00-7:00
        return OVERNIGHT;
    }
}
