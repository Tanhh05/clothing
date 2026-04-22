<template>
  <section class="account-page client-page-shell">
    <el-card shadow="never" class="surface-card profile-card">
      <template #header>
        <div class="card-head">
          <span>Thông tin cá nhân</span>
        </div>
      </template>

      <el-form label-position="top" class="profile-form">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="Họ và tên">
              <el-input v-model="profileForm.fullName" placeholder="Nhập họ và tên" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="Số điện thoại">
              <el-input v-model="profileForm.phone" placeholder="Nhập số điện thoại" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="Email">
              <el-input :model-value="authStore.profile?.email || ''" disabled />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="Tên đăng nhập">
              <el-input :model-value="authStore.profile?.username || authStore.username || ''" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-actions">
          <el-button type="primary" :loading="savingProfile" @click="saveProfile">Lưu thông tin cá nhân</el-button>
        </div>
      </el-form>
    </el-card>

    <el-card shadow="never" class="address-panel surface-card">
      <template #header>
        <div class="card-head">
          <span>Danh sách địa chỉ giao hàng</span>
          <el-button type="primary" link @click="openCreate">+ Thêm địa chỉ</el-button>
        </div>
      </template>

      <div v-if="loading" v-loading="loading" class="loading-box"></div>
      <el-empty v-else-if="!addresses.length" description="Chưa có địa chỉ nào" />
      <div v-else class="address-list">
        <article v-for="item in addresses" :key="item.id" class="address-card">
          <div class="address-main">
            <p class="name-line"><strong>{{ item.recipientName }}</strong> · {{ item.phone }}</p>
            <p>{{ item.addressLine }}, {{ item.ward }}, {{ item.district }}, {{ item.province }}</p>
          </div>
          <div class="card-actions">
            <el-tag v-if="item.isDefault" type="success">Mặc định</el-tag>
            <el-button size="small" @click="openEdit(item)">Sửa</el-button>
            <el-button size="small" type="danger" plain @click="removeAddress(item)">Xóa</el-button>
          </div>
        </article>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ'" width="680px">
      <el-form label-position="top">
        <el-row :gutter="10">
          <el-col :xs="24" :sm="12">
            <el-form-item label="Người nhận">
              <el-input v-model="form.recipientName" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="SĐT">
              <el-input v-model="form.phone" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="10">
          <el-col :xs="24" :sm="12">
            <el-form-item label="Tỉnh/Thành">
              <el-select
                v-model="selectedProvinceId"
                placeholder="Chọn tỉnh/thành"
                filterable
                :loading="addressLoading.provinces"
              >
                <el-option v-for="item in provinces" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="Quận/Huyện">
              <el-select
                v-model="selectedDistrictId"
                placeholder="Chọn quận/huyện"
                filterable
                :disabled="!selectedProvinceId"
                :loading="addressLoading.districts"
              >
                <el-option v-for="item in districts" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="10">
          <el-col :xs="24" :sm="12">
            <el-form-item label="Phường/Xã">
              <el-select
                v-model="selectedWardId"
                placeholder="Chọn phường/xã"
                filterable
                :disabled="!selectedDistrictId"
                :loading="addressLoading.wards"
              >
                <el-option v-for="item in wards" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="Mặc định">
              <el-switch v-model="form.isDefault" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Số nhà, tên đường">
          <el-input v-model="form.addressLine" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Hủy</el-button>
        <el-button type="primary" @click="saveAddress">Lưu</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { ElMessage } from "@/utils/dialogMessage";
import { useAuthStore } from "@/store/authStore";
import { userAddressApi } from "@/modules/address/api/userAddressApi";
import { addressApi } from "@/modules/address/api/addressApi";
import { useConfirmDialog } from "@/composables/useConfirmDialog";

const authStore = useAuthStore();
const { confirm } = useConfirmDialog();

const loading = ref(false);
const savingProfile = ref(false);
const addresses = ref([]);
const dialogVisible = ref(false);
const editingId = ref(null);
const provinces = ref([]);
const districts = ref([]);
const wards = ref([]);
const selectedProvinceId = ref("");
const selectedDistrictId = ref("");
const selectedWardId = ref("");
const addressLoading = ref({
  provinces: false,
  districts: false,
  wards: false
});
const profileForm = ref({
  fullName: "",
  phone: ""
});
const form = ref(defaultForm());

const defaultAddress = computed(() => addresses.value.find((item) => item.isDefault) || null);
const displayName = computed(
  () => authStore.profile?.fullName || authStore.profile?.username || authStore.username || "Tài khoản khách hàng"
);
const accountInitial = computed(() => String(displayName.value || "U").trim().charAt(0).toUpperCase());

