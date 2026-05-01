import api from "@/services/api";

export const posDraftApi = {
  saveDraft(payload) {
    return api.put("/orders/admin/pos/draft", payload);
  },
  getDraft(terminalId) {
    return api.get("/orders/admin/pos/draft", {
      params: { terminalId }
    });
  },
  deleteDraft(terminalId) {
    return api.delete("/orders/admin/pos/draft", {
      params: { terminalId }
    });
  }
};

