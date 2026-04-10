package com.clothing.controller;

import com.clothing.dto.request.ProductUpsertRequest;
import com.clothing.dto.request.ProductBulkActionRequest;
import com.clothing.dto.response.InventoryAlertResponse;
import com.clothing.dto.response.InventoryLogResponse;
import com.clothing.dto.response.ProductImportResponse;
import com.clothing.dto.request.ProductImageRequest;
import com.clothing.dto.response.PageResponse;
import com.clothing.dto.response.ProductResponse;
import com.clothing.service.ProductService;
import com.clothing.service.UploadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final UploadService uploadService;
    private final ObjectMapper objectMapper;

    public ProductController(ProductService productService, UploadService uploadService, ObjectMapper objectMapper) {
        this.productService = productService;
        this.uploadService = uploadService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(productService.getAll(page, size, sortBy, direction, category, q));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<ProductResponse>> getRecommendations(
            @RequestParam(required = false) List<Long> productIds,
            @RequestParam(defaultValue = "4") int limit
    ) {
        return ResponseEntity.ok(productService.getRecommendations(productIds, limit));
    }

    @GetMapping("/variant-options")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> getVariantOptions(@RequestParam String type) {
        return ResponseEntity.ok(productService.getVariantOptions(type));
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductResponse>> getDeletedProducts() {
        return ResponseEntity.ok(productService.getDeletedProducts());
    }

    @PostMapping("/variant-options")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> createVariantOption(
            @RequestParam String type,
            @RequestBody Map<String, String> payload
    ) {
        String value = payload == null ? null : payload.get("value");
        String savedValue = productService.createVariantOption(type, value);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("value", savedValue));
    }

    @GetMapping("/{productKey}")
    public ResponseEntity<ProductResponse> getByKey(@PathVariable String productKey) {
        return ResponseEntity.ok(productService.getBySlugOrId(productKey));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createMultipart(
            @RequestPart("data") String data,
            @RequestPart("files") List<MultipartFile> files
    ) throws Exception {
        ProductUpsertRequest request = objectMapper.readValue(data, ProductUpsertRequest.class);
        List<String> uploadedUrls = uploadService.uploadProductFiles(files);
        request.setImages(mergeImages(request.getImages(), uploadedUrls));
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductUpsertRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateMultipart(
            @PathVariable Long id,
            @RequestPart("data") String data,
            @RequestPart("files") List<MultipartFile> files
    ) throws Exception {
        ProductUpsertRequest request = objectMapper.readValue(data, ProductUpsertRequest.class);
        List<String> uploadedUrls = uploadService.uploadProductFiles(files);
        request.setImages(mergeImages(request.getImages(), uploadedUrls));
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> restore(@PathVariable Long id) {
        return ResponseEntity.ok(productService.restore(id));
    }

    @PostMapping("/bulk/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> bulkDelete(@Valid @RequestBody ProductBulkActionRequest request) {
        int affected = productService.bulkDelete(request.getIds());
        return ResponseEntity.ok(Map.of("affected", affected));
    }

    @PostMapping("/bulk/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> bulkRestore(@Valid @RequestBody ProductBulkActionRequest request) {
        int affected = productService.bulkRestore(request.getIds());
        return ResponseEntity.ok(Map.of("affected", affected));
    }

    @PatchMapping("/bulk/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> bulkUpdateStatus(@Valid @RequestBody ProductBulkActionRequest request) {
        int affected = productService.bulkUpdateStatus(request.getIds(), request.getStatus());
        return ResponseEntity.ok(Map.of("affected", affected));
    }

    @GetMapping("/inventory-alerts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InventoryAlertResponse>> getInventoryAlerts(
            @RequestParam(defaultValue = "5") int threshold
    ) {
        return ResponseEntity.ok(productService.getLowStockAlerts(threshold));
    }

    @GetMapping("/inventory-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InventoryLogResponse>> getInventoryLogs(
            @RequestParam(required = false) Long variantId
    ) {
        return ResponseEntity.ok(productService.getInventoryLogs(variantId));
    }

    @PostMapping(value = "/import/xlsx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductImportResponse> importXlsx(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean dryRun,
            @RequestParam(defaultValue = "false") boolean upsertBySku
    ) {
        return ResponseEntity.ok(productService.importFromXlsx(file, dryRun, upsertBySku));
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        byte[] content = productService.generateImportTemplateXlsx();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=product-import-template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    private List<ProductImageRequest> mergeImages(List<ProductImageRequest> requestImages, List<String> uploadedUrls) {
        List<ProductImageRequest> merged = new ArrayList<>();
        if (requestImages != null) {
            requestImages.stream()
                    .filter(image -> image != null && image.getUrl() != null && !image.getUrl().isBlank())
                    .forEach(merged::add);
        }
        if (uploadedUrls != null) {
            uploadedUrls.forEach(url -> {
                ProductImageRequest image = new ProductImageRequest();
                image.setUrl(url);
                image.setIsMain(false);
                merged.add(image);
            });
        }
        if (!merged.isEmpty() && merged.stream().noneMatch(img -> Boolean.TRUE.equals(img.getIsMain()))) {
            merged.get(0).setIsMain(true);
        }
        return merged;
    }
}
