<template>
  <div class="orders-page client-page-shell">
    <div class="orders-container">
      <el-card class="hero-card" shadow="never">
        <div class="hero-top">
          <div>
            <p class="eyebrow">Customer Center</p>
            <h1>Đơn Hàng Của Tôi</h1>
            <p class="hero-sub">Theo dõi hành trình đơn hàng và xử lý đổi trả trong một màn hình.</p>
          </div>
          <el-button type="primary" :icon="Refresh" plain :loading="store.loading" @click="store.fetchMyOrders">Làm mới</el-button>
        </div>

        <div class="stats-grid">
          <article class="stat-card total">
            <span>Tổng đơn</span>
            <strong>{{ sortedOrders.length }}</strong>
          </article>
          <article class="stat-card progress">
            <span>Đang xử lý</span>
            <strong>{{ inProgressCount }}</strong>
          </article>
          <article class="stat-card delivered">
            <span>Đã giao</span>
            <strong>{{ deliveredCount }}</strong>
          </article>
          <article class="stat-card failed">
            <span>Lỗi/Hủy</span>
            <strong>{{ failedCount }}</strong>
          </article>
        </div>
      </el-card>

      <el-card class="filter-card" shadow="never">
        <div class="filter-grid">
          <el-input
            v-model="keyword"
            :prefix-icon="Search"
            placeholder="Tìm mã đơn, sản phẩm, SKU, địa chỉ"
            clearable
            class="search-input"
          />

          <el-segmented v-model="statusFilter" :options="statusFilterOptions" class="status-segment" />

          <el-input model-value="10 / trang" class="page-size" disabled />
        </div>
      </el-card>

      <div v-if="store.loading" class="loading-wrap" v-loading="store.loading"></div>

      <el-alert
        v-else-if="store.error"
        :title="store.error"
        type="error"
        show-icon
        :closable="false"
      />

      <el-empty
        v-else-if="sortedOrders.length === 0"
        description="Bạn chưa có đơn hàng nào"
        class="empty-card"
      >
        <router-link to="/products">
          <el-button type="primary">Mua sắm ngay</el-button>
        </router-link>
      </el-empty>

      <template v-else>
        <el-empty
          v-if="pagedOrders.length === 0"
          description="Không tìm thấy đơn phù hợp bộ lọc"
          class="empty-card"
        />

        <div v-else class="orders-stack">
          <el-card v-for="order in pagedOrders" :key="order.id" class="order-card" shadow="never">
            <div class="order-head">
              <div class="head-left">
                <strong>#{{ order.id }}</strong>
                <span>{{ formatDateTime(order.createdAt) }}</span>
              </div>
              <div class="head-right">
                <el-tag :type="statusType(order.status)" effect="dark">{{ statusLabel(order.status) }}</el-tag>
                <el-tag v-if="getReturnStatus(order.id)" :type="returnStatusType(getReturnStatus(order.id))">
                  {{ returnStatusLabel(getReturnStatus(order.id)) }}
                </el-tag>
                <p>{{ formatCurrency(order.totalPrice) }}</p>
              </div>
            </div>

            <div class="order-body">
              <section class="meta-panel">
                <p><span>Thanh toán</span><strong>{{ paymentLabel(order.paymentMethod) }}</strong></p>
                <p><span>Địa chỉ</span><strong>{{ order.address || "N/A" }}</strong></p>
                <p><span>Tổng sản phẩm</span><strong>{{ itemCount(order) }}</strong></p>
                <p><span>Mã vận đơn</span><strong>{{ order.shippingCode || "Chưa có" }}</strong></p>
                <p><span>Trạng thái GHN</span><strong>{{ shippingStatusLabel(order.shippingStatus) }}</strong></p>
              </section>

              <section class="items-panel">
                <div v-for="item in order.items || []" :key="item.id" class="item-line">
                  <div class="item-main">
                    <strong>{{ item.productName || "Sản phẩm" }}</strong>
                    <span>{{ item.sku || "-" }}</span>
                  </div>
                  <div class="item-side">
                    <el-button
                      v-if="String(order.status || '').toUpperCase() === 'DELIVERED'"
                      size="small"
                      text
                      @click="openReviewDialog(order, item)"
                    >
                      Đánh giá
                    </el-button>
                    <span>x{{ item.quantity }}</span>
                    <strong>{{ formatCurrency(item.lineTotal) }}</strong>
                  </div>
                </div>
              </section>
            </div>

            <div class="order-foot">
              <div class="foot-actions">
                <el-button plain @click="handleReorder(order)">Mua lại</el-button>
                <el-button
                  v-if="canRequestReturn(order)"
                  type="warning"
                  round
                  @click="openReturnDialog(order)"
                >
                  Trả hàng / Hoàn tiền
                </el-button>
              </div>
              <span v-if="!canRequestReturn(order) && isGhnDelivered(order)" class="return-note">
                Hết thời gian đổi trả hoặc đã có yêu cầu
              </span>
            </div>

            <div class="status-timeline">
              <span v-for="(step, idx) in sortedHistory(order.statusHistory)" :key="`${order.id}-${idx}-${step.status}`">
                {{ statusLabel(step.status) }} · {{ formatDateTime(step.changedAt) }}
              </span>
            </div>
          </el-card>
        </div>

        <div class="pagination-wrap">
          <el-pagination
            layout="total, prev, pager, next"
            :total="filteredOrders.length"
            :current-page="currentPage"
            :page-size="pageSize"
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </div>
  </div>

  <el-dialog v-model="returnDialogVisible" title="Tạo yêu cầu đổi trả" width="760px" destroy-on-close>
    <el-form label-position="top" class="return-form">
      <el-form-item label="Đơn hàng">
        <el-input :model-value="`#${returnForm.orderId || ''}`" disabled />
      </el-form-item>
      <el-form-item label="Loại yêu cầu">
        <el-radio-group v-model="returnForm.returnType">
          <el-radio-button label="REFUND">Trả hàng / Hoàn tiền</el-radio-button>
          <el-radio-button label="EXCHANGE">Đổi hàng</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="Lý do chính">
        <el-select v-model="returnForm.reasonCode" placeholder="Chọn lý do" class="w-full">
          <el-option v-for="option in returnReasonOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="Mô tả chi tiết">
        <el-input
          v-model="returnForm.reasonDetail"
          type="textarea"
          :rows="4"
          maxlength="1000"
          show-word-limit
          placeholder="Mô tả rõ lỗi/sản phẩm cần đổi trả (tối thiểu 10 ký tự)"
        />
      </el-form-item>
      <el-form-item label="Link ảnh/video minh chứng (mỗi dòng 1 link)">
        <el-input
          v-model="returnForm.evidenceUrls"
          type="textarea"
          :rows="3"
          placeholder="https://...&#10;https://..."
        />
      </el-form-item>
      <el-form-item label="Chọn sản phẩm cần đổi trả">
        <div class="return-items">
          <div v-for="item in returnForm.orderItems" :key="item.id" class="return-item-row">
            <el-checkbox v-model="item.selected" />
            <div class="return-item-meta">
              <strong>{{ item.productName || "Sản phẩm" }}</strong>
              <span>{{ item.sku || "-" }}</span>
            </div>
            <el-input-number
              v-model="item.quantity"
              :min="1"
              :max="item.maxQty"
              :disabled="!item.selected"
              controls-position="right"
              size="small"
            />
          </div>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="returnDialogVisible = false">Hủy</el-button>
      <el-button type="primary" :loading="returnSubmitting" @click="submitReturnRequest">Gửi yêu cầu</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="reviewDialogVisible" title="Đánh giá sản phẩm" width="560px" destroy-on-close>
    <el-form label-position="top">
      <el-form-item label="Sản phẩm">
        <el-input :model-value="reviewForm.productName" disabled />
      </el-form-item>
      <el-form-item label="Số sao">
        <el-rate v-model="reviewForm.rating" />
      </el-form-item>
      <el-form-item label="Nội dung">
        <el-input
          v-model="reviewForm.comment"
          type="textarea"
          :rows="4"
          maxlength="1000"
          show-word-limit
          placeholder="Mô tả trải nghiệm sử dụng sản phẩm"
        />
      </el-form-item>
      <el-form-item label="Ảnh đánh giá">
        <el-upload
          v-model:file-list="reviewUploadFiles"
          list-type="picture-card"
          :auto-upload="false"
          :multiple="true"
          :limit="5"
          accept="image/*"
          :before-upload="beforeReviewUpload"
          :on-exceed="handleReviewUploadExceed"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
        <p class="upload-note">Tối đa 5 ảnh, mỗi ảnh không quá 5MB.</p>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reviewDialogVisible = false">Hủy</el-button>
      <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">Gửi đánh giá</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { Plus, Refresh, Search } from "@element-plus/icons-vue";