function defaultForm() {
  return {
    recipientName: "",
    phone: "",
    addressLine: "",
    isDefault: false
  };
}

const hydrateProfileForm = () => {
  profileForm.value.fullName = authStore.profile?.fullName || "";
  profileForm.value.phone = authStore.profile?.phone || "";
};

const fetchProfile = async () => {
  try {
    await authStore.fetchProfile(true);
    hydrateProfileForm();
  } catch (_error) {
    ElMessage.error("Không tải được thông tin cá nhân");
  }
};

const saveProfile = async () => {
  try {
    savingProfile.value = true;
    await authStore.updateProfile({
      fullName: profileForm.value.fullName?.trim() || null,
      phone: profileForm.value.phone?.trim() || null
    });
    hydrateProfileForm();
    ElMessage.success("Đã cập nhật thông tin cá nhân");
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Cập nhật thông tin thất bại");
  } finally {
    savingProfile.value = false;
  }
};

const fetchAddresses = async () => {
  loading.value = true;
  try {
    const { data } = await userAddressApi.getMyAddresses();
    addresses.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Không tải được địa chỉ");
  } finally {
    loading.value = false;
  }
};

const fetchProvinces = async () => {
  try {
    addressLoading.value.provinces = true;
    const { data } = await addressApi.getProvinces();
    provinces.value = Array.isArray(data) ? data : [];
  } catch (_error) {
    provinces.value = [];
    ElMessage.error("Không tải được danh sách tỉnh/thành");
  } finally {
    addressLoading.value.provinces = false;
  }
};

const fetchDistricts = async (provinceId) => {
  if (!provinceId) {
    districts.value = [];
    return;
  }
  try {
    addressLoading.value.districts = true;
    const { data } = await addressApi.getDistricts(provinceId);
    districts.value = Array.isArray(data) ? data : [];
  } catch (_error) {
    districts.value = [];
    ElMessage.error("Không tải được quận/huyện");
  } finally {
    addressLoading.value.districts = false;
  }
};

const fetchWards = async (districtId) => {
  if (!districtId) {
    wards.value = [];
    return;
  }
  try {
    addressLoading.value.wards = true;
    const { data } = await addressApi.getWards(districtId);
    wards.value = Array.isArray(data) ? data : [];
  } catch (_error) {
    wards.value = [];
    ElMessage.error("Không tải được phường/xã");
  } finally {
    addressLoading.value.wards = false;
  }
};

const resetAddressSelection = () => {
  selectedProvinceId.value = "";
  selectedDistrictId.value = "";
  selectedWardId.value = "";
  districts.value = [];
  wards.value = [];
};

const openCreate = () => {
  editingId.value = null;
  form.value = defaultForm();
  resetAddressSelection();
  dialogVisible.value = true;
};

const openEdit = async (item) => {
  editingId.value = item.id;
  form.value = {
    recipientName: item.recipientName || "",
    phone: item.phone || "",
    addressLine: item.addressLine || "",
    isDefault: Boolean(item.isDefault)
  };

  const matchedProvince = provinces.value.find((p) => p.name === item.province);
  selectedProvinceId.value = matchedProvince?.id || "";
  await fetchDistricts(selectedProvinceId.value);

  const matchedDistrict = districts.value.find((d) => d.name === item.district);
  selectedDistrictId.value = matchedDistrict?.id || "";
  await fetchWards(selectedDistrictId.value);

  const matchedWard = wards.value.find((w) => w.name === item.ward);
  selectedWardId.value = matchedWard?.id || "";

  dialogVisible.value = true;
};

const saveAddress = async () => {
  const province = provinces.value.find((item) => item.id === selectedProvinceId.value)?.name || "";
  const district = districts.value.find((item) => item.id === selectedDistrictId.value)?.name || "";
  const ward = wards.value.find((item) => item.id === selectedWardId.value)?.name || "";

  if (!form.value.recipientName?.trim()) return ElMessage.warning("Vui lòng nhập người nhận");
  if (!form.value.phone?.trim()) return ElMessage.warning("Vui lòng nhập số điện thoại");
  if (!province || !district || !ward) return ElMessage.warning("Vui lòng chọn đủ tỉnh/quận/phường");
  if (!form.value.addressLine?.trim()) return ElMessage.warning("Vui lòng nhập số nhà, tên đường");

  const payload = {
    recipientName: form.value.recipientName.trim(),
    phone: form.value.phone.trim(),
    province,
    district,
    ward,
    addressLine: form.value.addressLine.trim(),
    isDefault: Boolean(form.value.isDefault)
  };

  try {
    if (editingId.value) {
      await userAddressApi.updateAddress(editingId.value, payload);
    } else {
      await userAddressApi.createAddress(payload);
    }
    dialogVisible.value = false;
    await fetchAddresses();
    ElMessage.success("Đã lưu địa chỉ");
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Lưu địa chỉ thất bại");
  }
};

