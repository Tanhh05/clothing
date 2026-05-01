import api from "@/services/api";

export const userAddressApi = {
  getMyAddresses() {
    return api.get("/user/addresses");
  },
  createAddress(payload) {
    return api.post("/user/addresses", payload);
  },
  updateAddress(id, payload) {
    return api.put(`/user/addresses/${id}`, payload);
  },
  deleteAddress(id) {
    return api.delete(`/user/addresses/${id}`);
  }
};
