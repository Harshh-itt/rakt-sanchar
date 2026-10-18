package com.example.blood.dto;

import com.example.blood.exception.ApiException;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static final int MAX_PAGE_SIZE = 50;

    public static <T> PageResponse<T> of(List<T> allItems, int page, int size) {
        if (page < 0) {
            throw ApiException.badRequest("Page index must not be less than zero");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw ApiException.badRequest("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        long total = allItems.size();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        int fromIndex = page * size;
        if (fromIndex >= allItems.size()) {
            return new PageResponse<>(List.of(), page, size, total, totalPages);
        }
        int toIndex = Math.min(fromIndex + size, allItems.size());
        return new PageResponse<>(allItems.subList(fromIndex, toIndex), page, size, total, totalPages);
    }
}
