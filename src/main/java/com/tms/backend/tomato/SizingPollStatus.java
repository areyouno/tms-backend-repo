package com.tms.backend.tomato;

import com.tms.backend.dto.TomatoSizingResponse;

public record SizingPollStatus(double progressPercent, TomatoSizingResponse result) {
    public boolean isCompleted() { return result != null; }
}
