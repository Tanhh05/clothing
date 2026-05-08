import { useEffect, useState } from "react";
import { GetServerSideProps } from "next";
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
  sku?: string;
};

type Order = {
  id: number;
  status?: string;
  paymentMethod?: string;
  totalPrice?: number;
  subTotal?: number;
  shippingFee?: number;
  discountAmount?: number;
  address?: string;
  shippingStatus?: string;
  createdAt?: string;
  items?: OrderItem[];
};

type ApiEnvelope<T> = {
  data?: T;
};

const extractPayload = <T,>(input: T | ApiEnvelope<T> | undefined): T | undefined => {
  if (!input || typeof input !== "object") return input as T | undefined;
  const wrapped = input as ApiEnvelope<T>;
  return wrapped.data ?? (input as T);
};

const isCancelableOrder = (status?: string) =>
  ["WAITING_PAYMENT", "PENDING", "PROCESSING", "CONFIRMED"].includes(
    String(status || "").toUpperCase()
  );

const OrderDetailPage = () => {
  const t = useTranslations("Orders");
  const auth = useAuth();
  const router = useRouter();
  const { formatPrice } = useCurrency();
  const [order, setOrder] = useState<Order | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!auth.isAuthReady) return;
    if (!auth.user) {
      router.push("/");
      return;
    }
    const id = router.query.id;
    if (!id || Array.isArray(id)) return;

    let active = true;
    const loadOrder = async () => {
      if (!auth.user?.token) return;
      setIsLoading(true);
      setError("");
      try {
        const res = await axios.get<Order | ApiEnvelope<Order>>(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/orders/my/${id}`,
          {
            headers: {
              Authorization: `Bearer ${auth.user.token}`,
            },
          }
        );
        if (!active) return;
        setOrder(extractPayload<Order>(res.data) || null);
      } catch (err) {
        console.error("Load order detail failed:", err);
        if (!active) return;
        setError(t("cannot_load_order_detail"));
      } finally {
        if (!active) return;
        setIsLoading(false);
      }
    };
    loadOrder();
    return () => {
      active = false;
    };
  }, [auth.isAuthReady, auth.user, router, router.query.id]);

  const handleCancelOrder = async () => {
    if (!auth.user?.token || !order?.id) return;
    if (!window.confirm(t("cancel_confirm"))) return;
    setIsCancelling(true);
    setError("");
    try {
      const res = await axios.patch<Order | ApiEnvelope<Order>>(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/orders/my/${order.id}/cancel`,
        {},
        {
          headers: {
            Authorization: `Bearer ${auth.user.token}`,
          },
        }
      );
      setOrder(extractPayload<Order>(res.data) || { ...order, status: "CANCELLED" });
    } catch (err) {
      console.error("Cancel order failed:", err);
      setError(t("cannot_cancel_order"));
    } finally {
      setIsCancelling(false);
    }
  };

  if (!auth.isAuthReady || !auth.user) return null;

  return (
    <div>
      <Header title={`${t("order_detail")} - Twenty`} />
      <main id="main-content" className="app-max-width app-x-padding py-8 md:py-10">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-3xl">{t("order_detail")}</h1>
          <Link href="/orders">
            <a className="px-4 py-2 border border-gray300 hover:bg-gray100">{t("back")}</a>
          </Link>
        </div>

        {isLoading && <p>{t("loading")}</p>}
        {error && <p className="text-red">{error}</p>}

        {!isLoading && !error && order && (
          <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
            <section className="xl:col-span-2 border border-gray200 p-5">
              <h2 className="text-xl mb-4">{t("products")}</h2>
              <div className="overflow-auto">
                <table className="w-full min-w-[640px]">
                  <thead>
                    <tr className="border-b border-gray200 bg-gray100">
                      <th className="text-left font-medium px-4 py-3">{t("product")}</th>
                      <th className="text-left font-medium px-4 py-3">SKU</th>
                      <th className="text-right font-medium px-4 py-3">{t("qty")}</th>
                      <th className="text-right font-medium px-4 py-3">{t("unit_price")}</th>
                      <th className="text-right font-medium px-4 py-3">{t("line_total")}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(order.items || []).map((item) => (
                      <tr key={item.id} className="border-b border-gray200">
                        <td className="px-4 py-3">{item.productName || "-"}</td>
                        <td className="px-4 py-3">{item.sku || "-"}</td>
                        <td className="px-4 py-3 text-right">{item.quantity || 0}</td>
                        <td className="px-4 py-3 text-right">
                          {formatPrice(item.price || 0)}
                        </td>
                        <td className="px-4 py-3 text-right">
                          {formatPrice(item.lineTotal || 0)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>

            <section className="border border-gray200 p-5">
              <h2 className="text-xl mb-4">{t("order_info")}</h2>
              <div className="space-y-2 text-sm">
                <p>
                  <span className="text-gray400">{t("order_code")}:</span> #{order.id}
                </p>
                <p>
                  <span className="text-gray400">{t("created_at")}:</span>{" "}
                  {order.createdAt
                    ? new Date(order.createdAt).toLocaleString(
                        router.locale === "en" ? "en-US" : "vi-VN"
                      )
                    : "-"}
                </p>
                <p>
                  <span className="text-gray400">{t("status")}:</span>{" "}
                  {order.status || "-"}
                </p>
                <p>
                  <span className="text-gray400">{t("shipping")}:</span>{" "}
                  {order.shippingStatus || "-"}
                </p>
                <p>
                  <span className="text-gray400">{t("payment")}:</span>{" "}
                  {order.paymentMethod || "-"}
                </p>
                <p>
                  <span className="text-gray400">{t("address")}:</span>{" "}
                  {order.address || "-"}
                </p>
              </div>
              <div className="border-t border-gray200 mt-4 pt-4 space-y-2 text-sm">
                <div className="flex justify-between">
                  <span>{t("subtotal")}</span>
                  <span>{formatPrice(order.subTotal || 0)}</span>
                </div>
                <div className="flex justify-between">
                  <span>{t("shipping_fee")}</span>
                  <span>{formatPrice(order.shippingFee || 0)}</span>
                </div>
                <div className="flex justify-between">
                  <span>{t("discount")}</span>
                  <span>{formatPrice(order.discountAmount || 0)}</span>
                </div>
                <div className="flex justify-between text-base font-semibold border-t border-gray200 pt-2">
                  <span>{t("total")}</span>
                  <span>{formatPrice(order.totalPrice || 0)}</span>
                </div>
              </div>
              {isCancelableOrder(order.status) && (
                <button
                  type="button"
                  onClick={handleCancelOrder}
                  disabled={isCancelling}
                  className="mt-5 w-full border border-gray300 text-red px-4 py-2 hover:bg-gray100 disabled:opacity-50"
                >
                  {isCancelling ? t("cancelling") : t("cancel_order")}
                </button>
              )}
            </section>
          </div>
        )}
      </main>
      <Footer />
    </div>
  );
};

export const getServerSideProps: GetServerSideProps = async ({ locale }) => {
  return {
    props: {
      messages: (await import(`../../messages/common/${locale}.json`)).default,
    },
  };
};

export default OrderDetailPage;
