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
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        String url = ghnProperties.getBaseUrl() + path;
        ResponseEntity<JsonNode> response;
        try {
            response = restTemplate.exchange(url, method, entity, JsonNode.class);
        } catch (Exception ex) {
            throw new BusinessException("Failed to call GHN API", HttpStatus.BAD_GATEWAY);
        }

        JsonNode root = response.getBody();
        if (root == null || !root.has("data")) {
            throw new BusinessException("Invalid GHN API response", HttpStatus.BAD_GATEWAY);
        }
        return root.get("data");
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
