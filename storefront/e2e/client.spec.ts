import { expect, test, type Page } from "@playwright/test";

const BACKEND_URL = "http://localhost:8080";
const VALID_LOGIN_EMAIL = "admin";
const VALID_LOGIN_PASSWORD = "123456";

function unwrap<T>(payload: any): T {
  return (payload?.data ?? payload) as T;
}

async function getInStockVariantId(page: Page): Promise<number> {
  const productsRes = await page.request.get(`${BACKEND_URL}/api/products?page=0&size=10`);
  expect(productsRes.ok()).toBeTruthy();
  const productsPage = unwrap<any>(await productsRes.json());
  const products = Array.isArray(productsPage?.content) ? productsPage.content : [];
  expect(products.length).toBeGreaterThan(0);

  for (const product of products) {
    const productId = Number(product?.id);
    if (!productId) continue;
    const detailRes = await page.request.get(`${BACKEND_URL}/api/products/${productId}`);
    if (!detailRes.ok()) continue;
    const detail = unwrap<any>(await detailRes.json());
    const variants = Array.isArray(detail?.variants) ? detail.variants : [];
    const inStock = variants.find((v: any) => Number(v?.stock || 0) > 0);
    if (inStock?.id) return Number(inStock.id);
  }

  throw new Error("No in-stock product variant found for checkout tests");
}

async function bootstrapAuthenticatedUser(page: Page): Promise<string> {
  const loginRes = await page.request.post(`${BACKEND_URL}/api/auth/login`, {
    data: {
      usernameOrEmail: VALID_LOGIN_EMAIL,
      password: VALID_LOGIN_PASSWORD,
    },
  });
  expect(loginRes.ok()).toBeTruthy();
  const body = await loginRes.json();
  const data = unwrap<any>(body);
  const token = String(data?.accessToken || data?.token || "").trim();
  expect(token.length).toBeGreaterThan(0);

  const cookieUser = {
    id: Number(data?.userId || data?.id || 1),
    username: data?.username || VALID_LOGIN_EMAIL,
    email: data?.email || `${VALID_LOGIN_EMAIL}@example.com`,
    fullname: data?.fullName || data?.fullname || VALID_LOGIN_EMAIL,
    phone: data?.phone || "",
    token,
    refreshToken: data?.refreshToken || "",
    tokenType: data?.tokenType || "Bearer",
  };

  await page.context().addCookies([
    {
      name: "user",
      value: JSON.stringify(cookieUser),
      url: "http://localhost:3000",
    },
  ]);

  return token;
}

async function addOneProductToCart(page: Page) {
  await bootstrapAuthenticatedUser(page);
  const variantId = await getInStockVariantId(page);
  const productRes = await page.request.get(`${BACKEND_URL}/api/products?page=0&size=10`);
  expect(productRes.ok()).toBeTruthy();
  const productsPage = unwrap<any>(await productRes.json());
  const products = Array.isArray(productsPage?.content) ? productsPage.content : [];
  const product = products.find((p: any) => Number(p?.id) > 0) || products[0];
  expect(product).toBeTruthy();

  const productDetailRes = await page.request.get(
    `${BACKEND_URL}/api/products/${Number(product.id)}`
  );
  expect(productDetailRes.ok()).toBeTruthy();
  const productDetail = unwrap<any>(await productDetailRes.json());
  const variants = Array.isArray(productDetail?.variants) ? productDetail.variants : [];
  const variant = variants.find((v: any) => Number(v?.id) === variantId) || variants[0];
  expect(variant).toBeTruthy();

  const cartPayload = [
    {
      id: Number(product.id),
      slug: product.slug || "",
      name: product.name || "Auto Product",
      price: Number(product.price || 100000),
      qty: 1,
      img1: product.image1 || product.img1 || "",
      img2: product.image2 || product.img2 || "",
      selectedSize: variant?.size || "",
      selectedColor: variant?.color || "",
      selectedVariantId: Number(variant?.id || 0),
    },
  ];
  await page.context().addCookies([
    {
      name: "cart",
      value: JSON.stringify(cartPayload),
      url: "http://localhost:3000",
    },
  ]);

  await page.goto("/shopping-cart", { waitUntil: "networkidle" });
  await expect(page.getByRole("heading", { name: /giỏ hàng|shopping cart/i })).toBeVisible();
}

