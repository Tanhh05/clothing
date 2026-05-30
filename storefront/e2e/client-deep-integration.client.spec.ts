import { expect, test } from "@playwright/test";

const BACKEND_URL = "http://localhost:8080";

function unwrap<T>(payload: any): T {
  return (payload?.data ?? payload) as T;
}

async function registerUser(request: any, nonce: number) {
  const email = `autodeep_${nonce}@example.com`;
  const username = `autodeep_${nonce}`;
  const password = "Aa123456!";

  const registerRes = await request.post(`${BACKEND_URL}/api/auth/register`, {
    data: {
      email,
      fullName: "Auto Deep User",
      username,
      password,
      shippingAddress: "123 Test Street",
      phone: "0912345678",
    },
  });
  expect(registerRes.status()).toBeGreaterThanOrEqual(200);
  expect(registerRes.status()).toBeLessThan(300);
  const body = await registerRes.json();
  const data = unwrap<any>(body);
  const token = String(data?.accessToken || "");
  expect(token.length).toBeGreaterThan(0);
  return { email, username, password, token };
}

test.describe("Client Deep Integration", () => {
  test("address + wishlist + cart + order + notifications flow", async ({
    request,
  }) => {
    const nonce = Date.now();
    const user = await registerUser(request, nonce);
    const headers = { Authorization: `Bearer ${user.token}` };

    // Address CRUD
    const createAddressRes = await request.post(`${BACKEND_URL}/api/user/addresses`, {
      headers,
      data: {
        recipientName: "Auto Receiver",
        phone: "0912345678",
        province: "Lào Cai",
        district: "Huyện Si Ma Cai",
        ward: "Thị Trấn Si Ma Cai",
        addressLine: "123 Test Street",
        isDefault: true,
      },
    });
    expect(createAddressRes.ok()).toBeTruthy();
    const createdAddress = unwrap<any>(await createAddressRes.json());
    const addressId = Number(createdAddress?.id);
    expect(addressId).toBeGreaterThan(0);

    const updateAddressRes = await request.put(
      `${BACKEND_URL}/api/user/addresses/${addressId}`,
      {
        headers,
        data: {
          recipientName: "Auto Receiver Updated",
          phone: "0912345678",
          province: "Lào Cai",
          district: "Huyện Si Ma Cai",
          ward: "Thị Trấn Si Ma Cai",
          addressLine: "456 Test Street",
          isDefault: true,
        },
      }
    );
    expect(updateAddressRes.ok()).toBeTruthy();

    // Product/variant discovery
    const productsRes = await request.get(`${BACKEND_URL}/api/products?page=0&size=5`);
    expect(productsRes.ok()).toBeTruthy();
    const productsPage = unwrap<any>(await productsRes.json());
    const products = Array.isArray(productsPage?.content) ? productsPage.content : [];
    expect(products.length).toBeGreaterThan(0);
    const productId = Number(products[0].id);
    expect(productId).toBeGreaterThan(0);

    const productDetailRes = await request.get(`${BACKEND_URL}/api/products/${productId}`);
    expect(productDetailRes.ok()).toBeTruthy();
    const productDetail = unwrap<any>(await productDetailRes.json());
    const variants = Array.isArray(productDetail?.variants) ? productDetail.variants : [];
    const activeVariant = variants.find((v: any) => Number(v.stock || 0) > 0);
    expect(activeVariant).toBeTruthy();
    const variantId = Number(activeVariant.id);
    expect(variantId).toBeGreaterThan(0);

    // Wishlist add/remove
    const addWishlistRes = await request.post(`${BACKEND_URL}/api/wishlist/items`, {
      headers,
      data: { productId },
    });
    expect(addWishlistRes.ok()).toBeTruthy();
    const listWishlistRes = await request.get(`${BACKEND_URL}/api/wishlist`, { headers });
    expect(listWishlistRes.ok()).toBeTruthy();
    const wishlistData = unwrap<any>(await listWishlistRes.json());
    const productIds = Array.isArray(wishlistData?.productIds)
      ? wishlistData.productIds.map((id: any) => Number(id))
      : [];
    expect(productIds.includes(productId)).toBeTruthy();

    const deleteWishlistRes = await request.delete(
      `${BACKEND_URL}/api/wishlist/items/${productId}`,
      { headers }
    );
    expect(deleteWishlistRes.ok()).toBeTruthy();

    // Cart add/update/delete path
    const addCartRes = await request.post(`${BACKEND_URL}/api/cart/items`, {
      headers,
      data: { variantId, quantity: 1 },
    });
    expect(addCartRes.ok()).toBeTruthy();

    const cartRes = await request.get(`${BACKEND_URL}/api/cart`, { headers });
    expect(cartRes.ok()).toBeTruthy();
    const cart = unwrap<any>(await cartRes.json());
    const cartItems = Array.isArray(cart?.items) ? cart.items : [];
    expect(cartItems.length).toBeGreaterThan(0);
    const cartItemId = Number(cartItems[0].id);
    expect(cartItemId).toBeGreaterThan(0);

    const updateCartRes = await request.put(
      `${BACKEND_URL}/api/cart/items/${cartItemId}`,
      { headers, data: { quantity: 1 } }
    );
    expect(updateCartRes.ok()).toBeTruthy();

    // Order creation (COD)
    const orderRes = await request.post(`${BACKEND_URL}/api/orders`, {
      headers: {
        ...headers,
        "X-Idempotency-Key": `e2e-${nonce}`,
      },
      data: {
        paymentMethod: "COD",
        address: "456 Test Street, Thị Trấn Si Ma Cai, Huyện Si Ma Cai, Lào Cai",
        recipientName: "Auto Receiver Updated",
        phone: "0912345678",
        province: "Lào Cai",
        district: "Huyện Si Ma Cai",
        ward: "Thị Trấn Si Ma Cai",
      },
    });
    expect(orderRes.ok()).toBeTruthy();
    const order = unwrap<any>(await orderRes.json());
    const orderId = Number(order?.id);
    expect(orderId).toBeGreaterThan(0);

    const myOrdersRes = await request.get(`${BACKEND_URL}/api/orders/my?page=0&size=10`, {
      headers,
    });
    expect(myOrdersRes.ok()).toBeTruthy();

    const orderDetailRes = await request.get(`${BACKEND_URL}/api/orders/my/${orderId}`, {
      headers,
    });
    expect(orderDetailRes.ok()).toBeTruthy();

    const userNotificationsRes = await request.get(
      `${BACKEND_URL}/api/user/notifications`,
      { headers }
    );
    expect(userNotificationsRes.ok()).toBeTruthy();

    // Cleanup address
    const deleteAddressRes = await request.delete(
      `${BACKEND_URL}/api/user/addresses/${addressId}`,
      { headers }
    );
    expect(deleteAddressRes.ok()).toBeTruthy();
  });
});

