module.exports = {
  i18n: {
    locales: ["en", "my", "vi"],
    defaultLocale: "vi",
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
      // Avoid unstable filesystem pack cache in dev (ENOENT rename/stat on .next/cache/webpack/*.pack).
      config.cache = false;
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
        ],
      },
    ];
  },
};
