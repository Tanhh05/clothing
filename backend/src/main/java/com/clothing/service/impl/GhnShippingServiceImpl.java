package com.clothing.service.impl;

import com.clothing.config.GhnProperties;
import com.clothing.exception.BusinessException;
import com.clothing.service.GhnShippingService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class GhnShippingServiceImpl implements GhnShippingService {

    private final RestTemplate restTemplate;
    private final GhnProperties ghnProperties;

    public GhnShippingServiceImpl(GhnProperties ghnProperties) {
        this.restTemplate = new RestTemplate();
        this.ghnProperties = ghnProperties;
    }

    @Override
    public boolean canCallShippingApi() {
        return ghnProperties.getToken() != null
                && !ghnProperties.getToken().isBlank()
                && resolvePrimaryShippingBaseUrl() != null
                && !resolvePrimaryShippingBaseUrl().isBlank();
    }

    @Override
    public GhnOrderDetail getOrderDetail(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) {
            throw new BusinessException("shippingCode is required", HttpStatus.BAD_REQUEST);
        }
        if (!canCallShippingApi()) {
            throw new BusinessException("GHN shipping config is incomplete", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnProperties.getToken().trim());
        if (ghnProperties.getShopId() != null && !ghnProperties.getShopId().isBlank()) {
            headers.set("ShopId", ghnProperties.getShopId().trim());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("order_code", orderCode.trim());

        ResponseEntity<JsonNode> response;
        try {
            response = restTemplate.postForEntity(
                    ghnProperties.getShippingBaseUrl().trim() + "/detail",
                    new HttpEntity<>(payload, headers),
                    JsonNode.class
            );
        } catch (Exception ex) {
            throw new BusinessException("Failed to call GHN shipping detail API", HttpStatus.BAD_GATEWAY);
        }

        JsonNode root = response.getBody();
        JsonNode data = root == null ? null : root.get("data");
        if (data == null || data.isNull()) {
            throw new BusinessException("Invalid GHN shipping detail response", HttpStatus.BAD_GATEWAY);
        }

        String actualOrderCode = extractText(data.get("order_code"));
        String status = normalizeGhnStatus(extractText(data.get("status")));
        if (status.isBlank()) {
            throw new BusinessException("GHN response missing shipping status", HttpStatus.BAD_GATEWAY);
        }
        if (actualOrderCode.isBlank()) {
            actualOrderCode = orderCode.trim();
        }

        return new GhnOrderDetail(actualOrderCode, status);
    }

    @Override
    public String createShippingOrder(CreateShippingOrderRequest request) {
        if (request == null) {
            throw new BusinessException("GHN request is required", HttpStatus.BAD_REQUEST);
        }
        if (!canCallShippingApi()) {
            throw new BusinessException("GHN shipping config is incomplete", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (isBlank(request.toName())
                || isBlank(request.toPhone())
                || isBlank(request.toAddress())
                || isBlank(request.toDistrictName())
                || isBlank(request.toWardName())) {
            throw new BusinessException("Missing receiver info for GHN create order", HttpStatus.BAD_REQUEST);
        }

        Long districtId = resolveDistrictId(request.toProvinceName(), request.toDistrictName());
        String wardCode = resolveWardCode(districtId, request.toWardName());

        Map<String, Object> payload = new HashMap<>();
        payload.put("client_order_code", safeTrim(request.clientOrderCode()));
        payload.put("to_name", safeTrim(request.toName()));
        payload.put("to_phone", normalizePhone(request.toPhone()));
        payload.put("to_address", safeTrim(request.toAddress()));
        payload.put("to_district_id", districtId);
        payload.put("to_ward_code", wardCode);
        payload.put("service_type_id", 2);
        payload.put("payment_type_id", 2);
        payload.put("required_note", "KHONGCHOXEMHANG");
        payload.put("cod_amount", Math.max(0L, request.codAmount() == null ? 0L : request.codAmount()));
        payload.put("insurance_value", Math.max(0L, request.codAmount() == null ? 0L : request.codAmount()));
        payload.put("weight", resolveWeight(request.totalWeightGrams()));
        payload.put("length", 20);
        payload.put("width", 20);
        payload.put("height", 10);
        payload.put("content", "Don hang " + safeTrim(request.clientOrderCode()));

        JsonNode root = callShippingApi(HttpMethod.POST, "/create", payload);
        JsonNode data = root.get("data");
        String orderCode = data == null ? "" : extractText(data.get("order_code"));
        if (orderCode.isBlank()) {
            throw new BusinessException("GHN create order missing order_code", HttpStatus.BAD_GATEWAY);
        }
        return orderCode.trim();
    }

    @Override
    public String toInternalOrderStatus(String ghnStatus) {
        String normalized = normalizeGhnStatus(ghnStatus);
        return switch (normalized) {
            case "ready_to_pick", "picking", "picked" -> "CONFIRMED";
            case "storing", "sorting", "transporting", "delivering", "money_collect_transporting" -> "SHIPPED";
            case "delivered" -> "DELIVERED";
            case "cancel" -> "CANCELLED";
            case "delivery_fail", "returned", "return", "exception" -> "FAILED_DELIVERY";
            default -> "";
        };
    }

    @Override
    public String normalizeGhnStatus(String ghnStatus) {
        if (ghnStatus == null) {
            return "";
        }
        return ghnStatus.trim().toLowerCase(Locale.ROOT);
    }

    private String extractText(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("");
    }

    private JsonNode callShippingApi(HttpMethod method, String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnProperties.getToken().trim());
        if (!isBlank(ghnProperties.getShopId())) {
            headers.set("ShopId", ghnProperties.getShopId().trim());
        }

        HttpEntity<?> entity = body == null ? new HttpEntity<>(headers) : new HttpEntity<>(body, headers);
        ResponseEntity<JsonNode> response = null;
        BusinessException lastException = null;
        for (String baseUrl : candidateShippingBaseUrls()) {
            try {
                response = restTemplate.exchange(baseUrl + path, method, entity, JsonNode.class);
                lastException = null;
                break;
            } catch (HttpStatusCodeException ex) {
                lastException = new BusinessException(
                        "GHN shipping API error (" + ex.getStatusCode().value() + "): " + compactBody(ex.getResponseBodyAsString()),
                        HttpStatus.BAD_GATEWAY
                );
                if (ex.getStatusCode().value() != 401) {
                    throw lastException;
                }
            } catch (Exception ex) {
                lastException = new BusinessException("Failed to call GHN shipping API", HttpStatus.BAD_GATEWAY);
            }
        }
        if (response == null) {
            throw lastException == null
                    ? new BusinessException("Failed to call GHN shipping API", HttpStatus.BAD_GATEWAY)
                    : lastException;
        }

        JsonNode root = response.getBody();
        if (root == null) {
            throw new BusinessException("Invalid GHN shipping response", HttpStatus.BAD_GATEWAY);
        }
        int code = root.path("code").asInt(200);
        if (code != 200) {
            String message = root.path("message").asText("GHN shipping API error");
            throw new BusinessException(message, HttpStatus.BAD_GATEWAY);
        }
        return root;
    }

    private Long resolveDistrictId(String provinceName, String districtName) {
        JsonNode provinces = callMasterDataApi(HttpMethod.GET, "/province", null);
        Long provinceId = null;
        for (JsonNode item : provinces) {
            String currentName = extractText(item.get("ProvinceName"));
            if (equalsIgnoreCaseNormalized(currentName, provinceName)) {
                provinceId = item.path("ProvinceID").asLong(0L);
                break;
            }
        }
        if (provinceId == null || provinceId <= 0) {
            throw new BusinessException("Cannot map GHN province: " + safeTrim(provinceName), HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("province_id", provinceId);
        JsonNode districts = callMasterDataApi(HttpMethod.POST, "/district", payload);
        for (JsonNode item : districts) {
            String currentName = extractText(item.get("DistrictName"));
            if (equalsIgnoreCaseNormalized(currentName, districtName)) {
                long districtId = item.path("DistrictID").asLong(0L);
                if (districtId > 0) {
                    return districtId;
                }
            }
        }
        throw new BusinessException("Cannot map GHN district: " + safeTrim(districtName), HttpStatus.BAD_REQUEST);
    }

    private String resolveWardCode(Long districtId, String wardName) {
        if (districtId == null || districtId <= 0) {
            throw new BusinessException("districtId is invalid", HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("district_id", districtId);
        JsonNode wards = callMasterDataApi(HttpMethod.POST, "/ward", payload);
        for (JsonNode item : wards) {
            String currentName = extractText(item.get("WardName"));
            if (equalsIgnoreCaseNormalized(currentName, wardName)) {
                String wardCode = extractText(item.get("WardCode"));
                if (!wardCode.isBlank()) {
                    return wardCode;
                }
            }
        }
        throw new BusinessException("Cannot map GHN ward: " + safeTrim(wardName), HttpStatus.BAD_REQUEST);
    }

    private JsonNode callMasterDataApi(HttpMethod method, String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnProperties.getToken().trim());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> entity = body == null ? new HttpEntity<>(headers) : new HttpEntity<>(body, headers);
        ResponseEntity<JsonNode> response = null;
        BusinessException lastException = null;
        for (String baseUrl : candidateMasterDataBaseUrls()) {
            try {
                response = restTemplate.exchange(baseUrl + path, method, entity, JsonNode.class);
                lastException = null;
                break;
            } catch (HttpStatusCodeException ex) {
                lastException = new BusinessException(
                        "GHN master-data API error (" + ex.getStatusCode().value() + "): " + compactBody(ex.getResponseBodyAsString()),
                        HttpStatus.BAD_GATEWAY
                );
                if (ex.getStatusCode().value() != 401) {
                    throw lastException;
                }
            } catch (Exception ex) {
                lastException = new BusinessException("Failed to call GHN master-data API", HttpStatus.BAD_GATEWAY);
            }
        }
        if (response == null) {
            throw lastException == null
                    ? new BusinessException("Failed to call GHN master-data API", HttpStatus.BAD_GATEWAY)
                    : lastException;
        }

        JsonNode root = response.getBody();
        JsonNode data = root == null ? null : root.get("data");
        if (data == null || !data.isArray()) {
            throw new BusinessException("Invalid GHN master-data response", HttpStatus.BAD_GATEWAY);
        }
        return data;
    }

    private boolean equalsIgnoreCaseNormalized(String left, String right) {
        return safeTrim(left).equalsIgnoreCase(safeTrim(right));
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizePhone(String phone) {
        String digits = safeTrim(phone).replaceAll("[^0-9]", "");
        return digits.isBlank() ? safeTrim(phone) : digits;
    }

    private long resolveWeight(Long weight) {
        if (weight == null || weight <= 0) {
            return 500L;
        }
        return Math.max(50L, weight);
    }

    private String compactBody(String body) {
        if (body == null) {
            return "";
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        if (compact.length() > 300) {
            return compact.substring(0, 300) + "...";
        }
        return compact;
    }

    private String resolvePrimaryShippingBaseUrl() {
        String explicit = safeTrim(ghnProperties.getShippingBaseUrl());
        if (!explicit.isBlank()) {
            return trimTrailingSlash(explicit);
        }
        return trimTrailingSlash("https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order");
    }

    private List<String> candidateShippingBaseUrls() {
        String primary = resolvePrimaryShippingBaseUrl();
        String alt = alternateGateway(primary);
        Set<String> urls = new LinkedHashSet<>();
        if (!primary.isBlank()) {
            urls.add(primary);
        }
        if (!alt.isBlank()) {
            urls.add(alt);
        }
        return new ArrayList<>(urls);
    }

    private List<String> candidateMasterDataBaseUrls() {
        String primary = trimTrailingSlash(safeTrim(ghnProperties.getBaseUrl()));
        if (primary.isBlank()) {
            primary = "https://online-gateway.ghn.vn/shiip/public-api/master-data";
        }
        String alt = alternateGateway(primary);
        Set<String> urls = new LinkedHashSet<>();
        urls.add(primary);
        if (!alt.isBlank()) {
            urls.add(alt);
        }
        return new ArrayList<>(urls);
    }

    private String alternateGateway(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        if (url.contains("online-gateway.ghn.vn")) {
            return trimTrailingSlash(url.replace("online-gateway.ghn.vn", "dev-online-gateway.ghn.vn"));
        }
        if (url.contains("dev-online-gateway.ghn.vn")) {
            return trimTrailingSlash(url.replace("dev-online-gateway.ghn.vn", "online-gateway.ghn.vn"));
        }
        return "";
    }

    private String trimTrailingSlash(String value) {
        String result = safeTrim(value);
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
