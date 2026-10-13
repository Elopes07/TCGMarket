package com.pitcc.controller;

import com.pitcc.dto.AdminCatalogCardResponse;
import com.pitcc.dto.AdminCatalogSearchRequest;
import com.pitcc.dto.AdminCatalogSummaryResponse;
import com.pitcc.dto.CatalogPageResponse;
import com.pitcc.dto.HideCardRequest;
import com.pitcc.security.AuthenticatedUser;
import com.pitcc.service.AdminCatalogService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/catalog")
public class AdminCatalogController {

  private final AdminCatalogService adminCatalogService;

  public AdminCatalogController(AdminCatalogService adminCatalogService) {
    this.adminCatalogService = adminCatalogService;
  }

  @GetMapping("/cards")
  public CatalogPageResponse<AdminCatalogCardResponse> search(
      @RequestParam(required = false) String game,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String set,
      @RequestParam(required = false) String rarity,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String visibility,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String sort) {
    return adminCatalogService.search(new AdminCatalogSearchRequest(
        game, q, set, rarity, type, visibility, page, size, sort));
  }

  @GetMapping("/summary")
  public AdminCatalogSummaryResponse summary() {
    return adminCatalogService.summary();
  }

  @PutMapping("/cards/{id}/hidden")
  public AdminCatalogCardResponse hide(
      @AuthenticationPrincipal AuthenticatedUser actor,
      @PathVariable String id,
      @Valid @RequestBody HideCardRequest request) {
    return adminCatalogService.hide(actor.id(), id, request.reason());
  }

  @DeleteMapping("/cards/{id}/hidden")
  public AdminCatalogCardResponse unhide(@PathVariable String id) {
    return adminCatalogService.unhide(id);
  }
}
