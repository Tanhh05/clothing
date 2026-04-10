package com.clothing.service;

import com.clothing.dto.request.ProductUpsertRequest;
import com.clothing.dto.response.InventoryAlertResponse;
import com.clothing.dto.response.InventoryLogResponse;
import com.clothing.dto.response.PageResponse;
import com.clothing.dto.response.ProductImportResponse;
import com.clothing.dto.response.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {

    ProductResponse create(ProductUpsertRequest request);

    ProductResponse update(Long id, ProductUpsertRequest request);

    void delete(Long id);

    ProductResponse getById(Long id);

    ProductResponse getBySlugOrId(String productKey);

    PageResponse<ProductResponse> getAll(int page, int size, String sortBy, String direction, Long category, String q);

    List<String> getVariantOptions(String type);

    String createVariantOption(String type, String value);

    List<ProductResponse> getDeletedProducts();

    ProductResponse restore(Long id);

    int bulkDelete(List<Long> ids);

    int bulkRestore(List<Long> ids);

    int bulkUpdateStatus(List<Long> ids, String status);

    List<InventoryAlertResponse> getLowStockAlerts(int threshold);

    List<InventoryLogResponse> getInventoryLogs(Long variantId);

    List<ProductResponse> getRecommendations(List<Long> productIds, int limit);

    ProductImportResponse importFromXlsx(MultipartFile file, boolean dryRun, boolean upsertBySku);

    byte[] generateImportTemplateXlsx();
}
