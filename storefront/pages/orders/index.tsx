import { useEffect, useRef, useState } from "react";
import { GetStaticProps } from "next";
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
import { useNotify } from "../../context/NotificationContext";
import { pushWithLang } from "../../lib/router-utils";

type OrderItem = {
  id: number;
  productId?: number;
  productName?: string;
  productImage?: string;
  quantity?: number;
  price?: number;
  lineTotal?: number;
};

type Order = {
  id: number;
  shippingCode?: string;
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

type ApiEnvelope<T> = {
  data?: T;
};

type PaymentResultBanner = {
  gateway: "MOMO" | "VNPAY";
  status: "success" | "failed" | "cancelled";
  message: string;
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

const getInvoiceCode = (order: Order) =>
  order.shippingCode && order.shippingCode.trim()
    ? order.shippingCode.trim()
    : `HD${order.id}`;

const ORDER_STATUS_FILTERS = [
  "ALL",
  "WAITING_PAYMENT",
  "PENDING",
  "PROCESSING",
  "CONFIRMED",
  "SHIPPING",
  "DELIVERED",
  "CANCELLED",
] as const;

type OrderStatusFilter = (typeof ORDER_STATUS_FILTERS)[number];

const OrdersPage = () => {
  const t = useTranslations("Orders");
  const auth = useAuth();
  const router = useRouter();
  const { formatPrice } = useCurrency();
  const { notify } = useNotify();
  const token = auth.user?.token;
  const [orders, setOrders] = useState<Order[]>([]);
  const [statusFilter, setStatusFilter] = useState<OrderStatusFilter>("ALL");
  const [isLoading, setIsLoading] = useState(false);
  const [errorKey, setErrorKey] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 10;
  const [totalPages, setTotalPages] = useState(1);
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);
  const [reorderLoadingId, setReorderLoadingId] = useState<number | null>(null);
  const [paymentResult, setPaymentResult] = useState<PaymentResultBanner | null>(null);
  const [productImageMap, setProductImageMap] = useState<Record<number, string>>({});
  const orderStatusMapRef = useRef<Record<number, string>>({});
  const isFetchingOrdersRef = useRef(false);
  const notifyRef = useRef(notify);
  const tRef = useRef(t);

  useEffect(() => {
    notifyRef.current = notify;
  }, [notify]);

  useEffect(() => {
    tRef.current = t;
  }, [t]);

  useEffect(() => {
    if (!router.isReady) return;
    const query = router.query || {};
    const hasMomoResult = typeof query.resultCode === "string";
    const hasVnpayResult =
      typeof query.vnp_ResponseCode === "string" ||
      typeof query.vnp_TransactionStatus === "string";
    if (!hasMomoResult && !hasVnpayResult) return;

    let nextResult: PaymentResultBanner | null = null;
    if (hasMomoResult) {
      const momoCode = String(query.resultCode || "");
      const rawMessage = String(query.message || query.orderInfo || "");
      const normalizedMessage = decodeURIComponent(rawMessage || "").trim();
      const status: PaymentResultBanner["status"] =
        momoCode === "0"
          ? "success"
          : momoCode === "1006" || momoCode === "1003"
          ? "cancelled"
          : "failed";
      nextResult = {
        gateway: "MOMO",
        status,
        message: normalizedMessage || `MoMo resultCode=${momoCode}`,
      };
    } else if (hasVnpayResult) {
      const responseCode = String(query.vnp_ResponseCode || "");
      const txnStatus = String(query.vnp_TransactionStatus || "");
      const isSuccess = responseCode === "00" && txnStatus === "00";
      const isCancelled = responseCode === "24";
      const status: PaymentResultBanner["status"] = isSuccess
        ? "success"
        : isCancelled
        ? "cancelled"
        : "failed";
      nextResult = {
        gateway: "VNPAY",
        status,
        message: `VNPay response=${responseCode || "-"}, txnStatus=${txnStatus || "-"}`,
      };
    }

    if (nextResult) setPaymentResult(nextResult);

    const cleanPath = router.asPath.split("?")[0] || "/orders";
    router.replace(cleanPath, undefined, { shallow: true });
  }, [router]);

  useEffect(() => {
    if (!auth.isAuthReady) return;
    if (!token) {
      pushWithLang(router, "/");
      return;
    }
    setCurrentPage((prev) => (prev === 1 ? prev : 1));
  }, [auth.isAuthReady, token, router]);

  useEffect(() => {
    if (!token) return;
    let active = true;
    const loadOrders = async (isSilent?: boolean) => {
      if (isFetchingOrdersRef.current) return;
      isFetchingOrdersRef.current = true;
      if (!isSilent) {
        setIsLoading(true);
        setErrorKey("");
      }
      try {
        const res = await axios.get<PagedOrderResponse | ApiEnvelope<PagedOrderResponse>>(
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
        const payload = extractPayload<PagedOrderResponse>(res.data) || {};
        const fetchedOrders = Array.isArray(payload.content) ? payload.content : [];
        if (isSilent) {
          fetchedOrders.forEach((nextOrder) => {
            const prevStatus = orderStatusMapRef.current[nextOrder.id];
            const nextStatus = String(nextOrder.status || "").toUpperCase();
            if (prevStatus && prevStatus !== nextStatus) {
              notifyRef.current(
                tRef.current("order_status_changed", {
                  code: getInvoiceCode(nextOrder),
                  status: nextStatus || "-",
                }),
                "success"
              );
            }
          });
        }
        orderStatusMapRef.current = fetchedOrders.reduce<Record<number, string>>(
          (acc, item) => {
            acc[item.id] = String(item.status || "").toUpperCase();
            return acc;
          },
          {}
        );
        setOrders(fetchedOrders);
        setTotalPages(Math.max(1, Number(payload.totalPages || 1)));
      } catch (err) {
        console.error("Load orders failed:", err);
        if (!active) return;
        if (!isSilent) setErrorKey("cannot_load_orders");
      } finally {
        isFetchingOrdersRef.current = false;
        if (!active) return;
        if (!isSilent) setIsLoading(false);
      }
    };
    loadOrders();
    const timer = setInterval(() => {
      if (!active) return;
      loadOrders(true);
    }, 20000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [token, currentPage]);

  useEffect(() => {
    const productIds = new Set<number>();
    orders.forEach((order) => {
      (order.items || []).forEach((item) => {
        if (item.productId) productIds.add(item.productId);
      });
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
  }, [orders, productImageMap]);

  const handleCancelOrder = async (orderId: number) => {
    if (!auth.user?.token) return;
    if (!window.confirm(t("cancel_confirm"))) return;
    setActionLoadingId(orderId);
    setErrorKey("");
    try {
      await axios.patch(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/orders/my/${orderId}/cancel`,
        {},
        {
          headers: {
            Authorization: `Bearer ${auth.user.token}`,
          },
        }
      );
      setOrders((prev) =>
        prev.map((item) =>
          item.id === orderId ? { ...item, status: "CANCELLED" } : item
        )
      );
    } catch (err) {
      console.error("Cancel order failed:", err);
      setErrorKey("cannot_cancel_order");
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleReorder = async (orderId: number) => {
    if (!auth.user?.token) return;
    setReorderLoadingId(orderId);
    setErrorKey("");
    try {
      const res = await axios.post<Order | ApiEnvelope<Order>>(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/orders/my/${orderId}/reorder`,
        {},
        {
          headers: {
            Authorization: `Bearer ${auth.user.token}`,
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
      setReorderLoadingId(null);
    }
  };

  const filteredOrders = orders.filter((order) => {
    if (statusFilter === "ALL") return true;
    return String(order.status || "").toUpperCase() === statusFilter;
  });

  if (!auth.isAuthReady || !token) return null;

  return (
    <div>
      <Header title={`${t("my_orders")} - TWENTY`} />
      <main id="main-content" className="app-max-width app-x-padding py-8 md:py-10">
        <AccountPageLayout section={t("my_orders")}>
        <div className="mb-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
          <h1 className="text-2xl font-semibold">{t("my_orders")}</h1>
          <select
            id="order-status-filter"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as OrderStatusFilter)}
            className="w-full sm:w-auto border border-gray300 px-3 py-2 bg-white text-sm"
          >
            {ORDER_STATUS_FILTERS.map((status) => (
              <option key={status} value={status}>
                {status === "ALL" ? t("all_statuses") : status}
              </option>
            ))}
          </select>
        </div>
        {paymentResult && (
          <div
            className={`mb-5 border p-4 ${
              paymentResult.status === "success"
                ? "border-emerald-300 bg-emerald-50"
                : paymentResult.status === "cancelled"
                ? "border-amber-300 bg-amber-50"
                : "border-rose-300 bg-rose-50"
            }`}
          >
            <p className="font-medium">
              {paymentResult.gateway}{" "}
              {paymentResult.status === "success"
                ? t("payment_success")
                : paymentResult.status === "cancelled"
                ? t("payment_cancelled")
                : t("payment_failed")}
            </p>
            <p className="text-sm mt-1 text-gray500">{paymentResult.message}</p>
            {paymentResult.status !== "success" && orders.length > 0 && (
              <div className="mt-3">
                <button
                  type="button"
                  onClick={() => handleReorder(orders[0].id)}
                  disabled={reorderLoadingId === orders[0].id}
                  className="px-4 py-2 border border-gray300 hover:bg-gray100 disabled:opacity-50"
                >
                  {reorderLoadingId === orders[0].id
                    ? t("reordering")
                    : t("retry_payment")}
                </button>
              </div>
            )}
          </div>
        )}


        {isLoading && <p>{t("loading")}</p>}
        {errorKey && <p className="text-red">{t(errorKey)}</p>}

        {!isLoading && !errorKey && filteredOrders.length === 0 && (
          <div className="border border-gray200 p-6 text-gray500">{t("no_orders")}</div>
        )}

        {!isLoading && !errorKey && filteredOrders.length > 0 && (
          <div className="space-y-4">
            <div className="space-y-3 md:hidden">
              {filteredOrders.map((order) => {
                const firstItem = (order.items || [])[0];
                const pid = firstItem?.productId;
                const url = firstItem?.productImage || (pid ? productImageMap[pid] : "");
                return (
                  <div key={order.id} className="border border-gray200 p-3">
                    <div className="flex items-center gap-3">
                      {url ? (
                        <Image
                          src={url}
                          alt={firstItem?.productName || "product"}
                          width={56}
                          height={56}
                          className="h-14 w-14 object-cover border border-gray200 shrink-0"
                        />
                      ) : (
                        <div className="h-14 w-14 bg-gray100 border border-gray200 shrink-0" />
                      )}
                      <div className="min-w-0">
                        <p className="text-sm font-medium line-clamp-2">
                          {firstItem?.productName || "-"}
                        </p>
                        <p className="text-xs text-gray400 break-all mt-1">
                          {t("order_code")}: {getInvoiceCode(order)}
                        </p>
                      </div>
                    </div>
                    <div className="mt-3 space-y-1.5 text-sm">
                      <p>
                        {t("created_at")}:{" "}
                        {order.createdAt
                          ? new Date(order.createdAt).toLocaleString(
                              router.locale === "en" ? "en-US" : "vi-VN"
                            )
                          : "-"}
                      </p>
                      <p>{t("status")}: {order.status || "-"}</p>
                      <p>{t("payment")}: {order.paymentMethod || "-"}</p>
                      <p className="font-medium">{t("total")}: {formatPrice(order.totalPrice || 0)}</p>
                    </div>
                    <div className="mt-3 flex flex-wrap gap-x-3 gap-y-1 text-sm">
                      <Link href={`/orders/${order.id}`}>
                        <a className="text-blue-600 hover:underline">{t("view")}</a>
                      </Link>
                      <button
                        type="button"
                        onClick={() => handleReorder(order.id)}
                        disabled={reorderLoadingId === order.id}
                        className="text-gray500 hover:underline disabled:opacity-50"
                      >
                        {reorderLoadingId === order.id ? t("reordering") : t("reorder")}
                      </button>
                      {isCancelableOrder(order.status) && (
                        <button
                          type="button"
                          onClick={() => handleCancelOrder(order.id)}
                          disabled={actionLoadingId === order.id}
                          className="text-red hover:underline disabled:opacity-50"
                        >
                          {actionLoadingId === order.id ? t("cancelling") : t("cancel_order")}
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>

            <div className="hidden md:block border border-gray200">
              <table className="w-full table-fixed">
                <thead>
                  <tr className="border-b border-gray200 bg-gray100">
                    <th className="w-[24%] text-left font-medium px-3 py-3">{t("product")}</th>
                    <th className="w-[12%] text-left font-medium px-3 py-3">{t("order_code")}</th>
                    <th className="w-[16%] text-left font-medium px-3 py-3">{t("created_at")}</th>
                    <th className="w-[12%] text-left font-medium px-3 py-3">{t("status")}</th>
                    <th className="w-[12%] text-left font-medium px-3 py-3">{t("payment")}</th>
                    <th className="w-[10%] text-right font-medium px-3 py-3">{t("total")}</th>
                    <th className="w-[14%] text-right font-medium px-3 py-3">{t("action")}</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredOrders.map((order) => (
                    <tr key={order.id} className="border-b border-gray200">
                      <td className="px-3 py-3">
                        <div className="flex items-center gap-3">
                          {(() => {
                            const firstItem = (order.items || [])[0];
                            const pid = firstItem?.productId;
                            const url =
                              firstItem?.productImage ||
                              (pid ? productImageMap[pid] : "");
                            if (!url) {
                              return (
                                <div className="h-12 w-12 bg-gray100 border border-gray200" />
                              );
                            }
                            return (
                              <Image
                                src={url}
                                alt={firstItem?.productName || "product"}
                                width={48}
                                height={48}
                                className="h-12 w-12 object-cover border border-gray200"
                              />
                            );
                          })()}
                          <span className="line-clamp-2 text-sm leading-5">
                            {(order.items || [])[0]?.productName || "-"}
                          </span>
                        </div>
                      </td>
                      <td className="px-3 py-3 text-sm break-all">{getInvoiceCode(order)}</td>
                      <td className="px-3 py-3 text-sm">
                        {order.createdAt
                          ? new Date(order.createdAt).toLocaleString(
                              router.locale === "en" ? "en-US" : "vi-VN"
                            )
                          : "-"}
                      </td>
                      <td className="px-3 py-3 text-sm break-words">{order.status || "-"}</td>
                      <td className="px-3 py-3 text-sm break-words">{order.paymentMethod || "-"}</td>
                      <td className="px-3 py-3 text-right text-sm">
                        {formatPrice(order.totalPrice || 0)}
                      </td>
                      <td className="px-3 py-3 text-right">
                        <div className="inline-flex items-center justify-end flex-wrap gap-x-3 gap-y-1 text-sm">
                          <Link href={`/orders/${order.id}`}>
                            <a className="text-blue-600 hover:underline">{t("view")}</a>
                          </Link>
                          <button
                            type="button"
                            onClick={() => handleReorder(order.id)}
                            disabled={reorderLoadingId === order.id}
                            className="text-gray500 hover:underline disabled:opacity-50"
                          >
                            {reorderLoadingId === order.id
                              ? t("reordering")
                              : t("reorder")}
                          </button>
                          {isCancelableOrder(order.status) && (
                            <button
                              type="button"
                              onClick={() => handleCancelOrder(order.id)}
                              disabled={actionLoadingId === order.id}
                              className="text-red hover:underline disabled:opacity-50"
                            >
                              {actionLoadingId === order.id
                                ? t("cancelling")
                                : t("cancel_order")}
                            </button>
                          )}
                        </div>
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
        </AccountPageLayout>
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
