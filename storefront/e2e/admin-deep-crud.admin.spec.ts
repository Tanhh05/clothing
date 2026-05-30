import { expect, test } from "@playwright/test";

const BACKEND_URL = "http://localhost:8080";
const ADMIN_USER = "admin";
const ADMIN_PASS = "123456";

function unwrap<T>(payload: any): T {
  return (payload?.data ?? payload) as T;
}

async function adminToken(request: any): Promise<string> {
  const res = await request.post(`${BACKEND_URL}/api/auth/login`, {
    data: { usernameOrEmail: ADMIN_USER, password: ADMIN_PASS },
  });
  expect(res.status()).toBe(200);
  const body = await res.json();
  const data = unwrap<any>(body);
  const token = String(data?.accessToken || "");
  expect(token.length).toBeGreaterThan(0);
  return token;
}

test.describe("Admin Deep CRUD", () => {
  test("vouchers, banners, categories, settings CRUD/update flow", async ({ request }) => {
    const token = await adminToken(request);
    const headers = { Authorization: `Bearer ${token}` };
    const nonce = Date.now();

    // Voucher CRUD
    const voucherCode = `AUTOE2E${nonce}`;
    const createVoucherRes = await request.post(`${BACKEND_URL}/api/admin/vouchers`, {
      headers,
      data: {
        code: voucherCode,
        discountType: "PERCENT",
        discountValue: 10,
        minOrderValue: 0,
        maxUsage: 10,
        status: "ACTIVE",
      },
    });
    expect(createVoucherRes.status()).toBeGreaterThanOrEqual(200);
    expect(createVoucherRes.status()).toBeLessThan(300);
    const createdVoucher = unwrap<any>(await createVoucherRes.json());
    const voucherId = Number(createdVoucher?.id);
    expect(voucherId).toBeGreaterThan(0);

    const updateVoucherRes = await request.put(
      `${BACKEND_URL}/api/admin/vouchers/${voucherId}`,
      {
        headers,
        data: {
          code: voucherCode,
          discountType: "AMOUNT",
          discountValue: 5000,
          minOrderValue: 10000,
          maxUsage: 20,
          status: "INACTIVE",
        },
      }
    );
    expect(updateVoucherRes.ok()).toBeTruthy();

    const deleteVoucherRes = await request.delete(
      `${BACKEND_URL}/api/admin/vouchers/${voucherId}`,
      { headers }
    );
    expect(deleteVoucherRes.ok()).toBeTruthy();

    // Banner CRUD
    const createBannerRes = await request.post(`${BACKEND_URL}/api/admin/banners`, {
      headers,
      data: {
        title: `Auto Banner ${nonce}`,
        imageUrl: "https://picsum.photos/1280/360",
        linkUrl: "/",
        status: "ACTIVE",
      },
    });
    expect(createBannerRes.ok()).toBeTruthy();
    const createdBanner = unwrap<any>(await createBannerRes.json());
    const bannerId = Number(createdBanner?.id);
    expect(bannerId).toBeGreaterThan(0);

    const updateBannerRes = await request.put(
      `${BACKEND_URL}/api/admin/banners/${bannerId}`,
      {
        headers,
        data: {
          title: `Auto Banner ${nonce} Updated`,
          imageUrl: "https://picsum.photos/1280/361",
          linkUrl: "/product-category/men",
          status: "INACTIVE",
        },
      }
    );
    expect(updateBannerRes.ok()).toBeTruthy();

    const deleteBannerRes = await request.delete(
      `${BACKEND_URL}/api/admin/banners/${bannerId}`,
      { headers }
    );
    expect(deleteBannerRes.ok()).toBeTruthy();

    // Category CRUD
    const categorySlug = `auto-e2e-${nonce}`;
    const createCategoryRes = await request.post(`${BACKEND_URL}/api/categories`, {
      headers,
      data: {
        name: `Auto Category ${nonce}`,
        slug: categorySlug,
        shortContent: "E2E generated category",
        displayOrder: 9999,
        status: "ACTIVE",
      },
    });
    expect(createCategoryRes.ok()).toBeTruthy();
    const createdCategory = unwrap<any>(await createCategoryRes.json());
    const categoryId = Number(createdCategory?.id);
    expect(categoryId).toBeGreaterThan(0);

    const updateCategoryRes = await request.put(
      `${BACKEND_URL}/api/categories/${categoryId}`,
      {
        headers,
        data: {
          name: `Auto Category ${nonce} Updated`,
          slug: `${categorySlug}-u`,
          shortContent: "updated",
          displayOrder: 9998,
          status: "INACTIVE",
        },
      }
    );
    expect(updateCategoryRes.ok()).toBeTruthy();

    const deleteCategoryRes = await request.delete(
      `${BACKEND_URL}/api/categories/${categoryId}`,
      { headers }
    );
    expect(deleteCategoryRes.ok()).toBeTruthy();

    // Store settings update then restore
    const settingsGetRes = await request.get(`${BACKEND_URL}/api/admin/store-settings`, {
      headers,
    });
    expect(settingsGetRes.ok()).toBeTruthy();
    const originalSettings = unwrap<any>(await settingsGetRes.json());

    const patchedSettings = {
      ...originalSettings,
      storeName: `${originalSettings?.storeName || "Store"} E2E`,
    };
    const updateSettingsRes = await request.put(
      `${BACKEND_URL}/api/admin/store-settings`,
      { headers, data: patchedSettings }
    );
    expect(updateSettingsRes.ok()).toBeTruthy();

    const restoreSettingsRes = await request.put(
      `${BACKEND_URL}/api/admin/store-settings`,
      { headers, data: originalSettings }
    );
    expect(restoreSettingsRes.ok()).toBeTruthy();
  });

  test("admin backoffice read/status endpoints should respond", async ({ request }) => {
    const token = await adminToken(request);
    const headers = { Authorization: `Bearer ${token}` };

    const endpoints = [
      "/api/orders/summary",
      "/api/orders/admin?page=0&size=20",
      "/api/orders/admin/status-options",
      "/api/admin/users?page=0&size=20",
      "/api/admin/returns",
      "/api/admin/warehouse-inbounds/page?page=0&size=10",
      "/api/admin/audit-logs",
      "/api/admin/notifications",
    ];

    for (const path of endpoints) {
      const res = await request.get(`${BACKEND_URL}${path}`, { headers });
      expect(res.status(), path).toBeGreaterThanOrEqual(200);
      expect(res.status(), path).toBeLessThan(300);
    }
  });
});
