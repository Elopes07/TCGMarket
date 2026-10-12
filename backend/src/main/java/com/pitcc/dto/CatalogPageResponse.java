package com.pitcc.dto;

import java.util.List;

public record CatalogPageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {

  public static <T> CatalogPageResponse<T> of(List<T> content, int page, int size, long total) {
    int totalPages = (int) ((total + size - 1) / size);
    return new CatalogPageResponse<>(List.copyOf(content), page, size, total, totalPages);
  }
}
