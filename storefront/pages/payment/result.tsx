import { GetServerSideProps } from "next";
import { useRouter } from "next/router";
import { useEffect, useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import axios from "axios";

import Header from "../../components/Header/Header";
import Footer from "../../components/Footer/Footer";
import Button from "../../components/Buttons/Button";
import { pushWithLang } from "../../lib/router-utils";

type PaymentResultState = "processing" | "success" | "failed" | "cancelled";

type VerifiedPayment = {
  status?: string;
  paid?: boolean;
  gatewaySuccessful?: boolean;
};

const PaymentResultPage = () => {
  const t = useTranslations("Orders");
  const router = useRouter();

  const redirectResult = useMemo(() => {
    const query = router.query || {};
    const hasMomoResult = typeof query.resultCode === "string";
    const hasVnpayResult =
      typeof query.vnp_ResponseCode === "string" ||
      typeof query.vnp_TransactionStatus === "string";

    if (!hasMomoResult && !hasVnpayResult) {
      return {
        gateway: "",
        state: "failed" as PaymentResultState,
        message: t("payment_result_not_found"),
      };
    }

    if (hasMomoResult) {
      const momoCode = String(query.resultCode || "");
      const rawMessage = String(query.message || query.orderInfo || "");
      const normalizedMessage = decodeURIComponent(rawMessage || "").trim();
      const state: PaymentResultState =
        momoCode === "0"
          ? "success"
          : momoCode === "1006" || momoCode === "1003"
          ? "cancelled"
          : "failed";
      return {
        gateway: "MoMo",
        state,
        message: normalizedMessage || `resultCode=${momoCode}`,
      };
    }

    const responseCode = String(query.vnp_ResponseCode || "");
    const txnStatus = String(query.vnp_TransactionStatus || "");
    const isSuccess = responseCode === "00" && txnStatus === "00";
    const isCancelled = responseCode === "24";
    const state: PaymentResultState = isSuccess
      ? "success"
      : isCancelled
      ? "cancelled"
      : "failed";
    return {
      gateway: "VNPay",
      state,
      message: `response=${responseCode || "-"}, txnStatus=${txnStatus || "-"}`,
    };
  }, [router.query, t]);

  const [result, setResult] = useState(() => ({
    gateway: "",
    state: "processing" as PaymentResultState,
    message: t("payment_processing"),
  }));

  useEffect(() => {
    if (!router.isReady) return;
    const query = router.query || {};
    const gateway =
      typeof query.resultCode === "string"
        ? "MOMO"
        : typeof query.vnp_ResponseCode === "string" ||
          typeof query.vnp_TransactionStatus === "string"
        ? "VNPAY"
        : "";

    if (!gateway) {
      setResult(redirectResult);
      return;
    }

    let active = true;
    let attempt = 0;
    const verify = async () => {
      try {
        const params = new URLSearchParams();
        params.set("gateway", gateway);
        Object.entries(query).forEach(([key, value]) => {
          if (typeof value === "string") params.set(key, value);
        });
        const response = await axios.get<VerifiedPayment>(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/payments/return/status?${params.toString()}`
        );
        if (!active) return;
        const paymentStatus = String(response.data?.status || "").toUpperCase();
        if (response.data?.paid || paymentStatus === "PAID") {
          setResult({
            gateway,
            state: "success",
            message: t("payment_verified"),
          });
          return;
        }
        if (response.data?.gatewaySuccessful === false) {
          setResult(redirectResult);
          return;
        }
        if (paymentStatus === "FAILED") {
          setResult(redirectResult);
          return;
        }
        attempt += 1;
        if (attempt < 10) {
          window.setTimeout(verify, 2000);
          return;
        }
        setResult({
          gateway,
          state: "processing",
          message: t("payment_processing"),
        });
      } catch {
        if (active) {
          setResult({
            gateway,
            state: "failed",
            message: t("payment_verification_failed"),
          });
        }
      }
    };
    verify();
    return () => {
      active = false;
    };
  }, [redirectResult, router.isReady, router.query, t]);

  const title =
    result.state === "success"
      ? t("payment_success")
      : result.state === "processing"
      ? t("payment_processing_title")
      : result.state === "cancelled"
      ? t("payment_cancelled")
      : t("payment_failed");

  return (
    <div>
      <Header title={`${title} - Clothing`} />
      <main id="main-content" className="app-max-width app-x-padding py-10">
        <div
          className={`border p-6 ${
            result.state === "success"
              ? "border-emerald-300 bg-emerald-50"
              : result.state === "processing"
              ? "border-blue-300 bg-blue-50"
              : result.state === "cancelled"
              ? "border-amber-300 bg-amber-50"
              : "border-rose-300 bg-rose-50"
          }`}
        >
          <h1 className="text-2xl mb-2">
            {result.gateway ? `${result.gateway}: ` : ""}
            {title}
          </h1>
          <p className="text-gray500 mb-6">{result.message}</p>
          <div className="flex flex-wrap gap-3">
            <Button value={t("my_orders")} onClick={() => pushWithLang(router, "/orders")} />
            {result.state !== "success" && result.state !== "processing" && (
              <Button
                value={t("retry_payment")}
                onClick={() => pushWithLang(router, "/checkout")}
              />
            )}
          </div>
        </div>
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

export default PaymentResultPage;
