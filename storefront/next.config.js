const productionBackendUrl = "https://clothing-api-znok.onrender.com";
const googleClientId =
  "44695475394-6mj3nra82h8mv483juk05pv86t8sbdo3.apps.googleusercontent.com";

module.exports = {
  i18n: {
    locales: ["en", "my", "vi"],
    defaultLocale: "vi",
  },
  env: {
    NEXT_PUBLIC_BACKEND_URL: process.env.VERCEL
      ? productionBackendUrl
      : process.env.NEXT_PUBLIC_BACKEND_URL || productionBackendUrl,
    NEXT_PUBLIC_PROD_BACKEND_URL: productionBackendUrl,
    NEXT_PUBLIC_GOOGLE_CLIENT_ID:
      process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || googleClientId,
  },
  reactStrictMode: true,
  // swcMinify: true,
  compiler: {
    removeConsole: true,
  },
  images: {
    domains: [
      "robohash.org",
      "res.cloudinary.com",
      "pub-1726861237464c8c92a49dc524de988f.r2.dev",
      "example.com",
    ],
  },
  webpack: (config, { dev }) => {
    config.resolve.alias = {
      ...(config.resolve.alias || {}),
      "next-intl": require("path").resolve(__dirname, "lib/next-intl.tsx"),
    };
    if (dev) {
      // Keep dev fast with memory cache, while avoiding flaky filesystem pack cache (.next/cache/webpack/*.pack).
      config.cache = {
        type: "memory",
      };
      config.watchOptions = {
        poll: 1000,
        aggregateTimeout: 300,
      };
    }
    return config;
  },
  async headers() {
    return [
      {
        source: "/_next/static/:path*",
        headers: [
          {
            key: "Cache-Control",
            value: "public, max-age=31536000, immutable",
          },
        ],
      },
      {
        source: "/_next/data/:path*",
        headers: [
          {
            key: "Cache-Control",
            value: "no-store, no-cache, must-revalidate, proxy-revalidate",
          },
        ],
      },
      {
        source: "/((?!_next/static|_next/image|favicon.ico).*)",
        headers: [
          {
            key: "Cache-Control",
            value: "no-store, no-cache, must-revalidate, proxy-revalidate",
          },
          {
            key: "X-Frame-Options",
            value: "DENY",
          },
          {
            key: "X-Content-Type-Options",
            value: "nosniff",
          },
          {
            key: "Referrer-Policy",
            value: "strict-origin-when-cross-origin",
          },
          {
            key: "Permissions-Policy",
            value: "camera=(), microphone=(), geolocation=()",
          },
        ],
      },
    ];
  },
};
