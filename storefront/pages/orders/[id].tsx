import { useEffect, useRef, useState } from "react";
import { GetServerSideProps } from "next";
import Link from "next/link";
import Image from "next/image";
import { useRouter } from "next/router";
import axios from "axios";
import { useTranslations } from "next-intl";

import Header from "../../components/Header/Header";
import Footer from "../../components/Footer/Footer";
import AccountPageLayout from "../../components/Account/AccountPageLayout";
import { useAuth } from "../../context/AuthContext";
import { useCurrency } from "../../context/CurrencyContext";
import { pushWithLang } from "../../lib/router-utils";

type OrderItem = {
  id: number;
  productId?: number;
  productName?: string;
  productImage?: string;
  quantity?: number;
  price?: number;
  lineTotal?: number;
  sku?: string;
};

type OrderStatusHistory = {
  status?: string;
  changedAt?: string;
};

type Order = {
  id: number;
  shippingCode?: string;
  status?: string;
  paymentMethod?: string;
  totalPrice?: number;
  subTotal?: number;
  shippingFee?: number;
  discountAmount?: number;
  appliedVoucherCode?: string;
  address?: string;
  shippingStatus?: string;
  shippingUpdatedAt?: string;
  createdAt?: string;
  items?: OrderItem[];
  statusHistory?: OrderStatusHistory[];
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

const isTerminalOrderStatus = (status?: string) =>
  ["DELIVERED", "CANCELLED", "RETURNED", "FAILED"].includes(
    String(status || "").toUpperCase()
  );

const getInvoiceCode = (order: Order) =>
  order.shippingCode && order.shippingCode.trim()
    ? order.shippingCode.trim()
    : `HD${order.id}`;

const OrderDetailPage = () => {
  const t = useTranslations("Orders");
  const auth = useAuth();
  const router = useRouter();
  const { formatPrice } = useCurrency();
  const token = auth.user?.token;
  const [order, setOrder] = useState<Order | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);
  const [isReordering, setIsReordering] = useState(false);
  const [reviewRatings, setReviewRatings] = useState<Record<number, number>>({});
  const [reviewComments, setReviewComments] = useState<Record<number, string>>({});
  const [reviewLoading, setReviewLoading] = useState<Record<number, boolean>>({});
  const [reviewSuccess, setReviewSuccess] = useState<Record<number, string>>({});
  const [reviewError, setReviewError] = useState<Record<number, string>>({});
  const [errorKey, setErrorKey] = useState("");
  const [productImageMap, setProductImageMap] = useState<Record<number, string>>({});
  const latestOrderStatusRef = useRef<string>("");
  const isFetchingOrderRef = useRef(false);
  const orderId = router.query.id;

  useEffect(() => {
    latestOrderStatusRef.current = String(order?.status || "").toUpperCase();
  }, [order?.status]);

  useEffect(() => {
    if (!auth.isAuthReady || token) return;
    pushWithLang(router, "/");
  }, [auth.isAuthReady, token, router]);

  useEffect(() => {
    if (!auth.isAuthReady || !token) return;
    if (!orderId || Array.isArray(orderId)) return;

    let active = true;
    const loadOrder = async (isSilent?: boolean) => {
      if (isFetchingOrderRef.current) return;
      isFetchingOrderRef.current = true;
      if (!isSilent) setIsLoading(true);
      if (!isSilent) setErrorKey("");
      try {
        const res = await axios.get<Order | ApiEnvelope<Order>>(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/orders/my/${orderId}`,
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        );
        if (!active) return;
        setOrder(extractPayload<Order>(res.data) || null);
      } catch (err) {
        console.error("Load order detail failed:", err);
        if (!active) return;
        setErrorKey("cannot_load_order_detail");
      } finally {
        isFetchingOrderRef.current = false;
        if (!active) return;
        if (!isSilent) setIsLoading(false);
      }
    };
    loadOrder();
    const timer = setInterval(() => {
      if (!active) return;
      if (isTerminalOrderStatus(latestOrderStatusRef.current)) return;
      loadOrder(true);
    }, 20000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [auth.isAuthReady, token, orderId]);

  useEffect(() => {
    const productIds = new Set<number>();
    (order?.items || []).forEach((item) => {
      if (item.productId) productIds.add(item.productId);
    });
    const idsToFetch = Array.from(productIds).filter((id) => !productImageMap[id]);
    if (idsToFetch.length === 0) return;
    let cancelled = false;
    const loadImages = async () => {
      const entries = await Promise.all(
        idsToFetch.map(async (id) => {
          try {
            const res = await axios.get(
              `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/products/${id}`
            );
            const payload = extractPayload<any>(res.data) || {};
            const images = Array.isArray(payload.images) ? payload.images : [];
            const main = images.find((img: any) => img?.isMain && img?.url)?.url;
            const fallback = images.find((img: any) => img?.url)?.url;
            return [id, main || fallback || ""] as const;
          } catch {
            return [id, ""] as const;
          }
        })
      );
      if (cancelled) return;
      setProductImageMap((prev) => {
        const next = { ...prev };
        entries.forEach(([id, url]) => {
          if (url) next[id] = url;
        });
        return next;
      });
    };
    loadImages();
    return () => {
      cancelled = true;
    };
  }, [order, productImageMap]);

  const handleCancelOrder = async () => {
    if (!token || !order?.id) return;
    if (!window.confirm(t("cancel_confirm"))) return;
    setIsCancelling(true);
    setErrorKey("");
    try {
      const res = await axios.patch<Order | ApiEnvelope<Order>>(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/orders/my/${order.id}/cancel`,
        {},
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );
      setOrder(extractPayload<Order>(res.data) || { ...order, status: "CANCELLED" });
    } catch (err) {
      console.error("Cancel order failed:", err);
      setErrorKey("cannot_cancel_order");
    } finally {
      setIsCancelling(false);
    }
  };

  const handleReorder = async () => {
    if (!token || !order?.id) return;
    setIsReordering(true);
    setErrorKey("");
    try {
      const res = await axios.post<Order | ApiEnvelope<Order>>(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/orders/my/${order.id}/reorder`,
        {},
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );
      const reordered = extractPayload<Order>(res.data);
      if (reordered?.id) {
        pushWithLang(router, `/orders/${reordered.id}`);
      } else {
        pushWithLang(router, "/orders");
      }
    } catch (err) {
      console.error("Reorder failed:", err);
      setErrorKey("cannot_reorder_order");
    } finally {
      setIsReordering(false);
    }
  };

  const handleSubmitReview = async (item: OrderItem) => {
    if (!token || !order?.id || !item.productId) return;
    const rating = reviewRatings[item.id] || 5;
    const comment = (reviewComments[item.id] || "").trim();
    setReviewLoading((prev) => ({ ...prev, [item.id]: true }));
    setReviewError((prev) => ({ ...prev, [item.id]: "" }));
    setReviewSuccess((prev) => ({ ...prev, [item.id]: "" }));
    try {
      await axios.post(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/reviews`,
        {
          orderId: order.id,
          productId: item.productId,
          rating,
          comment,
        },
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );
      setReviewSuccess((prev) => ({ ...prev, [item.id]: t("review_submitted") }));
    } catch (err: any) {
      console.error("Submit review failed:", err);
      const backendMessage =
        err?.response?.data?.message || err?.response?.data?.error || "";
      setReviewError((prev) => ({
        ...prev,
        [item.id]: backendMessage || t("review_submit_failed"),
      }));
    } finally {
      setReviewLoading((prev) => ({ ...prev, [item.id]: false }));
    }
  };

  if (!auth.isAuthReady || !token) return null;

  return (
    <div>
      <Header title={`${t("order_detail")} - TWENTY`} />
      <main id="main-content" className="app-max-width app-x-padding py-8 md:py-10">
        <AccountPageLayout section="CHI TIẾT ĐƠN HÀNG">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-3xl">{t("order_detail")}</h1>
          <Link href="/orders">
            <a className="px-4 py-2 border border-gray300 hover:bg-gray100">{t("back")}</a>
          </Link>
        </div>

        {isLoading && <p>{t("loading")}</p>}
        {errorKey && <p className="text-red">{t(errorKey)}</p>}

        {!isLoading && !errorKey && order && (
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
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-3">
                            {(() => {
                              const url =
                                item.productImage ||
                                (item.productId ? productImageMap[item.productId] : "");
                              if (!url) {
                                return (
                                  <div className="h-12 w-12 bg-gray100 border border-gray200" />
                                );
                              }
                              return (
                                <Image
                                  src={url}
                                  alt={item.productName || "product"}
                                  width={48}
                                  height={48}
                                  className="h-12 w-12 object-cover border border-gray200"
                                />
                              );
                            })()}
                            <span>{item.productName || "-"}</span>
                          </div>
                        </td>
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
                  <span className="text-gray400">{t("order_code")}:</span>{" "}
                  {getInvoiceCode(order)}
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
                  <span className="text-gray400">{t("shipping_updated_at")}:</span>{" "}
                  {order.shippingUpdatedAt
                    ? new Date(order.shippingUpdatedAt).toLocaleString(
                        router.locale === "en" ? "en-US" : "vi-VN"
                      )
                    : "-"}
                </p>
                <p>
                  <span className="text-gray400">{t("payment")}:</span>{" "}
                  {order.paymentMethod || "-"}
                </p>
                <p>
                  <span className="text-gray400">{t("voucher")}:</span>{" "}
                  {order.appliedVoucherCode || "-"}
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
              <button
                type="button"
                onClick={handleReorder}
                disabled={isReordering}
                className="mt-5 w-full border border-gray300 text-gray500 px-4 py-2 hover:bg-gray100 disabled:opacity-50"
              >
                {isReordering ? t("reordering") : t("reorder")}
              </button>
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
        {!isLoading && !errorKey && order && Array.isArray(order.statusHistory) && (
          <section className="mt-6 border border-gray200 p-5">
            <h2 className="text-xl mb-4">{t("status_timeline")}</h2>
            <div className="space-y-2 text-sm">
              {order.statusHistory.length === 0 ? (
                <p className="text-gray400">{t("no_status_timeline")}</p>
              ) : (
                order.statusHistory.map((entry, index) => (
                  <div key={`${entry.status || "status"}-${index}`} className="flex justify-between border-b border-gray100 pb-2">
                    <span>{entry.status || "-"}</span>
                    <span className="text-gray400">
                      {entry.changedAt
                        ? new Date(entry.changedAt).toLocaleString(
                            router.locale === "en" ? "en-US" : "vi-VN"
                          )
                        : "-"}
                    </span>
                  </div>
                ))
              )}
            </div>
          </section>
        )}
        {!isLoading &&
          !errorKey &&
          order &&
          String(order.status || "").toUpperCase() === "DELIVERED" && (
            <section className="mt-6 border border-gray200 p-5">
              <h2 className="text-xl mb-4">{t("review_products")}</h2>
              <div className="space-y-4">
                {(order.items || []).map((item) => (
                  <div key={`review-${item.id}`} className="border border-gray200 p-4">
                    <p className="font-medium">{item.productName || "-"}</p>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mt-3">
                      <div>
                        <label className="text-sm text-gray400">{t("rating")}</label>
                        <select
                          className="w-full border border-gray300 p-2 mt-1"
                          value={reviewRatings[item.id] || 5}
                          onChange={(e) =>
                            setReviewRatings((prev) => ({
                              ...prev,
                              [item.id]: Number(e.target.value),
                            }))
                          }
                        >
                          {[5, 4, 3, 2, 1].map((score) => (
                            <option key={score} value={score}>
                              {score} / 5
                            </option>
                          ))}
                        </select>
                      </div>
                      <div className="md:col-span-2">
                        <label className="text-sm text-gray400">{t("comment")}</label>
                        <input
                          type="text"
                          className="w-full border border-gray300 p-2 mt-1"
                          value={reviewComments[item.id] || ""}
                          onChange={(e) =>
                            setReviewComments((prev) => ({
                              ...prev,
                              [item.id]: e.target.value,
                            }))
                          }
                          placeholder={t("comment_placeholder")}
                        />
                      </div>
                    </div>
                    <div className="mt-3 flex items-center gap-3">
                      <button
                        type="button"
                        onClick={() => handleSubmitReview(item)}
                        disabled={reviewLoading[item.id] || !item.productId}
                        className="px-4 py-2 border border-gray300 hover:bg-gray100 disabled:opacity-50"
                      >
                        {reviewLoading[item.id] ? t("submitting") : t("submit_review")}
                      </button>
                      {reviewSuccess[item.id] && (
                        <span className="text-green-700 text-sm">{reviewSuccess[item.id]}</span>
                      )}
                      {reviewError[item.id] && (
                        <span className="text-red text-sm">{reviewError[item.id]}</span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}
        </AccountPageLayout>
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
