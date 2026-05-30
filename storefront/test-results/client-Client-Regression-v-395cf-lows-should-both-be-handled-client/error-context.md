# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: client.spec.ts >> Client Regression >> voucher invalid and valid flows should both be handled
- Location: e2e/client.spec.ts:179:7

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: getByRole('heading', { name: /giỏ hàng|shopping cart/i })
Expected: visible
Timeout: 10000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" with timeout 10000ms
  - waiting for getByRole('heading', { name: /giỏ hàng|shopping cart/i })

```

```yaml
- dialog "Unhandled Runtime Error":
  - navigation:
    - button [disabled]:
      - img
    - button [disabled]:
      - img
    - text: 1 of 1 unhandled error
  - button "Close"
  - heading "Unhandled Runtime Error" [level=1]
  - paragraph: "Error: Image is missing required \"src\" property. Make sure you pass \"src\" in props to the `next/image` component. Received: {\"width\":95,\"height\":128}"
  - heading "Call Stack" [level=5]
  - heading "Image" [level=6]
  - text: node_modules/next/dist/client/image.js (366:18)
  - heading "renderWithHooks" [level=6]
  - text: node_modules/react-dom/cjs/react-dom.development.js (14985:0)
  - heading "mountIndeterminateComponent" [level=6]
  - text: node_modules/react-dom/cjs/react-dom.development.js (17811:0)
  - heading "beginWork" [level=6]
  - text: node_modules/react-dom/cjs/react-dom.development.js (19049:0)
  - heading "HTMLUnknownElement.callCallback" [level=6]
  - text: node_modules/react-dom/cjs/react-dom.development.js (3945:0)
  - heading "Object.invokeGuardedCallbackDev" [level=6]
  - text: node_modules/react-dom/cjs/react-dom.development.js (3994:0)
  - heading "invokeGuardedCallback" [level=6]
  - text: node_modules/react-dom/cjs/react-dom.development.js (4056:0)
  - heading "beginWork$1" [level=6]
  - text: node_modules/react-dom/cjs/react-dom.development.js (23964:0)
  - heading "performUnitOfWork" [level=6]
  - text: node_modules/react-dom/cjs/react-dom.development.js (22776:0)
  - heading "workLoopSync" [level=6]
  - text: node_modules/react-dom/cjs/react-dom.development.js (22707:0)
  - heading "renderRootSync" [level=6]
  - text: node_modules/react-dom/cjs/react-dom.development.js (22670:0)
  - heading "performSyncWorkOnRoot" [level=6]
  - text: node_modules/react-dom/cjs/react-dom.development.js (22293:0)
  - heading "eval" [level=6]
  - text: node_modules/react-dom/cjs/react-dom.development.js (11327:0)
  - heading "unstable_runWithPriority" [level=6]
  - text: node_modules/scheduler/cjs/scheduler.development.js (468:0)
  - heading "runWithPriority$1" [level=6]
  - text: node_modules/react-dom/cjs/react-dom.development.js (11276:0)
  - heading "flushSyncCallbackQueueImpl" [level=6]
  - text: node_modules/react-dom/cjs/react-dom.development.js (11322:0)
  - heading "flushSyncCallbackQueue" [level=6]
  - text: node_modules/react-dom/cjs/react-dom.development.js (11309:0)
  - heading "flushPassiveEffectsImpl" [level=6]
  - text: node_modules/react-dom/cjs/react-dom.development.js (23620:0)
  - heading "unstable_runWithPriority" [level=6]
  - text: node_modules/scheduler/cjs/scheduler.development.js (468:0)
  - heading "runWithPriority$1" [level=6]
  - text: node_modules/react-dom/cjs/react-dom.development.js (11276:0)
  - heading "flushPassiveEffects" [level=6]
  - text: node_modules/react-dom/cjs/react-dom.development.js (23447:0)
  - heading "eval" [level=6]
  - text: node_modules/react-dom/cjs/react-dom.development.js (23324:0)
  - heading "workLoop" [level=6]
  - text: node_modules/scheduler/cjs/scheduler.development.js (417:0)
  - heading "flushWork" [level=6]
  - text: node_modules/scheduler/cjs/scheduler.development.js (390:0)
  - heading "MessagePort.performWorkUntilDeadline" [level=6]
  - text: node_modules/scheduler/cjs/scheduler.development.js (157:0)
