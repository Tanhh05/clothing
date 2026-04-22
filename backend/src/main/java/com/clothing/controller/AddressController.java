package com.clothing.controller;

import com.clothing.dto.response.AddressUnitResponse;
import com.clothing.service.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/address")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/provinces")
    public ResponseEntity<List<AddressUnitResponse>> getProvinces() {
        return ResponseEntity.ok(addressService.getProvinces());
    }

    @GetMapping("/districts")
    public ResponseEntity<List<AddressUnitResponse>> getDistricts(@RequestParam Long provinceId) {
        return ResponseEntity.ok(addressService.getDistricts(provinceId));
    }

    @GetMapping("/wards")
    public ResponseEntity<List<AddressUnitResponse>> getWards(@RequestParam Long districtId) {
        return ResponseEntity.ok(addressService.getWards(districtId));
    }

    @GetMapping("/shipping-fee")
    public ResponseEntity<Map<String, Long>> getShippingFee(
            @RequestParam Long districtId,
            @RequestParam String wardCode
    ) {
        long fee = addressService.getShippingFee(districtId, wardCode);
        return ResponseEntity.ok(Map.of("fee", fee));
    }
}
