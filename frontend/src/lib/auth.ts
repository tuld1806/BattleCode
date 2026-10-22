import { createClient, SupabaseClient } from "@supabase/supabase-js";

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || "https://mock-supabase.local";
const supabaseAnonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || "mock-anon-key";

export const supabase: SupabaseClient = createClient(supabaseUrl, supabaseAnonKey);

export interface UserSession {
  userId: string;
  email: string;
  token: string;
  role: "STUDENT" | "ADMIN";
}
