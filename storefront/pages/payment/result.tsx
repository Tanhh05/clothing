import { GetServerSideProps } from "next";
import { useRouter } from "next/router";
import { useMemo } from "react";
import { useTranslations } from "next-intl";

import Header from "../../components/Header/Header";
import Footer from "../../components/Footer/Footer";
import Button from "../../components/Buttons/Button";
import { pushWithLang } from "../../lib/router-utils";

type PaymentResultState = "success" | "failed" | "cancelled";

const PaymentResultPage = () => {
  const t = useTranslations("Orders");
  const router = useRouter();

  const result = useMemo(() => {
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

  const title =
    result.state === "success"
      ? t("payment_success")
      : result.state === "cancelled"
      ? t("payment_cancelled")
      : t("payment_failed");

  return (
    <div>
      <Header title={`${title} - TWENTY`} />
      <main id="main-content" className="app-max-width app-x-padding py-10">
        <div
          className={`border p-6 ${
            result.state === "success"
              ? "border-emerald-300 bg-emerald-50"
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
            {result.state !== "success" && (
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

