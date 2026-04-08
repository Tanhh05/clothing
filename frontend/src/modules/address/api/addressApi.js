import api from "@/services/api";

export const addressApi = {
  getProvinces() {
    return api.get("/address/provinces");
  },
  getDistricts(provinceId) {
    return api.get("/address/districts", { params: { provinceId } });
  },
  getWards(districtId) {
    return api.get("/address/wards", { params: { districtId } });
  }
};