import { useOrderStore } from "@/modules/order/store/orderStore";
import { orderApi } from "@/modules/order/api/orderApi";
import { reviewApi } from "@/modules/product/api/reviewApi";
import { returnApi } from "@/modules/returns/api/returnApi";
import { uploadApi } from "@/modules/upload/api/uploadApi";

const store = useOrderStore();

const keyword = ref("");
const statusFilter = ref("ALL");
const pageSize = ref(10);
const currentPage = ref(1);
const returnRequests = ref([]);
const returnDialogVisible = ref(false);
const returnSubmitting = ref(false);
const reviewDialogVisible = ref(false);
const reviewSubmitting = ref(false);
const reviewUploadFiles = ref([]);
const reviewForm = ref({
  orderId: null,
  productId: null,
  productName: "",
  rating: 5,
  comment: ""
});
const returnForm = ref({
  orderId: null,
  returnType: "REFUND",
  reasonCode: "NOT_AS_DESCRIBED",
  reasonDetail: "",
  evidenceUrls: "",
  orderItems: []
});

const returnReasonOptions = [
  { value: "NOT_AS_DESCRIBED", label: "Sản phẩm không giống mô tả" },
  { value: "DEFECTIVE", label: "Sản phẩm bị lỗi/hư hỏng" },
  { value: "WRONG_ITEM", label: "Shop giao sai sản phẩm" },
  { value: "WRONG_SIZE", label: "Sai kích cỡ" },
  { value: "CHANGE_MIND", label: "Không còn nhu cầu" },
  { value: "MISSING_PARTS", label: "Thiếu phụ kiện" },
  { value: "OTHER", label: "Lý do khác" }
];

