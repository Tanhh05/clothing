package com.clothing.service;

import com.clothing.dto.response.AddressUnitResponse;

import java.util.List;

public interface AddressService {

    List<AddressUnitResponse> getProvinces();

    List<AddressUnitResponse> getDistricts(Long provinceId);

    List<AddressUnitResponse> getWards(Long districtId);

    long getShippingFee(Long districtId, String wardCode);
}
