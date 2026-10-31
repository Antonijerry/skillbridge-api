package com.skillbridge.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

/** Validated paging input. Defaults: first page, 20 rows. Maximum 100 rows per page. */
public record PageParams(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    // The id is always the last sort key, so rows with equal timestamps keep a stable order across pages.
    public static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    public static final Sort OLDEST_FIRST = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
    public static final Sort BY_NAME = Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));

    public static PageParams of(Integer page, Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? DEFAULT_SIZE : size;
        if (p < 0 || s < 1 || s > MAX_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "page must be 0 or more and size must be between 1 and " + MAX_SIZE);
        }
        return new PageParams(p, s);
    }

    public Pageable pageable(Sort sort) {
        return PageRequest.of(page, size, sort);
    }
}