const statusMap = {
  PENDING: "Chờ xử lý",
  PROCESSING: "Đang xử lý",
  CONFIRMED: "Đã xác nhận",
  SHIPPED: "Đang giao",
  DELIVERED: "Đã giao",
  RETURN_REQUESTED: "Đang yêu cầu đổi trả",
  CANCELLED: "Đã hủy",
  FAILED: "Thất bại",
  FAILED_INSUFFICIENT_STOCK: "Thất bại - hết hàng",
  FAILED_DELIVERY: "Giao thất bại",
  REFUNDED: "Đã hoàn tiền"
};

const statusFilterOptions = [
  { label: "Tất cả", value: "ALL" },
  { label: "Đang xử lý", value: "IN_PROGRESS" },
  { label: "Đã giao", value: "DELIVERED" },
  { label: "Đổi trả", value: "RETURN_REQUESTED" },
  { label: "Đã hủy/lỗi", value: "FAILED_GROUP" }
];

const sortedOrders = computed(() => {
  return [...store.orders].sort((a, b) => {
    const timeA = new Date(a?.createdAt || 0).getTime();
    const timeB = new Date(b?.createdAt || 0).getTime();
    if (timeA !== timeB) return timeB - timeA;
    return (b?.id || 0) - (a?.id || 0);
  });
});

const containsKeyword = (order, query) => {
  const textChunks = [
    String(order?.id || ""),
    String(order?.address || ""),
    String(order?.paymentMethod || ""),
    ...(Array.isArray(order?.items)
      ? order.items.flatMap((item) => [String(item?.productName || ""), String(item?.sku || "")])
      : [])
  ];
  return textChunks.join(" ").toLowerCase().includes(query);
};

