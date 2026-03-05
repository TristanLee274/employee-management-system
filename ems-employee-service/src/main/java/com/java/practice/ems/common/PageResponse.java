package com.java.practice.ems.common;

import org.springframework.data.domain.Page;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ PageResponse — Standard Pagination Format ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 * Uses Java 21 Records for immutability and concise definition.
 */
public record PageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean last) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
