import api from "@/services/api";

export const notificationClientApi = {
  getMyNotifications() {
    return api.get("/user/notifications");
  },
  getUnreadCount() {
    return api.get("/user/notifications/unread-count");
  },
  markAsRead(id) {
    return api.patch(`/user/notifications/${id}/read`);
  },
  markAllAsRead() {
    return api.patch("/user/notifications/read-all");
  }
};
