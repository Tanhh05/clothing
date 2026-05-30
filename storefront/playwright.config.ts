import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  timeout: 90_000,
  expect: {
    timeout: 10_000,
  },
  fullyParallel: true,
  retries: 0,
  reporter: [["list"], ["html", { open: "never" }]],
  use: {
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  webServer: [
    {
      command: "npm run dev",
      port: 3000,
      reuseExistingServer: true,
      timeout: 180_000,
    },
    {
      command: "npm run dev",
      cwd: "../admin-portal",
      port: 5174,
      reuseExistingServer: true,
      timeout: 240_000,
    },
  ],
  projects: [
    {
      name: "client",
      testMatch: /.*client\.spec\.ts/,
      use: {
        ...devices["Desktop Chrome"],
        baseURL: "http://localhost:3000",
      },
    },
    {
      name: "admin",
      testMatch: /.*admin\.spec\.ts/,
      use: {
        ...devices["Desktop Chrome"],
        baseURL: "http://localhost:5174",
      },
    },
  ],
});

