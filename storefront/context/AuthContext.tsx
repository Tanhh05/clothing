import axios from "axios";
import { getCookie, removeCookies, setCookies } from "cookies-next";
import React, { useState, useEffect, useContext, createContext, useRef } from "react";
import { useNotify } from "./NotificationContext";

type authType = {
  user: null | User;
  isAuthReady: boolean;
  register?: (
    email: string,
    fullname: string,
    password: string,
    shippingAddress: string,
    phone: string
  ) => Promise<{
    success: boolean;
    message: string;
  }>;
  login?: (
    email: string,
    password: string
  ) => Promise<{
    success: boolean;
    message: string;
  }>;
  loginWithGoogle?: (idToken: string) => Promise<{
    success: boolean;
    message: string;
  }>;
  forgotPassword?: (email: string) => Promise<{
    success: boolean;
    message: string;
  }>;
  verifyForgotPasswordOtp?: (email: string, otp: string) => Promise<{
    success: boolean;
    message: string;
    resetToken?: string;
  }>;
  resetPasswordWithOtp?: (
    email: string,
    resetToken: string,
    newPassword: string
  ) => Promise<{
    success: boolean;
    message: string;
  }>;
  updateProfile?: (payload: {
    username?: string;
    email?: string;
    fullName?: string;
    phone?: string;
  }) => Promise<{
    success: boolean;
    message: string;
  }>;
  logout?: () => Promise<void>;
};

const initialAuth: authType = {
  user: null,
  isAuthReady: false,
};

const authContext = createContext<authType>(initialAuth);

type User = {
  id: number;
  username?: string;
  email: string;
  fullname: string;
  shippingAddress?: string;
  phone?: string;
  token: string;
  refreshToken?: string;
  tokenType?: string;
};

// Provider component that wraps your app and makes auth object ...
// ... available to any child component that calls useAuth().
export function ProvideAuth({ children }: { children: React.ReactNode }) {
  const auth = useProvideAuth();
  return <authContext.Provider value={auth}>{children}</authContext.Provider>;
}
// Hook for child components to get the auth object ...
// ... and re-render when it changes.
export const useAuth = () => {
  return useContext(authContext);
};

