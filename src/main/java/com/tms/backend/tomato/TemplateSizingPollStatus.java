package com.tms.backend.tomato;

import com.tms.backend.dto.TemplateSizingResultResponse.TemplateTmInfo;

public record TemplateSizingPollStatus(boolean completed, TemplateTmInfo templateTm) {}
