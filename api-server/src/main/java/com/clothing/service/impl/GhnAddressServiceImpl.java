package com.clothing.service.impl;

import com.clothing.config.GhnProperties;
import com.clothing.dto.response.AddressUnitResponse;
import com.clothing.exception.BusinessException;
import com.clothing.service.AddressService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GhnAddressServiceImpl implements AddressService {
    private static final Logger log = LoggerFactory.getLogger(GhnAddressServiceImpl.class);

    private final RestTemplate restTemplate;
    private final GhnProperties ghnProperties;

    public GhnAddressServiceImpl(GhnProperties ghnProperties) {
        this.restTemplate = new RestTemplate();
        this.ghnProperties = ghnProperties;
    }

    @Override
    public List<AddressUnitResponse> getProvinces() {
        if (ghnProperties.getToken() == null || ghnProperties.getToken().isBlank()) {
            log.warn("GHN token is missing, return empty provinces list");
            return List.of();
        }
        JsonNode data = callGhn(HttpMethod.GET, "/province", null);
        return mapAddressUnits(data, "ProvinceID", "ProvinceName");
    }

    @Override
    public List<AddressUnitResponse> getDistricts(Long provinceId) {
        if (ghnProperties.getToken() == null || ghnProperties.getToken().isBlank()) {
            log.warn("GHN token is missing, return empty districts list");
            return List.of();
        }
        if (provinceId == null) {
            throw new BusinessException("provinceId is required", HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("province_id", provinceId);
        JsonNode data = callGhn(HttpMethod.POST, "/district", payload);
        return mapAddressUnits(data, "DistrictID", "DistrictName");
    }

    @Override
    public List<AddressUnitResponse> getWards(Long districtId) {
        if (ghnProperties.getToken() == null || ghnProperties.getToken().isBlank()) {
            log.warn("GHN token is missing, return empty wards list");
            return List.of();
        }
        if (districtId == null) {
            throw new BusinessException("districtId is required", HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("district_id", districtId);
        JsonNode data = callGhn(HttpMethod.POST, "/ward", payload);
        return mapAddressUnits(data, "WardCode", "WardName");
    }

    @Override
    public long getShippingFee(Long districtId, String wardCode) {
        if (districtId == null) {
            throw new BusinessException("districtId is required", HttpStatus.BAD_REQUEST);
        }
        String safeWardCode = safeTrim(wardCode);
        if (safeWardCode.isBlank()) {
            throw new BusinessException("wardCode is required", HttpStatus.BAD_REQUEST);
        }
        if (ghnProperties.getToken() == null || ghnProperties.getToken().isBlank()) {
            throw new BusinessException("GHN token is not configured", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (ghnProperties.getShopId() == null || ghnProperties.getShopId().isBlank()) {
            throw new BusinessException("GHN shop id is not configured", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("service_type_id", 2);
        payload.put("to_district_id", districtId);
        payload.put("to_ward_code", safeWardCode);
        payload.put("height", 10);
        payload.put("length", 20);
        payload.put("weight", 500);
        payload.put("width", 20);
        payload.put("insurance_value", 0);

        JsonNode root = callShippingApi(HttpMethod.POST, "/fee", payload);
        JsonNode data = root.get("data");
        if (data == null || data.isNull()) {
            throw new BusinessException("Invalid GHN shipping fee response", HttpStatus.BAD_GATEWAY);
        }
        long total = data.path("total").asLong(-1L);
        if (total < 0) {
            total = data.path("service_fee").asLong(-1L);
        }
        if (total < 0) {
            throw new BusinessException("GHN shipping fee is missing", HttpStatus.BAD_GATEWAY);
        }
        return Math.max(0L, total);
    }

    private JsonNode callGhn(HttpMethod method, String path, Object body) {
        if (ghnProperties.getToken() == null || ghnProperties.getToken().isBlank()) {
            throw new BusinessException("GHN token is not configured", HttpStatus.INTERNAL_SERVER_ERROR);
        }

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
                String rawMessage = compactBody(ex.getResponseBodyAsString()).toLowerCase();
                if (ex.getStatusCode().value() == 401 || rawMessage.contains("token is not valid")) {
                    lastException = new BusinessException("GHN token is not valid", HttpStatus.BAD_GATEWAY);
                    continue;
                }
                lastException = new BusinessException(
                        "GHN API error (" + ex.getStatusCode().value() + "): " + compactBody(ex.getResponseBodyAsString()),
                        HttpStatus.BAD_GATEWAY
                );
                throw lastException;
            } catch (Exception ex) {
                lastException = new BusinessException("Failed to call GHN API", HttpStatus.BAD_GATEWAY);
            }
        }
        if (response == null) {
            throw lastException == null ? new BusinessException("Failed to call GHN API", HttpStatus.BAD_GATEWAY) : lastException;
        }

        JsonNode root = response.getBody();
        if (root == null || !root.has("data")) {
            throw new BusinessException("Invalid GHN API response", HttpStatus.BAD_GATEWAY);
        }
        int code = root.path("code").asInt(200);
        if (code != 200) {
            String message = root.path("message").asText("GHN API error");
            throw new BusinessException(message, HttpStatus.BAD_GATEWAY);
        }
        return root.get("data");
    }

    private JsonNode callShippingApi(HttpMethod method, String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnProperties.getToken().trim());
        headers.set("ShopId", ghnProperties.getShopId().trim());
        headers.setContentType(MediaType.APPLICATION_JSON);

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

    private List<String> candidateShippingBaseUrls() {
        String primary = trimTrailingSlash(safeTrim(ghnProperties.getShippingBaseUrl()));
        if (primary.isBlank()) {
            primary = "https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order";
        }
        String alt = alternateGateway(primary);
        Set<String> urls = new LinkedHashSet<>();
        urls.add(primary);
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

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimTrailingSlash(String value) {
        String result = safeTrim(value);
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private List<AddressUnitResponse> mapAddressUnits(JsonNode data, String idField, String nameField) {
        List<AddressUnitResponse> result = new ArrayList<>();
        if (data == null || !data.isArray()) {
            return result;
        }

        for (JsonNode item : data) {
            JsonNode idNode = item.get(idField);
            JsonNode nameNode = item.get(nameField);
            if (idNode == null || nameNode == null) {
                continue;
            }
            String id = idNode.asText();
            if (id == null || id.isBlank()) {
                continue;
            }

            result.add(AddressUnitResponse.builder()
                    .id(id)
                    .name(nameNode.asText())
                    .build());
        }

        return result;
    }
}
