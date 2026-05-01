import { useConfirmDialog } from "@/composables/useConfirmDialog";

const TITLE_BY_TYPE = {
  success: "Thành công",
  error: "Lỗi",
  warning: "Cảnh báo",
  info: "Thông báo"
};

const resolvePayload = (input) => {
  if (typeof input === "string") {
    return { message: input, type: "info" };
  }
  if (input && typeof input === "object") {
    return {
      message: String(input.message || ""),
      type: String(input.type || "info").toLowerCase()
    };
  }
  return { message: "", type: "info" };
};

const show = (input, forcedType = "") => {
  const payload = resolvePayload(input);
  const type = forcedType || payload.type || "info";
  const title = TITLE_BY_TYPE[type] || TITLE_BY_TYPE.info;
  try {
    const { showAlert } = useConfirmDialog();
    void showAlert(payload.message, title);
  } catch (_error) {
    // Fallback when called too early before Pinia is ready.
    console.log(`[${title}] ${payload.message}`);
  }
};

export const ElMessage = (input) => show(input);
ElMessage.success = (message) => show(message, "success");
ElMessage.error = (message) => show(message, "error");
ElMessage.warning = (message) => show(message, "warning");
ElMessage.info = (message) => show(message, "info");

