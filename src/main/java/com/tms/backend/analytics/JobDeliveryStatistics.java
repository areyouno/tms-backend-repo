package com.tms.backend.analytics;

public interface JobDeliveryStatistics {
    String getFilterValue();
    Long getOnTime();
    Long getOverdue();
}
