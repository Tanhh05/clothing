import { expect, test } from "@playwright/test";

const VALID_USERNAME = "admin";
const VALID_PASSWORD = "123456";

test.describe("Admin Regression", () => {
  test("unauthorized dashboard access should redirect to login", async ({
    page,
  }) => {
    await page.goto("/#/dashboard", { waitUntil: "networkidle" });
    await expect(page).toHaveURL(/#\/login/);
  });

  test("admin login wrong credentials should fail", async ({ page }) => {
    await page.goto("/#/login", { waitUntil: "networkidle" });
    await page.fill('input[name="username"]', "wrong-admin");
    await page.fill('input[name="password"]', "wrong-password");

    const loginResponsePromise = page.waitForResponse((response) => {
      return (
        response.url().includes("/api/auth/login") &&
        response.request().method() === "POST"
      );
    });

    await page.getByRole("button", { name: /đăng nhập|login/i }).first().click();
    const loginResponse = await loginResponsePromise;
    expect(loginResponse.status()).toBe(401);
    await expect(page).toHaveURL(/#\/login/);
  });

  test("admin login valid credentials should succeed", async ({ page }) => {
    await page.goto("/#/login", { waitUntil: "networkidle" });
    await page.fill('input[name="username"]', VALID_USERNAME);
    await page.fill('input[name="password"]', VALID_PASSWORD);

    const loginResponsePromise = page.waitForResponse((response) => {
      return (
        response.url().includes("/api/auth/login") &&
        response.request().method() === "POST"
      );
    });

    await page.getByRole("button", { name: /đăng nhập|login/i }).first().click();
    const loginResponse = await loginResponsePromise;
    expect(loginResponse.status()).toBe(200);

    await expect(page).toHaveURL(/#\/(overview\/dashboard|dashboard)/);
  });
});
