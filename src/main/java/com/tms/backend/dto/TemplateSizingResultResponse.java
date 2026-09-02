package com.tms.backend.dto;

public record TemplateSizingResultResponse(String jobId, String status, Result result) {
    public record Result(String fileName, TemplateTmInfo templateTm) {}

    public record TemplateTmInfo(Long tmId, String name, Integer unitCount) {}
}