const filteredOrders = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  return sortedOrders.value.filter((order) => {
    const orderStatus = String(order?.status || "").toUpperCase();
    const byStatus = (() => {
      if (statusFilter.value === "ALL") return true;
      if (statusFilter.value === "IN_PROGRESS") return ["PENDING", "PROCESSING", "CONFIRMED", "SHIPPED"].includes(orderStatus);
      if (statusFilter.value === "FAILED_GROUP") return ["FAILED", "FAILED_INSUFFICIENT_STOCK", "CANCELLED"].includes(orderStatus);
      return orderStatus === statusFilter.value;
    })();
    if (!byStatus) return false;
    if (!query) return true;
    return containsKeyword(order, query);
  });
});

const pagedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredOrders.value.slice(start, start + pageSize.value);
});

const inProgressCount = computed(() => {
  const progressStatuses = new Set(["PENDING", "PROCESSING", "CONFIRMED", "SHIPPED"]);
  return sortedOrders.value.filter((order) => progressStatuses.has(String(order?.status || "").toUpperCase())).length;
});

const deliveredCount = computed(() => {
  return sortedOrders.value.filter((order) => String(order?.status || "").toUpperCase() === "DELIVERED").length;
});

const failedCount = computed(() => {
  const failedStatuses = new Set(["FAILED", "FAILED_INSUFFICIENT_STOCK", "CANCELLED"]);
  return sortedOrders.value.filter((order) => failedStatuses.has(String(order?.status || "").toUpperCase())).length;
});

const formatCurrency = (amount) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(amount || 0);

const formatDateTime = (value) => {
  if (!value) return "N/A";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("vi-VN");
};

const paymentLabel = (method) => {
  if ((method || "").toUpperCase() === "COD") return "Thanh toán khi nhận hàng (COD)";
  if ((method || "").toUpperCase() === "MOMO") return "Ví điện tử MoMo";
  return method || "N/A";
};

const statusLabel = (status) => statusMap[status] || status || "N/A";

const shippingStatusLabel = (status) => {
  const map = {
    ready_to_pick: "Sẵn sàng lấy hàng",
    picking: "Đang lấy hàng",
    picked: "Đã lấy hàng",
    storing: "Đã nhập kho trung chuyển",
    sorting: "Đang phân loại",
    transporting: "Đang vận chuyển",
    delivering: "Đang giao",
    delivered: "Giao thành công",
    cancel: "Đã hủy",
    delivery_fail: "Giao thất bại",
    returned: "Đang hoàn hàng"
  };
  const normalized = String(status || "").trim().toLowerCase();
  return map[normalized] || (normalized || "N/A");
};

const statusType = (status) => {
  const value = (status || "").toUpperCase();
  if (["DELIVERED", "CONFIRMED"].includes(value)) return "success";
  if (["RETURN_REQUESTED", "SHIPPED", "PROCESSING", "PENDING"].includes(value)) return "warning";
  if (["FAILED", "FAILED_INSUFFICIENT_STOCK", "CANCELLED"].includes(value)) return "danger";
  return "info";
};

const returnStatusLabel = (status) => {
  const normalized = String(status || "").toUpperCase();
  if (normalized === "REQUESTED") return "Đã gửi yêu cầu";
  if (normalized === "UNDER_REVIEW") return "Đang xem xét";
  if (normalized === "RETURN_APPROVED") return "Đã duyệt trả hàng";
  if (normalized === "IN_TRANSIT_BACK") return "Đang gửi hàng trả";
  if (normalized === "RETURN_RECEIVED") return "Shop đã nhận hàng trả";
  if (normalized === "REFUND_PROCESSING") return "Đang hoàn tiền";
  if (normalized === "EXCHANGE_PROCESSING") return "Đang đổi hàng";
  if (normalized === "RETURN_REJECTED") return "Từ chối yêu cầu";
  if (normalized === "EXCHANGED") return "Đã đổi hàng";
  if (normalized === "REFUNDED") return "Đã hoàn tiền";
  return normalized || "N/A";
};

const returnStatusType = (status) => {
  const normalized = String(status || "").toUpperCase();
  if (["REQUESTED", "UNDER_REVIEW", "IN_TRANSIT_BACK"].includes(normalized)) return "warning";
  if (["RETURN_APPROVED", "RETURN_RECEIVED", "REFUND_PROCESSING", "EXCHANGE_PROCESSING"].includes(normalized)) return "primary";
  if (["REFUNDED", "EXCHANGED"].includes(normalized)) return "success";
  return "danger";
};

