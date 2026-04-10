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

    const contentType = String(file.type || "").trim().toLowerCase();
    const fileName = String(file.name || "upload.jpg").trim();
    const fileSize = Number(file.size || 0);

    const { data } = await api.post("/uploads/presigned-url", {
      fileName,
      contentType,
      fileSize,
      folder
    });

    const headers = data?.requiredHeaders || {};
    const response = await fetch(data.uploadUrl, {
      method: data?.method || "PUT",
      headers,
      body: file
    });

    if (!response.ok) {
      throw new Error("Upload failed");
    }

    return data.fileUrl;
  }
};
