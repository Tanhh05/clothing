import api from "@/services/api";

export const adminApi = {
  getAuditLogs() {
    return api.get("/admin/audit-logs");
  }
};

