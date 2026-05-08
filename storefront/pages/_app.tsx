import { NextComponentType, NextPageContext } from "next";
import Router from "next/router";
import { useEffect } from "react";
import { useRouter } from "next/router";
import axios from "axios";
import NProgress from "nprogress";
import { NextIntlProvider } from "next-intl";

import { ProvideCart } from "../context/cart/CartProvider";
import { ProvideWishlist } from "../context/wishlist/WishlistProvider";
import { ProvideAuth } from "../context/AuthContext";
import { ProvideCurrency } from "../context/CurrencyContext";
import { NotificationProvider } from "../context/NotificationContext";

import "../styles/globals.css";
import "animate.css";
import "nprogress/nprogress.css";

// Import Swiper styles
import "swiper/swiper.min.css";
import "swiper/components/navigation/navigation.min.css";
import "swiper/components/pagination/pagination.min.css";
import "swiper/components/scrollbar/scrollbar.min.css";

Router.events.on("routeChangeStart", () => NProgress.start());
Router.events.on("routeChangeComplete", () => NProgress.done());
Router.events.on("routeChangeError", () => NProgress.done());

type AppCustomProps = {
  Component: NextComponentType<NextPageContext, any, {}>;
  pageProps: any;
  cartState: string;
  wishlistState: string;
};

const MyApp = ({ Component, pageProps }: AppCustomProps) => {
  const router = useRouter();

  useEffect(() => {
    const requestInterceptorId = axios.interceptors.request.use((config) => {
      const nextConfig = { ...config };
      nextConfig.headers = nextConfig.headers || {};

      const locale = router.locale || "vi";
      const savedCurrency =
        typeof window !== "undefined" ? localStorage.getItem("currency_v2") : null;
      const normalizedCurrency = savedCurrency === "MMK" ? "MYN" : savedCurrency;
      const currency = normalizedCurrency || "VND";

      nextConfig.headers["Accept-Language"] = locale;
      nextConfig.headers["X-Currency"] = currency;
      return nextConfig;
    });

    const responseInterceptorId = axios.interceptors.response.use(
      (response) => {
        const payload = response?.data;
        if (
          payload &&
          typeof payload === "object" &&
          Object.prototype.hasOwnProperty.call(payload, "meta") &&
          Object.prototype.hasOwnProperty.call(payload, "data")
        ) {
          response.data = payload.data;
        }
        return response;
      },
      (error) => {
        if (error?.response?.data?.data) {
          error.response.data = error.response.data.data;
        }
        return Promise.reject(error);
      }
    );

    return () => {
      axios.interceptors.request.eject(requestInterceptorId);
      axios.interceptors.response.eject(responseInterceptorId);
    };
  }, [router.locale]);

  useEffect(() => {
    if (
      process.env.NODE_ENV === "development" &&
      typeof window !== "undefined" &&
      "serviceWorker" in navigator
    ) {
      navigator.serviceWorker
        .getRegistrations()
        .then((registrations) => {
          registrations.forEach((registration) => {
            registration.unregister();
          });
        })
        .catch(() => undefined);
    }
  }, []);

  return (
    <NextIntlProvider messages={pageProps?.messages}>
      <NotificationProvider>
        <ProvideCurrency>
          <ProvideAuth>
            <ProvideWishlist>
              <ProvideCart>
                <Component {...pageProps} />
              </ProvideCart>
            </ProvideWishlist>
          </ProvideAuth>
        </ProvideCurrency>
      </NotificationProvider>
    </NextIntlProvider>
  );
};

export default MyApp;
