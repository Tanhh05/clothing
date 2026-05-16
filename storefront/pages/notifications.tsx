import { useEffect, useState } from "react";
import { GetServerSideProps } from "next";
import Link from "next/link";
import axios from "axios";

import Header from "../components/Header/Header";
import Footer from "../components/Footer/Footer";
import AccountPageLayout from "../components/Account/AccountPageLayout";
import { useAuth } from "../context/AuthContext";
import { pushWithLang } from "../lib/router-utils";
import { useRouter } from "next/router";

type NotificationItem = {
  id: number;
  title?: string;
  content?: string;
  type?: string;
  isRead?: boolean;
  createdAt?: string;
};

const NotificationsPage = () => {
  const auth = useAuth();
  const router = useRouter();
  const token = auth.user?.token;
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(false);

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
        const res = await axios.get<NotificationItem[]>(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/user/notifications`,
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        );
        if (!active) return;
        setItems(Array.isArray(res.data) ? res.data : []);
      } catch (err) {
        console.error("Load notifications failed:", err);
      } finally {
        if (!active) return;
        setLoading(false);
      }
    };
    load();
    return () => {
      active = false;
    };
  }, [auth.isAuthReady, token, router]);

  const markRead = async (id: number) => {
    if (!token) return;
    try {
      await axios.patch(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/user/notifications/${id}/read`,
        {},
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );
      setItems((prev) =>
        prev.map((item) => (item.id === id ? { ...item, isRead: true } : item))
      );
    } catch (err) {
      console.error("Mark notification read failed:", err);
    }
  };

  const markAllRead = async () => {
    if (!token) return;
    try {
      await axios.patch(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/user/notifications/read-all`,
        {},
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );
      setItems((prev) => prev.map((item) => ({ ...item, isRead: true })));
    } catch (err) {
      console.error("Mark all notifications read failed:", err);
    }
  };

  if (!auth.isAuthReady || !token) return null;

  return (
    <div>
      <Header title="Thông báo - Haru" />
      <main id="main-content" className="app-max-width app-x-padding py-8 md:py-10">
        <AccountPageLayout section="THÔNG BÁO">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-3xl">Thông báo</h1>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={markAllRead}
              className="px-4 py-2 border border-gray300 hover:bg-gray100"
            >
              Đánh dấu tất cả đã đọc
            </button>
            <Link href="/profile">
              <a className="px-4 py-2 border border-gray300 hover:bg-gray100">Tài khoản</a>
            </Link>
          </div>
        </div>

        {loading && <p>Đang tải...</p>}

        {!loading && items.length === 0 && (
          <p className="text-gray400">Chưa có thông báo.</p>
        )}

        <div className="space-y-3">
          {items.map((item) => (
            <article
              key={item.id}
              className={`border p-4 ${item.isRead ? "border-gray200" : "border-gray400"}`}
            >
              <div className="flex items-start justify-between gap-4">
                <div>
                  <h2 className="text-lg">{item.title || "Thông báo"}</h2>
                  <p className="text-sm text-gray400">{item.type || "-"}</p>
                </div>
                {!item.isRead && (
                  <button
                    type="button"
                    onClick={() => markRead(item.id)}
                    className="px-3 py-1 border border-gray300 hover:bg-gray100"
                  >
                    Đã đọc
                  </button>
                )}
              </div>
              <p className="mt-3 whitespace-pre-wrap">{item.content || ""}</p>
              <p className="mt-3 text-sm text-gray400">
                {item.createdAt ? new Date(item.createdAt).toLocaleString("vi-VN") : "-"}
              </p>
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

export default NotificationsPage;
