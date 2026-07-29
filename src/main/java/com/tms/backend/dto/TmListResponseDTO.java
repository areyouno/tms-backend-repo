package com.tms.backend.dto;

import java.util.List;

public record TmListResponseDTO(List<TranslationMemoryDTO> data, PaginationDTO pagination) {

    public record PaginationDTO(
            Integer currentPage,
            Integer pageSize,
            Integer totalCount,
            Integer totalPages,
            Boolean hasNextPage,
            Boolean hasPreviousPage
    ) {}
}
