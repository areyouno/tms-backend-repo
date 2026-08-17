package com.tms.backend.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record PagedResponseDTO<T>(List<T> data, PaginationMetaDTO pagination) {

    public record PaginationMetaDTO(
            int currentPage,
            int pageSize,
            long totalCount,
            int totalPages,
            boolean hasNextPage,
            boolean hasPreviousPage) {
    }

    public static <T> PagedResponseDTO<T> from(Page<T> page) {
        return new PagedResponseDTO<>(
                page.getContent(),
                new PaginationMetaDTO(
                        page.getNumber() + 1,
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.hasNext(),
                        page.hasPrevious()));
    }
}
