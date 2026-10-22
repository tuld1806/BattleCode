"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Terminal, Shield, Trophy, Play, FileText, Cpu } from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/store/useAuthStore";

export function Navbar() {
  const pathname = usePathname();
  const { session } = useAuthStore();

  const links = [
    { href: "/", label: "LOBBY", icon: Terminal },
    { href: "/rules", label: "RULES & KITS", icon: FileText },
    { href: "/tournament", label: "TOURNAMENT", icon: Shield },
    { href: "/bracket", label: "BRACKET", icon: Trophy },
    { href: "/standings", label: "STANDINGS", icon: Cpu },
    { href: "/replay", label: "REPLAY THEATER", icon: Play },
  ];

  return (
    <header className="sticky top-0 z-50 w-full border-b border-slate-200 bg-white/90 backdrop-blur-md">
      <div className="max-w-7xl mx-auto flex h-14 items-center justify-between px-4 sm:px-6">
        {/* Brand */}
        <Link href="/" className="flex items-center gap-2.5 group">
          <div className="w-8 h-8 rounded bg-sky-600 flex items-center justify-center text-white font-mono font-bold text-sm shadow-sm transition-transform group-hover:scale-105">
            BC
          </div>
          <div className="flex flex-col">
            <span className="font-mono font-bold text-sm tracking-wider text-slate-900 leading-tight">
              BATTLECODE<span className="text-sky-600">-UET</span>
            </span>
            <span className="text-[9px] font-mono tracking-widest text-slate-500 uppercase">
              AI Arena Cockpit
            </span>
          </div>
        </Link>

        {/* Navigation links */}
        <nav className="hidden md:flex items-center gap-1">
          {links.map((link) => {
            const Icon = link.icon;
            const isActive = pathname === link.href;
            return (
              <Link
                key={link.href}
                href={link.href}
                className={cn(
                  "flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-mono font-medium transition-colors",
                  isActive
                    ? "bg-sky-50 text-sky-700 border border-sky-200"
                    : "text-slate-600 hover:text-slate-900 hover:bg-slate-100"
                )}
              >
                <Icon className="w-3.5 h-3.5" />
                {link.label}
              </Link>
            );
          })}
        </nav>

        {/* Right Action / Auth Status */}
        <div className="flex items-center gap-3">
          <Link
            href="/submit"
            className="hidden sm:inline-flex items-center gap-1.5 px-3 py-1.5 bg-sky-600 hover:bg-sky-700 text-white rounded font-mono text-xs font-semibold shadow-sm transition-colors"
          >
            SUBMIT BOT
          </Link>
          <Link
            href="/admin"
            className="p-1.5 text-slate-500 hover:text-slate-900 rounded hover:bg-slate-100 transition-colors"
            title="Admin Console"
          >
            <Shield className="w-4 h-4" />
          </Link>
          {session ? (
            <span className="text-xs font-mono text-slate-600 font-medium">
              {session.email} ({session.role})
            </span>
          ) : (
            <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-100 text-slate-600 border border-slate-200">
              GUEST
            </span>
          )}
        </div>
      </div>
    </header>
  );
}