async function fillCheckoutAddress(page: Page) {
  await page.fill('input[name="name"]', "Auto Test User");
  await page.fill(
    'input[name="email"]',
    `autotest_${Date.now()}_${Math.floor(Math.random() * 10000)}@example.com`
  );
  const passwordField = page.locator('input[name="password"]');
  if (await passwordField.count()) {
    await passwordField.fill("Aa123456!");
  }
  await page.fill('input[name="phone"]', "0912345678");
  await page.fill('input[name="addressLine"]', "123 Test Street");
  const selectFirstValidOption = async (selector: string) => {
    const options = await page.locator(`${selector} option`).evaluateAll((nodes) =>
      nodes.map((node) => ({
        value: node.getAttribute("value") || "",
        text: (node.textContent || "").trim(),
      }))
    );
    const candidate = options.find(
      (opt) => opt.value && !/chọn|select/i.test(opt.text)
    );
    expect(candidate, `No valid option for ${selector}`).toBeTruthy();
    await page.selectOption(selector, candidate!.value);
    await page.waitForTimeout(600);
  };

  await selectFirstValidOption("#province");
  await selectFirstValidOption("#district");
  await selectFirstValidOption("#ward");
}

async function ensurePlaceOrderEnabled(page: Page) {
  const placeOrderBtn = page.getByRole("button", { name: /đặt hàng|place order/i });
  await expect(placeOrderBtn).toBeEnabled();
  return placeOrderBtn;
}

test.describe("Client Regression", () => {
  test("unauthorized profile/orders should redirect away", async ({ page }) => {
    await page.goto("/profile", { waitUntil: "networkidle" });
    await expect(page).not.toHaveURL(/\/profile$/);

    await page.goto("/orders", { waitUntil: "networkidle" });
    await expect(page).not.toHaveURL(/\/orders$/);
  });

  test("login wrong credentials should fail", async ({ request }) => {
    const response = await request.post(`${BACKEND_URL}/api/auth/login`, {
      data: {
        usernameOrEmail: "wrong@example.com",
        password: "wrong-password",
      },
    });
    expect(response.status()).toBe(401);
  });

  test("login valid credentials should succeed", async ({ request }) => {
    const response = await request.post(`${BACKEND_URL}/api/auth/login`, {
      data: {
        usernameOrEmail: VALID_LOGIN_EMAIL,
        password: VALID_LOGIN_PASSWORD,
      },
    });
    expect(response.status()).toBe(200);
  });

  test("voucher invalid and valid flows should both be handled", async ({ page, request }) => {
    await addOneProductToCart(page);
    await page.goto("/checkout", { waitUntil: "networkidle" });
    await fillCheckoutAddress(page);

    const voucherInput = page.locator("#voucher-code");
    await voucherInput.fill("INVALID_VOUCHER_CODE");
    await page.getByRole("button", { name: /áp dụng|apply/i }).click();
    await expect(page.getByText(/không hợp lệ|invalid/i)).toBeVisible();

    const bestVoucherRes = await request.get(
      `${BACKEND_URL}/api/vouchers/best?subTotal=1000000`
    );
    expect(bestVoucherRes.status()).toBe(200);
    const bestVoucherPayload = await bestVoucherRes.json();
    const suggestedCode = String(
      bestVoucherPayload?.data?.code ?? bestVoucherPayload?.code ?? ""
    ).trim();
    test.skip(!suggestedCode, "No active voucher available in current environment");

    await voucherInput.fill(suggestedCode);
    await page.getByRole("button", { name: /áp dụng|apply/i }).click();
    await expect(page.locator("text=/discount|giảm|mã/i").first()).toBeVisible();
  });

  test("payment MOMO should return payment URL", async ({ page }) => {
    await addOneProductToCart(page);
    await page.goto("/checkout", { waitUntil: "networkidle" });
    await fillCheckoutAddress(page);
    await page.locator('label:has-text("MoMo")').first().click();
    const placeOrderBtn = await ensurePlaceOrderEnabled(page);

    await placeOrderBtn.click();
    await page.waitForURL(/momo/i, { timeout: 30_000 });
  });

  test("payment VNPAY should return payment URL", async ({ page }) => {
    await addOneProductToCart(page);
    await page.goto("/checkout", { waitUntil: "networkidle" });
    await fillCheckoutAddress(page);
    await page.locator('label:has-text("VNPay")').first().click();
    const placeOrderBtn = await ensurePlaceOrderEnabled(page);

    await placeOrderBtn.click();
    await page.waitForURL(/vnp/i, { timeout: 30_000 });
  });
});
