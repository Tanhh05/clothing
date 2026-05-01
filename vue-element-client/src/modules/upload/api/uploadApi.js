import api from "@/services/api";

export const uploadApi = {
  async uploadReviewFiles(files) {
    const list = Array.isArray(files) ? files.filter(Boolean) : [];
    if (!list.length) return [];

    const formData = new FormData();
    list.forEach((file) => formData.append("files", file));

    const { data } = await api.post("/uploads/review-images", formData, {
      headers: {
        "Content-Type": "multipart/form-data"
      }
    });
    return Array.isArray(data) ? data : [];
  },

  async uploadPublicFile(file, folder = "uploads") {
    if (!file) {
      throw new Error("File is required");
    }
    const formData = new FormData();
    formData.append("files", file);
    formData.append("folder", String(folder || "uploads"));
    try {
      const { data } = await api.post("/uploads/public-images", formData, {
        headers: {
          "Content-Type": "multipart/form-data"
        }
      });
      const url = Array.isArray(data) ? String(data[0] || "") : "";
      if (!url) {
        throw new Error("Không nhận được URL ảnh sau khi upload");
      }
      return url;
    } catch (error) {
      const status = Number(error?.response?.status || 0);
      if (status === 401) {
        throw new Error("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
      }
      if (error?.code === "ECONNABORTED") {
        throw new Error("Backend phản hồi quá chậm. Vui lòng thử lại.");
      }
      if (!error?.response) {
        throw new Error("Không kết nối được backend (localhost:8080). Kiểm tra backend đã chạy ổn định.");
      }
      const message = error?.response?.data?.message || "Upload ảnh thất bại";
      throw new Error(message);
    }
  }
};
