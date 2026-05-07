const withPWA = require("next-pwa");

module.exports = withPWA({
  // module.exports = {
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
  pwa: {
    dest: "public",
    skipWaiting: true,
    disable: process.env.NODE_ENV === "development",
  },
  // };
});
