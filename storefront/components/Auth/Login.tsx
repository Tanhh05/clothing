import React, { useEffect, useRef, useState } from "react";
import { Dialog } from "@headlessui/react";
import { useTranslations } from "next-intl";

import { useAuth } from "../../context/AuthContext";
import { useNotify } from "../../context/NotificationContext";
import Button from "../Buttons/Button";
import Input from "../Input/Input";

type Props = {
  onRegister: () => void;
  onForgotPassword: () => void;
  errorMsg: string;
  setErrorMsg: React.Dispatch<React.SetStateAction<string>>;
  setSuccessMsg: React.Dispatch<React.SetStateAction<string>>;
};

const Login: React.FC<Props> = ({
  onRegister,
  onForgotPassword,
  errorMsg,
  setErrorMsg,
  setSuccessMsg,
}) => {
  const auth = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const googleBtnHostRef = useRef<HTMLDivElement | null>(null);
  const googleInitializedRef = useRef(false);
  const googleScriptInjectedRef = useRef(false);
  const t = useTranslations("LoginRegister");
  const { notify } = useNotify();

  useEffect(() => {
    const scriptId = "google-identity-services";
    const setupGoogle = () => {
      const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;
      const google = (window as any).google;
      if (!clientId || !google?.accounts?.id || !googleBtnHostRef.current) return;

      if (!googleInitializedRef.current) {
        google.accounts.id.initialize({
          client_id: clientId,
          callback: async (response: { credential?: string }) => {
            try {
              if (!response?.credential) {
                setErrorMsg("google_no_credential");
                return;
              }
              const loginResponse = await auth.loginWithGoogle!(response.credential);
              if (loginResponse.success) {
                setSuccessMsg("login_successful");
                notify(t("login_successful"), "success");
              } else {
                setErrorMsg(loginResponse.message);
              }
            } catch (error) {
              console.error("Google login callback failed:", error);
              setErrorMsg("google_login_process_failed");
            }
          },
        });
        googleInitializedRef.current = true;
      }

      if (googleBtnHostRef.current.childElementCount === 0) {
        google.accounts.id.renderButton(googleBtnHostRef.current, {
          theme: "outline",
          size: "large",
          text: "signin_with",
          width: 250,
        });
      }
    };

    if ((window as any).google?.accounts?.id) {
      setupGoogle();
      return;
    }

    if (googleScriptInjectedRef.current) {
      return;
    }

    const existingScript = document.getElementById(scriptId) as HTMLScriptElement | null;
    if (existingScript) {
      googleScriptInjectedRef.current = true;
      existingScript.addEventListener("load", setupGoogle, { once: true });
      return;
    }

    const script = document.createElement("script");
    script.id = scriptId;
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    script.defer = true;
    script.onload = setupGoogle;
    document.body.appendChild(script);
    googleScriptInjectedRef.current = true;
  }, [auth, notify, setErrorMsg, setSuccessMsg, t]);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const loginResponse = await auth.login!(email, password);
    if (loginResponse.success) {
      setSuccessMsg("login_successful");
      notify(t("login_successful"), "success");
    } else {
      setErrorMsg("incorrect_email_password");
    }
  };

  const handleGoogleLogin = async () => {
    const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;
    const host = googleBtnHostRef.current;
    const google = (window as any).google;
    if (!clientId || !host || !google?.accounts?.id) {
      setErrorMsg("google_sdk_not_ready");
      return;
    }
    const realBtn = host.querySelector("div[role='button']") as
      | HTMLDivElement
      | null;
    if (realBtn) {
      realBtn.click();
      return;
    }
    google.accounts.id.prompt((notification: any) => {
      if (
        notification?.isNotDisplayed?.() ||
        notification?.isSkippedMoment?.()
      ) {
        const reason =
          notification?.getNotDisplayedReason?.() ||
          notification?.getSkippedReason?.() ||
          "unknown";
        setErrorMsg(`${t("google_onetap_not_displayed")}: ${reason}`);
      }
    });
  };

  return (
    <>
      <Dialog.Title
        as="h3"
        className="text-4xl text-center my-8 font-medium leading-6 text-gray-900"
      >
        {t("login")}
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
        />
        <Input
          type="password"
          placeholder={`${t("password")} *`}
          name="password"
          required
          extraClass="w-full focus:border-gray500 mb-4"
          border="border-2 border-gray300"
          onChange={(e) => setPassword((e.target as HTMLInputElement).value)}
          value={password}
        />
        {errorMsg !== "" && (
          <div className="text-red text-sm mb-4 whitespace-nowrap">
            {errorMsg.includes(" ") ? errorMsg : t(errorMsg)}
          </div>
        )}
        <div className="flex justify-between mb-4">
          <div className="flex items-center text-gray400 focus:outline-none">
            <input
              type="checkbox"
              id="remember"
              name="remember"
              className="w-4 h-4 mb-0 mr-2"
            />
            <label htmlFor="remember" className="text-sm">
              {t("remember_me")}
            </label>
          </div>
          <span
            onClick={onForgotPassword}
            className="text-gray400 text-sm hover:text-gray500 focus:outline-none focus:text-gray500"
          >
            {t("forgot_password")}
          </span>
        </div>
        <Button
          type="submit"
          value={t("login")}
          extraClass="w-full text-center text-xl mb-4"
          size="lg"
        />
        <Button
          type="button"
          value={t("login_with_google")}
          extraClass="w-full text-center mb-4"
          onClick={handleGoogleLogin}
        />
        <div
          ref={googleBtnHostRef}
          className="absolute opacity-0 pointer-events-none -left-[9999px] -top-[9999px]"
          aria-hidden="true"
        />
        <div className="text-center text-gray400">
          {t("not_member")}{" "}
          <span
            onClick={onRegister}
            className="text-gray500 focus:outline-none focus:underline cursor-pointer"
          >
            {t("register")}
          </span>
        </div>
      </form>
    </>
  );
};

export default Login;