const latestReturnByOrderId = computed(() => {
  const map = new Map();
  for (const item of returnRequests.value) {
    const orderId = Number(item?.orderId || 0);
    if (!orderId || map.has(orderId)) continue;
    map.set(orderId, item);
  }
  return map;
});

const getReturnStatus = (orderId) => latestReturnByOrderId.value.get(Number(orderId || 0))?.status || null;

const hasActiveReturn = (orderId) => {
  const status = String(getReturnStatus(orderId) || "").toUpperCase();
  return [
    "REQUESTED",
    "UNDER_REVIEW",
    "RETURN_APPROVED",
    "IN_TRANSIT_BACK",
    "RETURN_RECEIVED",
    "REFUND_PROCESSING",
    "EXCHANGE_PROCESSING"
  ].includes(status);
};

const latestDeliveredAt = (order) => {
  if (isGhnDelivered(order) && order?.shippingUpdatedAt) {
    return order.shippingUpdatedAt;
  }
  const steps = Array.isArray(order?.statusHistory) ? order.statusHistory : [];
  for (let i = steps.length - 1; i >= 0; i -= 1) {
    const step = steps[i];
    if (String(step?.status || "").toUpperCase() === "DELIVERED") return step?.changedAt || null;
  }
  return null;
};

const isGhnDelivered = (order) => {
  const ghnStatus = String(order?.shippingStatus || "").trim().toLowerCase();
  return ghnStatus === "delivered";
};

const canRequestReturn = (order) => {
  if (!isGhnDelivered(order)) return false;
  if (hasActiveReturn(order?.id)) return false;
  const deliveredAt = latestDeliveredAt(order);
  if (!deliveredAt) return false;
  const deadline = new Date(deliveredAt);
  deadline.setDate(deadline.getDate() + 7);
  return Date.now() <= deadline.getTime();
};

const itemCount = (order) => {
  if (!Array.isArray(order?.items)) return 0;
  return order.items.reduce((sum, item) => sum + (Number(item?.quantity) || 0), 0);
};

const sortedHistory = (history) => {
  const rows = Array.isArray(history) ? [...history] : [];
  return rows.sort((a, b) => new Date(a?.changedAt || 0).getTime() - new Date(b?.changedAt || 0).getTime());
};

const handleReorder = async (order) => {
  try {
    await orderApi.reorder(order.id);
    ElMessage.success("Đã thêm lại sản phẩm của đơn vào giỏ hàng");
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Không thể mua lại đơn hàng này");
  }
};

const openReviewDialog = (order, item) => {
  reviewForm.value = {
    orderId: order?.id || null,
    productId: item?.productId || null,
    productName: item?.productName || "Sản phẩm",
    rating: 5,
    comment: ""
  };
  reviewUploadFiles.value = [];
  reviewDialogVisible.value = true;
};

const beforeReviewUpload = (rawFile) => {
  const isImage = String(rawFile?.type || "").startsWith("image/");
  if (!isImage) {
    ElMessage.warning("Chỉ hỗ trợ file ảnh");
    return false;
  }
  const maxSize = 5 * 1024 * 1024;
  if (Number(rawFile?.size || 0) > maxSize) {
    ElMessage.warning("Mỗi ảnh tối đa 5MB");
    return false;
  }
  return true;
};

const handleReviewUploadExceed = () => {
  ElMessage.warning("Bạn chỉ có thể chọn tối đa 5 ảnh");
};

const uploadReviewImages = async () => {
  const rawFiles = (Array.isArray(reviewUploadFiles.value) ? reviewUploadFiles.value : [])
    .map((item) => item?.raw)
    .filter(Boolean)
    .slice(0, 5);
  if (!rawFiles.length) return [];
  return uploadApi.uploadReviewFiles(rawFiles);
};

