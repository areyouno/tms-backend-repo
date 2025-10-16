package com.tms.backend.analytics;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class JobDeliveryAnalyticsService {
    private final JobDeliveryAnalyticsRepository repo;

    public JobDeliveryAnalyticsService(JobDeliveryAnalyticsRepository repo) {
        this.repo = repo;
    }

    public Map<String, List<JobDeliveryStatistics>> getTimeAnalytics(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long providerId,
            String localePair,
            String status,
            Long projectId,
            String timePeriod,
            Integer duration) {

    LocalDateTime effectiveStart;
    LocalDateTime effectiveEnd;

        // define the time window (date range)
        if (timePeriod != null) {
            LocalDateTime now = LocalDateTime.now();
            switch (timePeriod.toUpperCase()) {
                case "YEAR" -> {
                    startDate = (startDate == null) ? now.withDayOfYear(1).with(LocalTime.MIN) : startDate;
                    endDate = (endDate == null) ? now.withDayOfYear(now.toLocalDate().lengthOfYear()).with(LocalTime.MAX) : endDate;
                }
                case "MONTH" -> {
                    startDate = (startDate == null) ? now.withDayOfMonth(1).with(LocalTime.MIN) : startDate;
                    endDate = (endDate == null) ? now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).with(LocalTime.MAX) : endDate;
                }
                case "PREVIOUS_MONTH" -> {
                    LocalDateTime firstDayPrevMonth = now.minusMonths(1).withDayOfMonth(1).with(LocalTime.MIN);
                    LocalDateTime lastDayPrevMonth = firstDayPrevMonth
                            .withDayOfMonth(firstDayPrevMonth.toLocalDate().lengthOfMonth()).with(LocalTime.MAX);
                    startDate = firstDayPrevMonth;
                    endDate = lastDayPrevMonth;
                }
                case "PREVIOUS_3_MONTHS" -> {
                    LocalDateTime firstDayThreeMonthsAgo = now.minusMonths(3)
                            .withDayOfMonth(1)
                            .with(LocalTime.MIN);
                    LocalDateTime lastDayPreviousMonth = now.minusMonths(1)
                            .withDayOfMonth(now.minusMonths(1).toLocalDate().lengthOfMonth())
                            .with(LocalTime.MAX);
                    startDate = firstDayThreeMonthsAgo;
                    endDate = lastDayPreviousMonth;
                }
                case "PREVIOUS_12_MONTHS" -> {
                    LocalDateTime firstDayTwelveMonthsAgo = now.minusMonths(12)
                            .withDayOfMonth(1)
                            .with(LocalTime.MIN);
                    LocalDateTime lastDayPreviousMonth = now.minusMonths(1)
                            .withDayOfMonth(now.minusMonths(1).toLocalDate().lengthOfMonth())
                            .with(LocalTime.MAX);
                    startDate = firstDayTwelveMonthsAgo;
                    endDate = lastDayPreviousMonth;
                }
                case "WEEK" -> {
                    startDate = (startDate == null) ? now.with(java.time.DayOfWeek.MONDAY).with(LocalTime.MIN) : startDate;
                    endDate = (endDate == null) ? now.with(java.time.DayOfWeek.SUNDAY).with(LocalTime.MAX) : endDate;
                }
                case "PREVIOUS_WEEK" -> {
                    LocalDateTime lastMonday = now.minusWeeks(1).with(java.time.DayOfWeek.MONDAY).with(LocalTime.MIN);
                    LocalDateTime lastSunday = now.minusWeeks(1).with(java.time.DayOfWeek.SUNDAY).with(LocalTime.MAX);
                    startDate = lastMonday;
                    endDate = lastSunday;
                }
                case "DAY" -> {
                    startDate = (startDate == null) ? now : startDate;
                    endDate = (endDate == null) ? now : endDate;
                }
                case "YESTERDAY" -> {
                    startDate = now.minusDays(1).with(LocalTime.MIN);
                    endDate = now.minusDays(1).with(LocalTime.MAX);
                }
                case "PREVIOUS_7_DAYS" -> {
                    startDate = now.minusDays(7).with(LocalTime.MIN);
                    endDate = now.with(LocalTime.MAX);
                }
                case "PREVIOUS_30_DAYS" -> {
                    startDate = now.minusDays(30).with(LocalTime.MIN);
                    endDate = now.with(LocalTime.MAX);
                }
                case "HOUR" -> {
                    int hours = (duration != null && duration > 0) ? duration : 1; // fallback 1 hour
                    effectiveEnd = now.truncatedTo(ChronoUnit.HOURS);
                    effectiveStart = effectiveEnd.minusHours(hours);
                    return Map.of("byHour", repo.countByHour(effectiveStart, effectiveEnd,
                            providerId, localePair, status, projectId));
                }
                case "MINUTE" -> {
                    int minutes = (duration != null && duration > 0) ? duration : 5; // fallback 5 mins
                    effectiveEnd = now.truncatedTo(ChronoUnit.MINUTES);
                    effectiveStart = effectiveEnd.minusMinutes(minutes);
                    return Map.of("byMinute", repo.countByMinute(effectiveStart, effectiveEnd,
                            providerId, localePair, status, projectId));
            }
            default -> throw new IllegalArgumentException("Unsupported granularity: " + timePeriod);
        }
        } else {
            // fallback if no granularity — require explicit dates
            if (startDate == null || endDate == null) {
                throw new IllegalArgumentException(
                        "startDate and endDate are required when granularity is not provided.");
            }
        }

        Map<String, List<JobDeliveryStatistics>> result = new HashMap<>();

        List<JobDeliveryStatistics> byDate;

        // choosing the repo query to run depending on the date granularity (by year, month, week, day)
        switch (timePeriod == null ? "MONTH" : timePeriod.toUpperCase()) {
            case "YEAR" -> byDate = repo.countByYear(startDate, endDate, providerId, localePair, status, projectId);
            case "MONTH", "PREVIOUS_MONTH", "PREVIOUS_3_MONTHS", "PREVIOUS_12_MONTHS" -> byDate = repo.countByMonth(startDate, endDate, providerId, localePair, status, projectId);
            case "WEEK", "PREVIOUS_WEEK" -> byDate = repo.countByWeek(startDate, endDate, providerId, localePair, status, projectId);
            case "DAY", "YESTERDAY", "PREVIOUS_7_DAYS", "PREVIOUS_30_DAYS" -> byDate = repo.countByDay(startDate, endDate, providerId, localePair, status, projectId);
            case "HOUR" -> byDate = repo.countByHour(startDate, endDate, providerId, localePair, status, projectId);
            case "MINUTE" -> byDate = repo.countByMinute(startDate, endDate, providerId, localePair, status, projectId);
            default -> throw new IllegalArgumentException("Unsupported timePeriod: " + timePeriod);
        }

        result.put("byDate", byDate);
        result.put("byProvider", repo.countByProvider(startDate, endDate, providerId, localePair, status, projectId));
        result.put("byLocale", repo.countByLocale(startDate, endDate, providerId, localePair, status, projectId));
        result.put("byStatus", repo.countByStatus(startDate, endDate, providerId, localePair, status, projectId));
        result.put("byProject", repo.countByProject(startDate, endDate, providerId, localePair, status, projectId));

        return result;
    }
}
