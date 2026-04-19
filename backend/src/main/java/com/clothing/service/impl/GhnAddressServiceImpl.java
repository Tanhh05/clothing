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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GhnAddressServiceImpl implements AddressService {

    private final RestTemplate restTemplate;
    private final GhnProperties ghnProperties;

    public GhnAddressServiceImpl(GhnProperties ghnProperties) {
        this.restTemplate = new RestTemplate();
        this.ghnProperties = ghnProperties;
    }

    @Override
    public List<AddressUnitResponse> getProvinces() {
        JsonNode data = callGhn(HttpMethod.GET, "/province", null);
        return mapAddressUnits(data, "ProvinceID", "ProvinceName");
    }

    @Override
    public List<AddressUnitResponse> getDistricts(Long provinceId) {
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
        if (districtId == null) {
            throw new BusinessException("districtId is required", HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("district_id", districtId);
        JsonNode data = callGhn(HttpMethod.POST, "/ward", payload);
        return mapAddressUnits(data, "WardCode", "WardName");
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
