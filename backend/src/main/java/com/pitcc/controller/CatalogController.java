package com.pitcc.controller;

import com.pitcc.dto.CatalogCardDetailsResponse;
import com.pitcc.dto.CatalogCardResponse;
import com.pitcc.dto.CatalogFiltersResponse;
import com.pitcc.dto.CatalogPageResponse;
import com.pitcc.dto.CatalogSearchRequest;
import com.pitcc.service.CatalogSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

  private final CatalogSearchService catalogSearchService;

  public CatalogController(CatalogSearchService catalogSearchService) {
    this.catalogSearchService = catalogSearchService;
  }

  @GetMapping("/{game}/cards")
  public CatalogPageResponse<CatalogCardResponse> search(
      @PathVariable String game,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String set,
      @RequestParam(required = false) String rarity,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String sort) {
    return catalogSearchService.search(
        game, new CatalogSearchRequest(q, set, rarity, type, page, size, sort));
  }

  @GetMapping("/{game}/filters")
  public CatalogFiltersResponse filters(@PathVariable String game) {
    return catalogSearchService.filters(game);
  }

  @GetMapping("/cards/{id}")
  public CatalogCardDetailsResponse findById(@PathVariable String id) {
    return catalogSearchService.findById(id);
  }

  @GetMapping("/{game}/cards/by-external-id")
  public CatalogCardResponse findByExternalId(
      @PathVariable String game, @RequestParam String externalId) {
    return catalogSearchService.findByExternalId(game, externalId);
  }
}