const removeAddress = async (item) => {
  try {
    await confirm({
      title: "Xác nhận",
      message: `Xóa địa chỉ của "${item?.recipientName || "người nhận"}"?`,
      confirmButtonText: "Xóa",
      cancelButtonText: "Hủy",
      onConfirm: async () => {
        await userAddressApi.deleteAddress(item.id);
        await fetchAddresses();
        ElMessage.success("Đã xóa địa chỉ");
      }
    });
  } catch (error) {
    if (error.message !== "cancel") {
      ElMessage.error(error?.response?.data?.message || "Không thể xóa địa chỉ");
    }
  }
};

watch(selectedProvinceId, async (value) => {
  selectedDistrictId.value = "";
  selectedWardId.value = "";
  wards.value = [];
  await fetchDistricts(value);
});

watch(selectedDistrictId, async (value) => {
  selectedWardId.value = "";
  await fetchWards(value);
});

onMounted(async () => {
  await Promise.all([fetchProfile(), fetchAddresses(), fetchProvinces()]);
});
</script>

<style scoped lang="scss">
.account-page {
  background: transparent;
  padding: 24px 0;
}

.hero-card {
  margin-bottom: 16px;
}

.hero-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.hero-content.compact {
  min-height: 84px;
}

.identity {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 56px;
  height: 56px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  border: 1px solid #d6dee8;
  background: #f8fafc;
  flex-shrink: 0;
}

.identity-content {
  display: grid;
  gap: 2px;
}

.identity-name {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.3px;
  color: #0f172a;
}

.identity-meta {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.hero-badges {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.surface-card {
  height: 100%;
  border-radius: 0 !important;
}

.surface-card :deep(.el-card__header) {
  border-radius: 0;
}

.surface-card :deep(.el-card__body) {
  border-radius: 0;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-weight: 700;
  letter-spacing: 0.2px;
}

.summary-card {
  height: 100%;
  border-radius: 0 !important;
}

.summary-number {
  font-size: 36px;
  font-weight: 800;
  line-height: 1;
  margin-bottom: 6px;
  color: #0f172a;
}

.summary-text {
  margin: 0 0 10px;
  color: var(--el-text-color-secondary);
}

.summary-btn {
  margin-top: 12px;
  width: 100%;
  border-radius: 0 !important;
}

.address-panel {
  margin-top: 16px;
  border-radius: 0 !important;
}

.address-panel :deep(.el-card__header) {
  border-radius: 0;
}

.address-panel :deep(.el-card__body) {
  border-radius: 0;
}

.address-list {
  display: grid;
  gap: 0;
}

.address-card {
  border: 1px solid #e5e7eb;
  border-bottom: none;
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  background: #fff;
}

.address-card:last-child {
  border-bottom: 1px solid #e5e7eb;
}

.address-card:first-child {
  border-top: 1px solid #e5e7eb;
}

.name-line {
  margin: 0 0 6px;
  font-weight: 600;
}

.address-main {
  flex: 1;
}

.address-main p {
  margin: 0;
  font-size: 14px;
  color: #333;
}

.address-main p:last-child {
  margin-top: 4px;
  color: #666;
  font-size: 13px;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.loading-box {
  min-height: 120px;
}

.profile-card {
  margin-bottom: 24px;
  border-radius: 0 !important;
}

.profile-form {
  padding: 12px 0;
}

.profile-form :deep(.el-input),
.profile-form :deep(.el-select) {
  width: 100%;
}

.profile-form :deep(.el-input__wrapper) {
  border-radius: 0 !important;
}

.profile-form :deep(.el-input__wrapper:hover) {
  box-shadow: none;
}

.profile-form :deep(.el-select .el-input__wrapper) {
  border-radius: 0 !important;
}

.form-actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}

// Dialog styles
:deep(.el-dialog) {
  border-radius: 0 !important;
}

:deep(.el-dialog__header) {
  border-radius: 0;
}

:deep(.el-dialog__body) {
  border-radius: 0;
}

:deep(.el-form-item__content .el-input__wrapper) {
  border-radius: 0 !important;
}

:deep(.el-form-item__content .el-select .el-input__wrapper) {
  border-radius: 0 !important;
}

// Button styles
:deep(.el-button) {
  border-radius: 0 !important;
}

:deep(.el-tag) {
  border-radius: 0 !important;
}

// Element Plus overrides
:deep(.el-card) {
  border-radius: 0 !important;
}

@media (max-width: 768px) {
  .hero-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-badges {
    width: 100%;
  }

  .address-card {
    flex-direction: column;
  }

  .card-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
