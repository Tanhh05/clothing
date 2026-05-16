import { useEffect, useMemo, useState } from "react";
import { GetServerSideProps } from "next";
import Link from "next/link";
import axios from "axios";
import { useRouter } from "next/router";

import Header from "../components/Header/Header";
import Footer from "../components/Footer/Footer";
import AccountPageLayout from "../components/Account/AccountPageLayout";
import { useAuth } from "../context/AuthContext";
import { pushWithLang } from "../lib/router-utils";

type ReturnRequestItem = {
  id?: number;
  orderItemId?: number;
  sku?: string;
  productName?: string;
  requestedQuantity?: number;
};

type ReturnRequest = {
  id: number;
  orderId?: number;
  status?: string;
  reasonCode?: string;
  reasonDetail?: string;
  requestedAt?: string;
  items?: ReturnRequestItem[];
};

const ReturnsPage = () => {
  const auth = useAuth();
  const router = useRouter();
  const token = auth.user?.token;
  const [items, setItems] = useState<ReturnRequest[]>([]);
  const [loading, setLoading] = useState(false);
  const status = useMemo(() => {
    const value = router.query.status;
    if (!value || Array.isArray(value)) return "";
    return value;
  }, [router.query.status]);

  useEffect(() => {
    if (!auth.isAuthReady) return;
    if (!token) {
      pushWithLang(router, "/");
      return;
    }

    let active = true;
    const load = async () => {
      setLoading(true);
      try {
        const res = await axios.get<ReturnRequest[]>(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/returns/my`,
          {
            params: status ? { status } : undefined,
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        );
        if (!active) return;
        setItems(Array.isArray(res.data) ? res.data : []);
      } catch (err) {
        console.error("Load return requests failed:", err);
      } finally {
        if (!active) return;
        setLoading(false);
      }
    };
    load();
    return () => {
      active = false;
    };
  }, [auth.isAuthReady, token, router, status]);

  if (!auth.isAuthReady || !token) return null;

  return (
    <div>
      <Header title="Đổi trả - Haru" />
      <main id="main-content" className="app-max-width app-x-padding py-8 md:py-10">
        <AccountPageLayout section="ĐỔI TRẢ">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-3xl">Yêu cầu đổi trả</h1>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => pushWithLang(router, "/returns")}
              className="px-4 py-2 border border-gray300 hover:bg-gray100"
            >
              Tất cả
            </button>
            <button
              type="button"
              onClick={() => pushWithLang(router, "/returns?status=PENDING")}
              className="px-4 py-2 border border-gray300 hover:bg-gray100"
            >
              Chờ xử lý
            </button>
            <button
              type="button"
              onClick={() => pushWithLang(router, "/returns?status=APPROVED")}
              className="px-4 py-2 border border-gray300 hover:bg-gray100"
            >
              Đã duyệt
            </button>
            <button
              type="button"
              onClick={() => pushWithLang(router, "/returns?status=REJECTED")}
              className="px-4 py-2 border border-gray300 hover:bg-gray100"
            >
              Từ chối
            </button>
            <Link href="/orders">
              <a className="px-4 py-2 border border-gray300 hover:bg-gray100">Đơn hàng</a>
            </Link>
          </div>
        </div>

        {loading && <p>Đang tải...</p>}
        {!loading && items.length === 0 && <p className="text-gray400">Chưa có yêu cầu đổi trả.</p>}

        <div className="space-y-4">
          {items.map((request) => (
            <article key={request.id} className="border border-gray200 p-4">
              <div className="flex items-center justify-between gap-4">
                <h2 className="text-lg">#{request.id} - Đơn #{request.orderId || "-"}</h2>
                <span className="text-sm border border-gray300 px-2 py-1">
                  {request.status || "-"}
                </span>
              </div>
              <p className="mt-2 text-sm text-gray400">
                Lý do: {request.reasonCode || "-"}
              </p>
              <p className="mt-1">{request.reasonDetail || "-"}</p>
              <p className="mt-2 text-sm text-gray400">
                Tạo lúc:{" "}
                {request.requestedAt
                  ? new Date(request.requestedAt).toLocaleString("vi-VN")
                  : "-"}
              </p>
              <div className="mt-3 overflow-auto">
                <table className="w-full min-w-[520px]">
                  <thead>
                    <tr className="border-b border-gray200 bg-gray100">
                      <th className="text-left px-3 py-2 font-medium">SKU</th>
                      <th className="text-left px-3 py-2 font-medium">Sản phẩm</th>
                      <th className="text-right px-3 py-2 font-medium">SL</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(request.items || []).map((item) => (
                      <tr key={item.id || `${request.id}-${item.orderItemId}`} className="border-b border-gray200">
                        <td className="px-3 py-2">{item.sku || "-"}</td>
                        <td className="px-3 py-2">{item.productName || "-"}</td>
                        <td className="px-3 py-2 text-right">{item.requestedQuantity || 0}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </article>
          ))}
        </div>
        </AccountPageLayout>
      </main>
      <Footer />
    </div>
  );
};

export const getServerSideProps: GetServerSideProps = async ({ locale }) => {
  return {
    props: {
      messages: (await import(`../messages/common/${locale}.json`)).default,
    },
  };
};

export default ReturnsPage;