```

# Test source

```ts
  9   | }
  10  | 
  11  | async function getInStockVariantId(page: Page): Promise<number> {
  12  |   const productsRes = await page.request.get(`${BACKEND_URL}/api/products?page=0&size=10`);
  13  |   expect(productsRes.ok()).toBeTruthy();
  14  |   const productsPage = unwrap<any>(await productsRes.json());
  15  |   const products = Array.isArray(productsPage?.content) ? productsPage.content : [];
  16  |   expect(products.length).toBeGreaterThan(0);
  17  | 
  18  |   for (const product of products) {
  19  |     const productId = Number(product?.id);
  20  |     if (!productId) continue;
  21  |     const detailRes = await page.request.get(`${BACKEND_URL}/api/products/${productId}`);
  22  |     if (!detailRes.ok()) continue;
  23  |     const detail = unwrap<any>(await detailRes.json());
  24  |     const variants = Array.isArray(detail?.variants) ? detail.variants : [];
  25  |     const inStock = variants.find((v: any) => Number(v?.stock || 0) > 0);
  26  |     if (inStock?.id) return Number(inStock.id);
  27  |   }
  28  | 
  29  |   throw new Error("No in-stock product variant found for checkout tests");
  30  | }
  31  | 
  32  | async function bootstrapAuthenticatedUser(page: Page): Promise<string> {
  33  |   const loginRes = await page.request.post(`${BACKEND_URL}/api/auth/login`, {
  34  |     data: {
  35  |       usernameOrEmail: VALID_LOGIN_EMAIL,
  36  |       password: VALID_LOGIN_PASSWORD,
  37  |     },
  38  |   });
  39  |   expect(loginRes.ok()).toBeTruthy();
  40  |   const body = await loginRes.json();
  41  |   const data = unwrap<any>(body);
  42  |   const token = String(data?.accessToken || data?.token || "").trim();
  43  |   expect(token.length).toBeGreaterThan(0);
  44  | 
  45  |   const cookieUser = {
  46  |     id: Number(data?.userId || data?.id || 1),
  47  |     username: data?.username || VALID_LOGIN_EMAIL,
  48  |     email: data?.email || `${VALID_LOGIN_EMAIL}@example.com`,
  49  |     fullname: data?.fullName || data?.fullname || VALID_LOGIN_EMAIL,
  50  |     phone: data?.phone || "",
  51  |     token,
  52  |     refreshToken: data?.refreshToken || "",
  53  |     tokenType: data?.tokenType || "Bearer",
  54  |   };
  55  | 
  56  |   await page.context().addCookies([
  57  |     {
  58  |       name: "user",
  59  |       value: JSON.stringify(cookieUser),
  60  |       url: "http://localhost:3000",
  61  |     },
  62  |   ]);
  63  | 
  64  |   return token;
  65  | }
  66  | 
  67  | async function addOneProductToCart(page: Page) {
  68  |   await bootstrapAuthenticatedUser(page);
  69  |   const variantId = await getInStockVariantId(page);
  70  |   const productRes = await page.request.get(`${BACKEND_URL}/api/products?page=0&size=10`);
  71  |   expect(productRes.ok()).toBeTruthy();
  72  |   const productsPage = unwrap<any>(await productRes.json());
  73  |   const products = Array.isArray(productsPage?.content) ? productsPage.content : [];
  74  |   const product = products.find((p: any) => Number(p?.id) > 0) || products[0];
  75  |   expect(product).toBeTruthy();
  76  | 
  77  |   const productDetailRes = await page.request.get(
  78  |     `${BACKEND_URL}/api/products/${Number(product.id)}`
  79  |   );
  80  |   expect(productDetailRes.ok()).toBeTruthy();
  81  |   const productDetail = unwrap<any>(await productDetailRes.json());
  82  |   const variants = Array.isArray(productDetail?.variants) ? productDetail.variants : [];
  83  |   const variant = variants.find((v: any) => Number(v?.id) === variantId) || variants[0];
  84  |   expect(variant).toBeTruthy();
  85  | 
  86  |   const cartPayload = [
  87  |     {
  88  |       id: Number(product.id),
  89  |       slug: product.slug || "",
  90  |       name: product.name || "Auto Product",
  91  |       price: Number(product.price || 100000),
  92  |       qty: 1,
  93  |       img1: product.image1 || product.img1 || "",
  94  |       img2: product.image2 || product.img2 || "",
  95  |       selectedSize: variant?.size || "",
  96  |       selectedColor: variant?.color || "",
  97  |       selectedVariantId: Number(variant?.id || 0),
  98  |     },
  99  |   ];
  100 |   await page.context().addCookies([
  101 |     {
  102 |       name: "cart",
  103 |       value: JSON.stringify(cartPayload),
  104 |       url: "http://localhost:3000",
  105 |     },
  106 |   ]);
  107 | 
  108 |   await page.goto("/shopping-cart", { waitUntil: "networkidle" });
> 109 |   await expect(page.getByRole("heading", { name: /giỏ hàng|shopping cart/i })).toBeVisible();
      |                                                                                ^ Error: expect(locator).toBeVisible() failed
  110 | }
  111 | 
  112 | async function fillCheckoutAddress(page: Page) {
  113 |   await page.fill('input[name="name"]', "Auto Test User");
  114 |   await page.fill(
  115 |     'input[name="email"]',
  116 |     `autotest_${Date.now()}_${Math.floor(Math.random() * 10000)}@example.com`
  117 |   );
  118 |   const passwordField = page.locator('input[name="password"]');
  119 |   if (await passwordField.count()) {
  120 |     await passwordField.fill("Aa123456!");
  121 |   }
  122 |   await page.fill('input[name="phone"]', "0912345678");
  123 |   await page.fill('input[name="addressLine"]', "123 Test Street");
  124 |   const selectFirstValidOption = async (selector: string) => {
  125 |     const options = await page.locator(`${selector} option`).evaluateAll((nodes) =>
  126 |       nodes.map((node) => ({
  127 |         value: node.getAttribute("value") || "",
  128 |         text: (node.textContent || "").trim(),
  129 |       }))
  130 |     );
  131 |     const candidate = options.find(
  132 |       (opt) => opt.value && !/chọn|select/i.test(opt.text)
  133 |     );
  134 |     expect(candidate, `No valid option for ${selector}`).toBeTruthy();
  135 |     await page.selectOption(selector, candidate!.value);
  136 |     await page.waitForTimeout(600);
  137 |   };
  138 | 
  139 |   await selectFirstValidOption("#province");
  140 |   await selectFirstValidOption("#district");
  141 |   await selectFirstValidOption("#ward");
  142 | }
  143 | 
  144 | async function ensurePlaceOrderEnabled(page: Page) {
  145 |   const placeOrderBtn = page.getByRole("button", { name: /đặt hàng|place order/i });
  146 |   await expect(placeOrderBtn).toBeEnabled();
  147 |   return placeOrderBtn;
  148 | }
  149 | 
  150 | test.describe("Client Regression", () => {
  151 |   test("unauthorized profile/orders should redirect away", async ({ page }) => {
  152 |     await page.goto("/profile", { waitUntil: "networkidle" });
  153 |     await expect(page).not.toHaveURL(/\/profile$/);
  154 | 
  155 |     await page.goto("/orders", { waitUntil: "networkidle" });
  156 |     await expect(page).not.toHaveURL(/\/orders$/);
  157 |   });
  158 | 
  159 |   test("login wrong credentials should fail", async ({ request }) => {
  160 |     const response = await request.post(`${BACKEND_URL}/api/auth/login`, {
  161 |       data: {
  162 |         usernameOrEmail: "wrong@example.com",
  163 |         password: "wrong-password",
  164 |       },
  165 |     });
  166 |     expect(response.status()).toBe(401);
  167 |   });
  168 | 
  169 |   test("login valid credentials should succeed", async ({ request }) => {
  170 |     const response = await request.post(`${BACKEND_URL}/api/auth/login`, {
  171 |       data: {
  172 |         usernameOrEmail: VALID_LOGIN_EMAIL,
  173 |         password: VALID_LOGIN_PASSWORD,
  174 |       },
  175 |     });
  176 |     expect(response.status()).toBe(200);
  177 |   });
  178 | 
  179 |   test("voucher invalid and valid flows should both be handled", async ({ page, request }) => {
  180 |     await addOneProductToCart(page);
  181 |     await page.goto("/checkout", { waitUntil: "networkidle" });
  182 |     await fillCheckoutAddress(page);
  183 | 
  184 |     const voucherInput = page.locator("#voucher-code");
  185 |     await voucherInput.fill("INVALID_VOUCHER_CODE");
  186 |     await page.getByRole("button", { name: /áp dụng|apply/i }).click();
  187 |     await expect(page.getByText(/không hợp lệ|invalid/i)).toBeVisible();
  188 | 
  189 |     const bestVoucherRes = await request.get(
  190 |       `${BACKEND_URL}/api/vouchers/best?subTotal=1000000`
  191 |     );
  192 |     expect(bestVoucherRes.status()).toBe(200);
  193 |     const bestVoucherPayload = await bestVoucherRes.json();
  194 |     const suggestedCode = String(
  195 |       bestVoucherPayload?.data?.code ?? bestVoucherPayload?.code ?? ""
  196 |     ).trim();
  197 |     test.skip(!suggestedCode, "No active voucher available in current environment");
  198 | 
  199 |     await voucherInput.fill(suggestedCode);
  200 |     await page.getByRole("button", { name: /áp dụng|apply/i }).click();
  201 |     await expect(page.locator("text=/discount|giảm|mã/i").first()).toBeVisible();
  202 |   });
  203 | 
  204 |   test("payment MOMO should return payment URL", async ({ page }) => {
  205 |     await addOneProductToCart(page);
  206 |     await page.goto("/checkout", { waitUntil: "networkidle" });
  207 |     await fillCheckoutAddress(page);
  208 |     await page.locator('label:has-text("MoMo")').first().click();
  209 |     const placeOrderBtn = await ensurePlaceOrderEnabled(page);
```