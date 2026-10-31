package com.skillbridge.common;

import java.util.List;

import org.springframework.http.ResponseEntity;

/** One page of results plus the total number of matching rows. */
public record PagedResult<T>(List<T> items, long total) {

    public static final String TOTAL_HEADER = "X-Total-Count";

    /** The body stays a plain JSON array; the total travels in a header. */
    public ResponseEntity<List<T>> toResponse() {
        return ResponseEntity.ok().header(TOTAL_HEADER, String.valueOf(total)).body(items);
    }
}