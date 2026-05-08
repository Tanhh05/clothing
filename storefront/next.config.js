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
  webpack: (config) => {
    config.resolve.alias = {
      ...(config.resolve.alias || {}),
      "next-intl": require("path").resolve(__dirname, "lib/next-intl.tsx"),
    };
    return config;
  },
};
