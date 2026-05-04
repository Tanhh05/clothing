import { createContext, useCallback, useContext, useMemo, useState } from "react";
import { Transition } from "@headlessui/react";

type NotificationType = "success" | "error";

type NotificationState = {
  open: boolean;
  message: string;
  type: NotificationType;
};

type NotificationContextType = {
  notify: (message: string, type?: NotificationType) => void;
};

const NotificationContext = createContext<NotificationContextType>({
  notify: () => {},
});

export const useNotify = () => useContext(NotificationContext);

export function NotificationProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<NotificationState>({
    open: false,
    message: "",
    type: "success",
  });

  const notify = useCallback((message: string, type: NotificationType = "success") => {
    setState({ open: true, message, type });
    window.setTimeout(() => {
      setState((prev) => ({ ...prev, open: false }));
    }, 2600);
  }, []);

  const value = useMemo(() => ({ notify }), [notify]);

  return (
    <NotificationContext.Provider value={value}>
      {children}
      <div className="fixed bottom-6 right-6 z-[2147483647] pointer-events-none w-[calc(100%-2rem)] max-w-md">
        <Transition
          show={state.open}
          enter="transform transition duration-200"
          enterFrom="opacity-0 translate-y-2"
          enterTo="opacity-100 translate-y-0"
          leave="transform transition duration-150"
          leaveFrom="opacity-100 translate-y-0"
          leaveTo="opacity-0 translate-y-2"
        >
          <div
            className={`w-full rounded-xl border-l-4 px-4 py-3 text-sm sm:text-base shadow-2xl backdrop-blur ${
              state.type === "success"
                ? "bg-emerald-900/95 border-emerald-300 text-emerald-50"
                : "bg-rose-900/95 border-rose-300 text-rose-50"
            }`}
          >
            <div className="flex items-center gap-3">
              <span className="text-base sm:text-lg leading-none">
                {state.type === "success" ? "✓" : "!"}
              </span>
              <span className="font-medium tracking-wide">{state.message}</span>
            </div>
          </div>
        </Transition>
      </div>
    </NotificationContext.Provider>
  );
}