const submitReview = async () => {
  const payload = {
    orderId: Number(reviewForm.value.orderId || 0),
    productId: Number(reviewForm.value.productId || 0),
    rating: Number(reviewForm.value.rating || 5),
    comment: String(reviewForm.value.comment || "").trim(),
    imageUrls: []
  };
  if (!payload.orderId || !payload.productId) {
    ElMessage.warning("Thiếu dữ liệu đánh giá");
    return;
  }
  reviewSubmitting.value = true;
  try {
    payload.imageUrls = await uploadReviewImages();
    await reviewApi.create(payload);
    ElMessage.success("Gửi đánh giá thành công");
    reviewDialogVisible.value = false;
    reviewUploadFiles.value = [];
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Không thể gửi đánh giá");
  } finally {
    reviewSubmitting.value = false;
  }
};

const handlePageChange = (page) => {
  currentPage.value = Math.max(1, Number(page) || 1);
};

const fetchMyReturnRequests = async () => {
  try {
    const { data } = await returnApi.getMyReturns();
    returnRequests.value = Array.isArray(data) ? data : [];
  } catch (error) {
    returnRequests.value = [];
  }
};

const openReturnDialog = (order) => {
  const orderItems = Array.isArray(order?.items) ? order.items : [];
  returnForm.value = {
    orderId: order?.id || null,
    returnType: "REFUND",
    reasonCode: "NOT_AS_DESCRIBED",
    reasonDetail: "",
    evidenceUrls: "",
    orderItems: orderItems.map((item) => ({
      id: item.id,
      sku: item.sku,
      productName: item.productName,
      maxQty: Number(item.quantity || 1),
      quantity: 1,
      selected: false
    }))
  };
  returnDialogVisible.value = true;
};

const submitReturnRequest = async () => {
  const orderId = Number(returnForm.value.orderId || 0);
  const returnType = String(returnForm.value.returnType || "").trim().toUpperCase();
  const reasonCode = String(returnForm.value.reasonCode || "").trim().toUpperCase();
  const reasonDetail = String(returnForm.value.reasonDetail || "").trim();
  const evidenceUrls = String(returnForm.value.evidenceUrls || "").trim();
  const selectedItems = (Array.isArray(returnForm.value.orderItems) ? returnForm.value.orderItems : [])
    .filter((item) => item.selected)
    .map((item) => ({
      orderItemId: Number(item.id),
      quantity: Number(item.quantity || 1)
    }));

  if (!orderId) return ElMessage.warning("Đơn hàng không hợp lệ");
  if (!["REFUND", "EXCHANGE"].includes(returnType)) return ElMessage.warning("Loại yêu cầu không hợp lệ");
  if (!reasonCode) return ElMessage.warning("Vui lòng chọn lý do đổi trả");
  if (reasonDetail.length < 10) return ElMessage.warning("Mô tả chi tiết cần tối thiểu 10 ký tự");
  if (selectedItems.length === 0) return ElMessage.warning("Vui lòng chọn ít nhất 1 sản phẩm cần đổi trả");

  returnSubmitting.value = true;
  try {
    await returnApi.createReturnRequest({
      orderId,
      returnType,
      reasonCode,
      reasonDetail,
      evidenceUrls: evidenceUrls || null,
      items: selectedItems
    });
    ElMessage.success("Đã gửi yêu cầu đổi trả");
    returnDialogVisible.value = false;
    await fetchMyReturnRequests();
    await store.fetchMyOrders();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Gửi yêu cầu đổi trả thất bại");
  } finally {
    returnSubmitting.value = false;
  }
};

watch([keyword, statusFilter, pageSize], () => {
  currentPage.value = 1;
});

watch(filteredOrders, (orders) => {
  const maxPage = Math.max(1, Math.ceil(orders.length / pageSize.value));
  if (currentPage.value > maxPage) currentPage.value = maxPage;
});

onMounted(async () => {
  await Promise.all([store.fetchMyOrders(), fetchMyReturnRequests()]);
});
</script>

<style scoped lang="scss">
.orders-page {
  padding: 24px 0 46px;
  background: #ffffff;
}

.orders-container {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 16px;
  display: grid;
  gap: 12px;
}

.hero-card,
.filter-card,
.order-card {
  border-radius: 0;
  border-color: #dbe3ee;
}

.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.eyebrow {
  margin: 0 0 6px;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: #64748b;
}

