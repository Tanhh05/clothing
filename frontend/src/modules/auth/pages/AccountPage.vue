<template>
  <section class="account-page">
    <el-card shadow="never" class="header-card">
      <h1>Thông tin cá nhân</h1>
      <p>Quản lý hồ sơ và địa chỉ giao hàng trên cùng một trang.</p>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card shadow="hover">
          <template #header>
            <div class="card-head">
              <span>Hồ sơ tài khoản</span>
            </div>
          </template>
          <el-form label-position="top" class="profile-form">
            <el-form-item label="Họ và tên">
              <el-input v-model="profileForm.fullName" placeholder="Nhập họ và tên" />
            </el-form-item>
            <el-form-item label="Số điện thoại">
              <el-input v-model="profileForm.phone" placeholder="Nhập số điện thoại" />
            </el-form-item>
            <el-row :gutter="10">
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
            <el-button type="primary" :loading="savingProfile" @click="saveProfile">Lưu thông tin cá nhân</el-button>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card shadow="hover" class="summary-card">
          <template #header>
            <div class="card-head">
              <span>Tổng quan địa chỉ</span>
            </div>
          </template>
          <div class="summary-number">{{ addresses.length }}</div>
          <p class="summary-text">Địa chỉ đã lưu</p>
          <el-tag v-if="defaultAddress" type="success" effect="plain">Mặc định: {{ defaultAddress.recipientName }}</el-tag>
          <el-button type="primary" class="summary-btn" @click="openCreate">Thêm địa chỉ mới</el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="address-panel">
      <template #header>
        <div class="card-head">
          <span>Danh sách địa chỉ giao hàng</span>
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
            <el-button size="small" type="danger" plain @click="removeAddress(item.id)">Xóa</el-button>
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
import { ElMessage } from "element-plus";
import { useAuthStore } from "@/store/authStore";
import { userAddressApi } from "@/modules/address/api/userAddressApi";
import { addressApi } from "@/modules/address/api/addressApi";

const authStore = useAuthStore();

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

const removeAddress = async (id) => {
  try {
    await userAddressApi.deleteAddress(id);
    await fetchAddresses();
    ElMessage.success("Đã xóa địa chỉ");
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Không thể xóa địa chỉ");
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
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.header-card {
  margin-bottom: 16px;

  h1 {
    margin: 0 0 6px;
    font-size: 28px;
    font-weight: 700;
  }

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
  }
}

.card-head {
  font-weight: 600;
}

.summary-card {
  height: 100%;
}

.summary-number {
  font-size: 40px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 6px;
  color: var(--el-color-primary);
}

.summary-text {
  margin: 0 0 10px;
  color: var(--el-text-color-secondary);
}

.summary-btn {
  margin-top: 12px;
  width: 100%;
}

.address-panel {
  margin-top: 16px;
}

.address-list {
  display: grid;
  gap: 10px;
}

.address-card {
  border: 1px solid var(--el-border-color);
  border-radius: 10px;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.name-line {
  margin: 0 0 6px;
}

.address-main p {
  margin: 0;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.loading-box {
  min-height: 120px;
}

.profile-form :deep(.el-input),
.profile-form :deep(.el-select) {
  width: 100%;
}

@media (max-width: 768px) {
  .address-card {
    flex-direction: column;
  }
}
</style>
