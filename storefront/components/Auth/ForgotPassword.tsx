import React, { useEffect, useRef, useState } from "react";
import { Dialog } from "@headlessui/react";
import { useTranslations } from "next-intl";

import { useAuth } from "../../context/AuthContext";
import { useNotify } from "../../context/NotificationContext";
import Button from "../Buttons/Button";
import Input from "../Input/Input";

type Props = {
  onLogin: () => void;
  errorMsg: string;
  setErrorMsg: React.Dispatch<React.SetStateAction<string>>;
  setSuccessMsg: React.Dispatch<React.SetStateAction<string>>;
};

type Step = "request" | "verify" | "reset";

const ForgotPassword: React.FC<Props> = ({
  onLogin,
  errorMsg,
  setErrorMsg,
  setSuccessMsg,
}) => {
  const auth = useAuth();
  const { notify } = useNotify();
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [step, setStep] = useState<Step>("request");
  const [resetToken, setResetToken] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const t = useTranslations("LoginRegister");
  const mountedRef = useRef(true);

  useEffect(() => {
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setIsSubmitting(true);
    setErrorMsg("");
    if (step === "request") {
      const response = await auth.forgotPassword!(email);
      if (!mountedRef.current) return;
      if (response.success) {
        notify(t("reset_email_sent"), "success");
        setStep("verify");
      } else {
        setErrorMsg(response.message || "error_occurs");
      }
      setIsSubmitting(false);
      return;
    }

    if (step === "verify") {
      const response = await auth.verifyForgotPasswordOtp!(email, otp);
      if (!mountedRef.current) return;
      if (response.success && response.resetToken) {
        setResetToken(response.resetToken);
        setStep("reset");
        notify(t("otp_verified"), "success");
      } else {
        setErrorMsg(response.message || "error_occurs");
      }
      setIsSubmitting(false);
      return;
    }

    if (newPassword.trim() !== confirmPassword.trim()) {
      setErrorMsg("password_confirm_not_match");
      setIsSubmitting(false);
      return;
    }
    const response = await auth.resetPasswordWithOtp!(email, resetToken, newPassword);
    if (!mountedRef.current) return;
    if (response.success) {
      setSuccessMsg("password_reset_success");
      notify(t("password_reset_success"), "success");
      setIsSubmitting(false);
      onLogin();
      return;
    }
    setErrorMsg(response.message || "error_occurs");
    setIsSubmitting(false);
  };

  return (
    <>
      <Dialog.Title
        as="h3"
        className="text-3xl text-center my-8 font-medium leading-10 text-gray-900"
      >
        {step === "request" && t("forgot_password")}
        {step === "verify" && t("verify_otp")}
        {step === "reset" && t("reset_password")}
      </Dialog.Title>
      <form onSubmit={handleSubmit} className="mt-2">
        <Input
          type="email"
          placeholder={`${t("email_address")} *`}
          name="email"
          required
          extraClass="w-full focus:border-gray500"
          border="border-2 border-gray300 mb-4"
          onChange={(e) => setEmail((e.target as HTMLInputElement).value)}
          value={email}
          disabled={step !== "request"}
        />
        {step === "verify" && (
          <Input
            type="text"
            placeholder={`${t("otp_code")} *`}
            name="otp"
            required
            extraClass="w-full focus:border-gray500"
            border="border-2 border-gray300 mb-4"
            onChange={(e) => setOtp((e.target as HTMLInputElement).value)}
            value={otp}
          />
        )}
        {step === "reset" && (
          <>
            <Input
              type="password"
              placeholder={`${t("new_password")} *`}
              name="newPassword"
              required
              extraClass="w-full focus:border-gray500"
              border="border-2 border-gray300 mb-4"
              onChange={(e) => setNewPassword((e.target as HTMLInputElement).value)}
              value={newPassword}
            />
            <Input
              type="password"
              placeholder={`${t("confirm_password")} *`}
              name="confirmPassword"
              required
              extraClass="w-full focus:border-gray500"
              border="border-2 border-gray300 mb-4"
              onChange={(e) => setConfirmPassword((e.target as HTMLInputElement).value)}
              value={confirmPassword}
            />
          </>
        )}
        {errorMsg !== "" && (
          <div className="text-red text-sm mb-4 whitespace-nowrap">
            {errorMsg.includes(" ") ? errorMsg : t(errorMsg)}
          </div>
        )}
        <Button
          type="submit"
          value={isSubmitting ? "..." : step === "reset" ? t("reset_password") : t("submit")}
          extraClass="w-full text-center text-xl mb-4"
          size="lg"
        />
        <div className="text-center text-gray400">
          {t("go_back_to")}{" "}
          <span
            onClick={onLogin}
            className="text-gray500 focus:outline-none focus:underline cursor-pointer"
          >
            {t("login")}
          </span>
        </div>
      </form>
    </>
  );
};

export default ForgotPassword;
