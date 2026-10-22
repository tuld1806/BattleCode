import type { Metadata } from "next";
import "./globals.css";
import { Navbar } from "@/components/layout/Navbar";
import { LiveTelemetryTicker } from "@/components/ui/kinetic-primitives";

export const metadata: Metadata = {
  title: "BattleCode-UET | Autonomous AI Esports Arena",
  description: "Aerospace AI Arena & Tournament Platform for 16 UET engineering teams",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="dot-matrix-bg min-h-screen flex flex-col antialiased">
        <LiveTelemetryTicker
          systemStatus="ONLINE"
          messages={[
            "SUPABASE POSTGRESQL POOLER: STABLE (HIKARI: 2)",
            "CYBER-GRID 16x16 SIMULATION KERNEL: READY (200 TICKS)",
            "SECURE RUNNER: BWRAP CGROUPS MEMORY: 96MB",
            "OFFICIAL PILOT TOURNAMENT: 16 SLOTS OPEN FOR REGISTRATION",
          ]}
        />
        <Navbar />
        <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8">
          {children}
        </main>
      </body>
    </html>
  );
}