// Provider hook that creates auth object and handles state
function useProvideAuth() {
  const { notify } = useNotify();
  const [user, setUser] = useState<User | null>(null);
  const [isAuthReady, setIsAuthReady] = useState(false);
  const isRefreshingRef = useRef(false);
  const refreshPromiseRef = useRef<Promise<string | null> | null>(null);

  const getLocale = () => {
    if (typeof window === "undefined") return "vi";
    const pathname = window.location.pathname || "";
    if (pathname.startsWith("/en")) return "en";
    if (pathname.startsWith("/my")) return "my";
    return "vi";
  };

  const tr = (key: "login_required" | "google_user") => {
    const locale = getLocale();
    const dict = {
      vi: {
        login_required: "Bạn cần đăng nhập để sử dụng tính năng này",
        google_user: "Người dùng Google",
      },
      en: {
        login_required: "You need to log in to use this feature",
        google_user: "Google User",
      },
      my: {
        login_required: "ဤလုပ်ဆောင်ချက်ကို အသုံးပြုရန် လော့ဂ်အင်ဝင်ရန် လိုအပ်သည်။",
        google_user: "Google အသုံးပြုသူ",
      },
    } as const;
    return dict[locale as "vi" | "en" | "my"]?.[key] || dict.vi[key];
  };

  useEffect(() => {
    const initialAuth = getCookie("user");
    if (initialAuth) {
      const initUser = JSON.parse(initialAuth as string);
      setUser(initUser);
    }
    setIsAuthReady(true);
  }, []);

  useEffect(() => {
    setCookies("user", user);
  }, [user]);

  useEffect(() => {
    let active = true;
    const loadUserProfile = async () => {
      if (!user?.token) return;
      try {
        const response = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/user/me`,
          {
            headers: {
              Authorization: `Bearer ${user.token}`,
            },
          }
        );
        if (!active) return;
        const me = response.data || {};
        setUser((prev) => {
          if (!prev) return prev;
          return {
            ...prev,
            username: me.username ?? prev.username,
            email: me.email ?? prev.email,
            fullname: me.fullName ?? prev.fullname,
            phone: me.phone ?? prev.phone ?? "",
          };
        });
      } catch (error) {
        console.error("Load profile failed:", error);
      }
    };
    loadUserProfile();
    return () => {
      active = false;
    };
  }, [user?.token]);

  useEffect(() => {
    const interceptorId = axios.interceptors.response.use(
      (response) => response,
      async (error) => {
        const status = error?.response?.status;
        const url = error?.config?.url || "";
        const isAuthEndpoint =
          url.includes("/api/auth/login") ||
          url.includes("/api/auth/google") ||
          url.includes("/api/auth/refresh") ||
          url.includes("/api/auth/logout");

        const originalRequest: any = error?.config;
        if (
          status === 401 &&
          !isAuthEndpoint &&
          originalRequest &&
          !originalRequest._retry
        ) {
          const cachedUserRaw = getCookie("user");
          const cachedUser = cachedUserRaw
            ? (JSON.parse(cachedUserRaw as string) as User)
            : null;
          const refreshToken = cachedUser?.refreshToken;
          if (refreshToken) {
            originalRequest._retry = true;
            try {
              if (!isRefreshingRef.current) {
                isRefreshingRef.current = true;
                refreshPromiseRef.current = axios
                  .post(`${process.env.NEXT_PUBLIC_BACKEND_URL}/api/auth/refresh`, {
                    refreshToken,
                  })
                  .then((res) => {
                    const refreshed = res.data || {};
                    const nextUser: User = {
                      id: +(refreshed.userId || cachedUser?.id || 0),
                      username: refreshed.username || cachedUser?.username,
                      email: refreshed.email || cachedUser?.email || "",
                      fullname:
                        refreshed.username ||
                        cachedUser?.fullname ||
                        cachedUser?.email ||
                        "",
                      phone: cachedUser?.phone,
                      token: refreshed.accessToken || "",
                      refreshToken: refreshed.refreshToken || refreshToken,
                      tokenType: refreshed.tokenType || cachedUser?.tokenType,
                    };
                    setUser(nextUser);
                    return nextUser.token || null;
                  })
                  .catch(() => {
                    setUser(null);
                    removeCookies("user");
                    return null;
                  })
                  .finally(() => {
                    isRefreshingRef.current = false;
                    refreshPromiseRef.current = null;
                  });
              }

              const nextToken = await refreshPromiseRef.current;
              if (nextToken) {
                originalRequest.headers = originalRequest.headers || {};
                originalRequest.headers.Authorization = `Bearer ${nextToken}`;
                return axios(originalRequest);
              }
            } catch (refreshErr) {
              return Promise.reject(refreshErr);
            }
          }
          notify(tr("login_required"), "error");
        }
        return Promise.reject(error);
      }
    );

    return () => {
      axios.interceptors.response.eject(interceptorId);
    };
  }, [notify]);

  const register = async (
    email: string,
    fullname: string,
    password: string,
    shippingAddress: string,
    phone: string
  ) => {
    try {
      const normalizedFullName = fullname.trim();
      const usernameFromEmail = email.split("@")[0]?.trim();
      const username =
        usernameFromEmail && usernameFromEmail.length > 0
          ? usernameFromEmail
          : normalizedFullName.replace(/\s+/g, "").toLowerCase();

      const response = await axios.post(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/auth/register`,
        {
          email,
          fullName: normalizedFullName,
          username,
          password,
          shippingAddress,
          phone,
        }
      );
      const registerResponse = response.data?.data || response.data;
      const user: User = {
        id: +(registerResponse.userId ?? registerResponse.id),
        username: registerResponse.username || username,
        email: registerResponse.email || email,
        fullname: registerResponse.fullName || normalizedFullName,
        shippingAddress,
        phone: registerResponse.phone || phone,
        token: registerResponse.accessToken || registerResponse.token,
        refreshToken: registerResponse.refreshToken,
        tokenType: registerResponse.tokenType,
      };
      setUser(user);
      return {
        success: true,
        message: "register_successful",
      };
    } catch (err) {
      const errResponse = (err as any)?.response?.data || {};
      const errorMessage =
        errResponse?.error?.type ||
        errResponse?.error?.detail?.message ||
        errResponse?.message ||
        "error_occurs";
      return {
        success: false,
        message: errorMessage,
      };
    }
  };

  const login = async (email: string, password: string) => {
    try {
      const response = await axios.post(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/auth/login`,
        {
          usernameOrEmail: email,
          password,
        }
      );
      const loginResponse = response.data;
      const user: User = {
        id: +loginResponse.userId,
        username: loginResponse.username,
        email,
        fullname: loginResponse.username || email,
        token: loginResponse.accessToken,
        refreshToken: loginResponse.refreshToken,
        tokenType: loginResponse.tokenType,
      };
      setUser(user);
      return {
        success: true,
        message: "login_successful",
      };
    } catch (err) {
      console.error("Login failed:", err);
      return {
        success: false,
        message: "incorrect_email_password",
      };
    }
  };

  const loginWithGoogle = async (idToken: string) => {
    try {
      const response = await axios.post(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/auth/google`,
        { idToken }
      );
      const loginResponse = response.data;
      const loginEmail = loginResponse.email || "";
      const user: User = {
        id: +loginResponse.userId,
        username: loginResponse.username,
        email: loginEmail,
        fullname: loginResponse.username || tr("google_user"),
        token: loginResponse.accessToken,
        refreshToken: loginResponse.refreshToken,
        tokenType: loginResponse.tokenType,
      };
      setUser(user);
      return {
        success: true,
        message: "login_successful",
      };
    } catch (err) {
      console.error("Google login failed:", err);
      const axiosErr = err as any;
      const backendMsg =
        axiosErr?.response?.data?.message ||
        axiosErr?.response?.data?.error ||
        "";
      return {
        success: false,
        message: backendMsg || "error_occurs",
      };
    }
  };

  const forgotPassword = async (email: string) => {
    try {
      await axios.post(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/auth/forgot-password`,
        {
          email,
        }
      );
      return {
        success: true,
        message: "reset_email_sent",
      };
    } catch (err) {
      console.error("Forgot password failed:", err);
      const axiosErr = err as any;
      const backendMsg =
        axiosErr?.response?.data?.message ||
        axiosErr?.response?.data?.error ||
        "";
      return {
        success: false,
        message: backendMsg || "error_occurs",
      };
    }
  };

  const verifyForgotPasswordOtp = async (email: string, otp: string) => {
    try {
      const response = await axios.post(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/auth/forgot-password/verify-otp`,
        {
          email,
          otp,
        }
      );
      return {
        success: true,
        message: "otp_verified",
        resetToken: response.data?.resetToken || "",
      };
    } catch (err) {
      console.error("Verify OTP failed:", err);
      const axiosErr = err as any;
      const backendMsg =
        axiosErr?.response?.data?.message ||
        axiosErr?.response?.data?.error ||
        "";
      return {
        success: false,
        message: backendMsg || "error_occurs",
      };
    }
  };

  const resetPasswordWithOtp = async (
    email: string,
    resetToken: string,
    newPassword: string
  ) => {
    try {
      await axios.post(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/auth/forgot-password/reset`,
        {
          email,
          resetToken,
          newPassword,
        }
      );
      return {
        success: true,
        message: "password_reset_success",
      };
    } catch (err) {
      console.error("Reset password failed:", err);
      const axiosErr = err as any;
      const backendMsg =
        axiosErr?.response?.data?.message ||
        axiosErr?.response?.data?.error ||
        "";
      return {
        success: false,
        message: backendMsg || "error_occurs",
      };
    }
  };

  const updateProfile = async (payload: {
    username?: string;
    email?: string;
    fullName?: string;
    phone?: string;
  }) => {
    if (!user?.token) {
      return { success: false, message: "error_occurs" };
    }
    try {
      const response = await axios.put(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/user/me`,
        payload,
        {
          headers: {
            Authorization: `Bearer ${user.token}`,
          },
        }
      );
      const nextUser = response.data;
      setUser({
        ...user,
        username: nextUser.username,
        email: nextUser.email,
        fullname: nextUser.fullName || user.fullname,
        phone: nextUser.phone || "",
      });
      return { success: true, message: "profile_updated" };
    } catch (err) {
      console.error("Update profile failed:", err);
      return { success: false, message: "error_occurs" };
    }
  };

  const logout = async () => {
    const refreshToken = user?.refreshToken;
    if (refreshToken) {
      try {
        await axios.post(`${process.env.NEXT_PUBLIC_BACKEND_URL}/api/auth/logout`, {
          refreshToken,
        });
      } catch (err) {
        console.error("Logout API failed:", err);
      }
    }
    setUser(null);
    removeCookies("user");
  };

  // Return the user object and auth methods
  return {
    user,
    isAuthReady,
    register,
    login,
    loginWithGoogle,
    forgotPassword,
    verifyForgotPasswordOtp,
    resetPasswordWithOtp,
    updateProfile,
    logout,
  };
}
