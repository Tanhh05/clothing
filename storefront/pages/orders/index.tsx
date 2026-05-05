import { useEffect, useState } from "react";
import { GetStaticProps } from "next";
import Link from "next/link";
import { useRouter } from "next/router";
import axios from "axios";

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
        setError("Không thể tải danh sách đơn hàng.");
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
      <Header title="Đơn hàng của tôi - Twenty" />
      <main id="main-content" className="app-max-width app-x-padding py-8 md:py-10">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-3xl">Đơn hàng của tôi</h1>
          <Link href="/profile">
            <a className="px-4 py-2 border border-gray300 hover:bg-gray100">Hồ sơ</a>
          </Link>
        </div>

        {isLoading && <p>Đang tải...</p>}
        {error && <p className="text-red">{error}</p>}

        {!isLoading && !error && orders.length === 0 && (
          <div className="border border-gray200 p-6 text-gray500">Chưa có đơn hàng nào.</div>
        )}

        {!isLoading && !error && orders.length > 0 && (
          <div className="space-y-4">
            <div className="overflow-auto border border-gray200">
            <table className="w-full min-w-[760px]">
              <thead>
                <tr className="border-b border-gray200 bg-gray100">
                  <th className="text-left font-medium px-4 py-3">Mã đơn</th>
                  <th className="text-left font-medium px-4 py-3">Ngày tạo</th>
                  <th className="text-left font-medium px-4 py-3">Trạng thái</th>
                  <th className="text-left font-medium px-4 py-3">Thanh toán</th>
                  <th className="text-right font-medium px-4 py-3">Tổng tiền</th>
                  <th className="text-right font-medium px-4 py-3">Chi tiết</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.id} className="border-b border-gray200">
                    <td className="px-4 py-3">#{order.id}</td>
                    <td className="px-4 py-3">
                      {order.createdAt
                        ? new Date(order.createdAt).toLocaleString("vi-VN")
                        : "-"}
                    </td>
                    <td className="px-4 py-3">{order.status || "-"}</td>
                    <td className="px-4 py-3">{order.paymentMethod || "-"}</td>
                    <td className="px-4 py-3 text-right">
                      {formatPrice(order.totalPrice || 0)}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <Link href={`/orders/${order.id}`}>
                        <a className="text-blue-600 hover:underline">Xem</a>
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-gray400">
                Trang {currentPage}/{totalPages}
              </span>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setCurrentPage((prev) => Math.max(1, prev - 1))}
                  disabled={currentPage <= 1}
                  className="px-3 py-1 border border-gray300 disabled:opacity-50"
                >
                  Trước
                </button>
                <button
                  type="button"
                  onClick={() =>
                    setCurrentPage((prev) => Math.min(totalPages, prev + 1))
                  }
                  disabled={currentPage >= totalPages}
                  className="px-3 py-1 border border-gray300 disabled:opacity-50"
                >
                  Sau
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
