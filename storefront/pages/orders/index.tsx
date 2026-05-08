import { useEffect, useState } from "react";
import { GetStaticProps } from "next";
import Link from "next/link";
import { useRouter } from "next/router";
import axios from "axios";
import { useTranslations } from "next-intl";

import Header from "../../components/Header/Header";
import Footer from "../../components/Footer/Footer";
import { useAuth } from "../../context/AuthContext";
import { useCurrency } from "../../context/CurrencyContext";

type OrderItem = {
  id: number;
  productName?: string;
  quantity?: number;
  price?: number;
  lineTotal?: number;
};

type Order = {
  id: number;
  status?: string;
  paymentMethod?: string;
  totalPrice?: number;
  subTotal?: number;
  shippingFee?: number;
  createdAt?: string;
  items?: OrderItem[];
};

type PagedOrderResponse = {
  content?: Order[];
  page?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
  first?: boolean;
  last?: boolean;
};

const OrdersPage = () => {
  const t = useTranslations("Orders");
  const auth = useAuth();
  const router = useRouter();
  const { formatPrice } = useCurrency();
  const [orders, setOrders] = useState<Order[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 10;
  const [totalPages, setTotalPages] = useState(1);

  useEffect(() => {
    if (!auth.isAuthReady) return;
    if (!auth.user) {
      router.push("/");
      return;
    }
    setCurrentPage(1);
  }, [auth.isAuthReady, auth.user, router]);

  useEffect(() => {
    const token = auth.user?.token;
    if (!token) return;
    let active = true;
    const loadOrders = async () => {
      setIsLoading(true);
      setError("");
      try {
        const res = await axios.get<PagedOrderResponse>(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/orders/my`,
          {
            params: {
              page: Math.max(0, currentPage - 1),
              size: pageSize,
            },
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        );
        if (!active) return;
        setOrders(Array.isArray(res.data?.content) ? res.data.content : []);
        setTotalPages(Math.max(1, Number(res.data?.totalPages || 1)));
      } catch (err) {
        console.error("Load orders failed:", err);
        if (!active) return;
        setError(t("cannot_load_orders"));
      } finally {
        if (!active) return;
        setIsLoading(false);
      }
    };
    loadOrders();
    return () => {
      active = false;
    };
  }, [auth.user?.token, currentPage]);

  if (!auth.isAuthReady || !auth.user) return null;

  return (
    <div>
      <Header title={`${t("my_orders")} - Twenty`} />
      <main id="main-content" className="app-max-width app-x-padding py-8 md:py-10">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-3xl">{t("my_orders")}</h1>
          <Link href="/profile">
            <a className="px-4 py-2 border border-gray300 hover:bg-gray100">{t("profile")}</a>
          </Link>
        </div>

        {isLoading && <p>{t("loading")}</p>}
        {error && <p className="text-red">{error}</p>}

        {!isLoading && !error && orders.length === 0 && (
          <div className="border border-gray200 p-6 text-gray500">{t("no_orders")}</div>
        )}

        {!isLoading && !error && orders.length > 0 && (
          <div className="space-y-4">
            <div className="overflow-auto border border-gray200">
            <table className="w-full min-w-[760px]">
              <thead>
                <tr className="border-b border-gray200 bg-gray100">
                  <th className="text-left font-medium px-4 py-3">{t("order_code")}</th>
                  <th className="text-left font-medium px-4 py-3">{t("created_at")}</th>
                  <th className="text-left font-medium px-4 py-3">{t("status")}</th>
                  <th className="text-left font-medium px-4 py-3">{t("payment")}</th>
                  <th className="text-right font-medium px-4 py-3">{t("total")}</th>
                  <th className="text-right font-medium px-4 py-3">{t("detail")}</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.id} className="border-b border-gray200">
                    <td className="px-4 py-3">#{order.id}</td>
                    <td className="px-4 py-3">
                      {order.createdAt
                        ? new Date(order.createdAt).toLocaleString(
                            router.locale === "en" ? "en-US" : "vi-VN"
                          )
                        : "-"}
                    </td>
                    <td className="px-4 py-3">{order.status || "-"}</td>
                    <td className="px-4 py-3">{order.paymentMethod || "-"}</td>
                    <td className="px-4 py-3 text-right">
                      {formatPrice(order.totalPrice || 0)}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <Link href={`/orders/${order.id}`}>
                        <a className="text-blue-600 hover:underline">{t("view")}</a>
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-gray400">
                {t("page")} {currentPage}/{totalPages}
              </span>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setCurrentPage((prev) => Math.max(1, prev - 1))}
                  disabled={currentPage <= 1}
                  className="px-3 py-1 border border-gray300 disabled:opacity-50"
                >
                  {t("prev")}
                </button>
                <button
                  type="button"
                  onClick={() =>
                    setCurrentPage((prev) => Math.min(totalPages, prev + 1))
                  }
                  disabled={currentPage >= totalPages}
                  className="px-3 py-1 border border-gray300 disabled:opacity-50"
                >
                  {t("next")}
                </button>
              </div>
            </div>
          </div>
        )}
      </main>
      <Footer />
    </div>
  );
};

export const getStaticProps: GetStaticProps = async ({ locale }) => {
  return {
    props: {
      messages: (await import(`../../messages/common/${locale}.json`)).default,
    },
  };
};

export default OrdersPage;
