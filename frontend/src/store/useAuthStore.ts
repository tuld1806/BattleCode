import { create } from "zustand";
import { UserSession } from "@/lib/auth";

interface AuthState {
  session: UserSession | null;
  setSession: (session: UserSession | null) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  session: null,
  setSession: (session) => set({ session }),
  logout: () => set({ session: null }),
}));