.hero-top h1 {
  margin: 0 0 4px;
  font-size: 30px;
  line-height: 1.1;
  letter-spacing: -0.5px;
  color: #0f172a;
  font-weight: 900;
}

.hero-sub {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.stat-card {
  border: 1px solid #e2e8f0;
  border-radius: 0;
  padding: 10px 12px;
  background: #fff;
}

.stat-card span {
  display: block;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 2px;
}

.stat-card strong {
  font-size: 22px;
  line-height: 1;
  color: #0f172a;
}

.stat-card.total { box-shadow: inset 0 0 0 1px #bfdbfe; }
.stat-card.progress { box-shadow: inset 0 0 0 1px #fde68a; }
.stat-card.delivered { box-shadow: inset 0 0 0 1px #bbf7d0; }
.stat-card.failed { box-shadow: inset 0 0 0 1px #fecaca; }

.filter-grid {
  display: grid;
  grid-template-columns: 1.1fr 1fr 160px;
  gap: 10px;
  align-items: center;
}

.status-segment {
  width: 100%;
}

.page-size {
  width: 100%;
}

.loading-wrap {
  min-height: 180px;
}

.empty-card {
  background: #fff;
  border-radius: 0;
  border: 1px solid #dbe3ee;
}

.orders-stack {
  display: grid;
  gap: 12px;
}

.order-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.head-left {
  display: grid;
  gap: 2px;
}

.head-left strong {
  font-size: 18px;
  color: #0f172a;
}

.head-left span {
  font-size: 12px;
  color: #64748b;
}

.head-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.head-right p {
  margin: 0 0 0 2px;
  color: #0f172a;
  font-weight: 800;
}

.order-body {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 12px;
}

.meta-panel {
  border: 1px solid #e2e8f0;
  border-radius: 0;
  background: #f8fafc;
  padding: 10px;
  display: grid;
  gap: 8px;
}

.meta-panel p {
  margin: 0;
  display: grid;
  gap: 2px;
}

.meta-panel span {
  font-size: 11px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.meta-panel strong {
  font-size: 13px;
  line-height: 1.35;
  color: #0f172a;
}

.items-panel {
  border: 1px solid #e2e8f0;
  border-radius: 0;
  background: #fff;
  padding: 8px 10px;
}

.item-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px dashed #e2e8f0;
}

.item-line:last-child {
  border-bottom: 0;
}

.item-main {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.item-main strong {
  font-size: 13px;
  color: #0f172a;
  word-break: break-word;
}

.item-main span {
  font-size: 12px;
  color: #64748b;
  word-break: break-all;
}

.item-side {
  display: grid;
  justify-items: end;
  gap: 1px;
}

.item-side span {
  font-size: 12px;
  color: #64748b;
}

.item-side strong {
  font-size: 13px;
  color: #0f172a;
}

.order-foot {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.foot-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.return-note {
  font-size: 12px;
  color: #6b7280;
}

.status-timeline {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;

  span {
    font-size: 11px;
    color: #64748b;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    padding: 4px 6px;
  }
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
}

.w-full {
  width: 100%;
}

.return-items {
  border: 1px solid #e2e8f0;
  border-radius: 0;
  background: #f8fafc;
}

.upload-note {
  margin: 8px 0 0;
  font-size: 12px;
  color: #64748b;
}

.return-item-row {
  padding: 10px 12px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 10px;
  border-bottom: 1px dashed #e2e8f0;
}

.return-item-row:last-child {
  border-bottom: 0;
}

.return-item-meta {
  display: grid;
  gap: 2px;
}

.return-item-meta strong {
  font-size: 13px;
  color: #0f172a;
}

.return-item-meta span {
  font-size: 12px;
  color: #64748b;
}

@media (max-width: 980px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }

  .order-body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .orders-container {
    padding: 0 12px;
  }

  .hero-top {
    flex-direction: column;
  }

  .hero-top h1 {
    font-size: 24px;
  }

  .order-head {
    flex-direction: column;
  }

  .head-right {
    justify-content: flex-start;
  }

  .order-foot {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .pagination-wrap {
    justify-content: center;
  }

  .return-item-row {
    grid-template-columns: auto 1fr;
  }
}
</style>
