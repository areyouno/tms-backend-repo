package com.tms.backend.analytics;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class JobDeliveryAnalyticsController {
    private final JobDeliveryAnalyticsService service;

    public JobDeliveryAnalyticsController(JobDeliveryAnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/time")
    public Map<String, List<JobDeliveryStatistics>> getTimeAnalytics(
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) String localePair,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String timePeriod, // year, month, week, day
            @RequestParam(required = false) Integer duration // number of hours or minutes
    ) {
        return service.getTimeAnalytics(startDate, endDate, providerId, localePair, status, projectId, timePeriod, duration);
    }
}
