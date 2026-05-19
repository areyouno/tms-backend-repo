package com.tms.backend.tomato;

import com.tms.backend.dto.TomatoSizingResponse;

public record SizingPollStatus(
    double progressPercent,
    String currentStage,
    int totalFiles,
    int processedFiles,
    int totalSegments,
    int processedSegments,
    TomatoSizingResponse result
) {
    public boolean isCompleted() { return result != null; }
}
