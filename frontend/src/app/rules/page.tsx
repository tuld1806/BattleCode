import React from "react";
import { SpotlightTechCard, CipherText } from "@/components/ui/kinetic-primitives";
import { BookOpen, Terminal, Shield, Zap, AlertTriangle } from "lucide-react";

export default function RulesPage() {
  return (
    <div className="space-y-8 py-4">
      {/* Header */}
      <div className="space-y-2 border-b border-slate-200 pb-4">
        <div className="inline-flex items-center gap-2 font-mono text-xs text-sky-700 bg-sky-50 px-2.5 py-1 rounded">
          <BookOpen className="w-3.5 h-3.5" />
          TECHNICAL SPECIFICATIONS & BOT PROTOCOL
        </div>
        <h1 className="text-3xl font-mono font-bold text-slate-900">
          <CipherText text="CYBER-GRID RULEBOOK" />
        </h1>
        <p className="text-sm text-slate-600">
          Complete guide to the 16x16 deterministic simulation, entity stats, combat rules, and communication protocol.
        </p>
      </div>

      {/* Grid Specs & Turn Limits */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <SpotlightTechCard className="space-y-2">
          <span className="text-xs font-mono text-slate-500">ARENA GEOMETRY</span>
          <div className="text-2xl font-mono font-bold text-slate-900">16 x 16 GRID</div>
          <p className="text-xs text-slate-600">
            Symmetric procedural map seeded randomly. Rotational symmetry ensures fair distance between cores and resource nodes.
          </p>
        </SpotlightTechCard>

        <SpotlightTechCard className="space-y-2">
          <span className="text-xs font-mono text-slate-500">MATCH DURATION</span>
          <div className="text-2xl font-mono font-bold text-slate-900">200 TICKS MAX</div>
          <p className="text-xs text-slate-600">
            If both cores survive past Tick 200, winner is decided by: Core HP &rarr; Total Unit HP &rarr; Remaining Resources.
          </p>
        </SpotlightTechCard>

        <SpotlightTechCard className="space-y-2">
          <span className="text-xs font-mono text-slate-500">COMMUNICATION</span>
          <div className="text-2xl font-mono font-bold text-slate-900">NDJSON / STDIN</div>
          <p className="text-xs text-slate-600">
            Single-line JSON per tick. Advance immediately upon receiving newline; no process restart per tick.
          </p>
        </SpotlightTechCard>
      </div>

      {/* Units & Economy Table */}
      <div className="space-y-4">
        <h2 className="text-lg font-mono font-bold text-slate-900 flex items-center gap-2">
          <Shield className="w-4 h-4 text-sky-600" />
          Unit Types & Specifications
        </h2>
        <div className="overflow-x-auto rounded border border-slate-300 bg-white">
          <table className="w-full text-left font-mono text-xs">
            <thead className="bg-slate-100 border-b border-slate-200 text-slate-600">
              <tr>
                <th className="p-3">UNIT</th>
                <th className="p-3">BUILD COST</th>
                <th className="p-3">MAX HP</th>
                <th className="p-3">ATTACK</th>
                <th className="p-3">VISION (R)</th>
                <th className="p-3">SPECIAL ABILITY</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 text-slate-800">
              <tr>
                <td className="p-3 font-bold text-sky-700">CORE</td>
                <td className="p-3">—</td>
                <td className="p-3">100</td>
                <td className="p-3">0</td>
                <td className="p-3">4</td>
                <td className="p-3">Generates 10 Data/tick; Spawns units; Loss = Defeat</td>
              </tr>
              <tr>
                <td className="p-3 font-bold text-sky-700">WORKER</td>
                <td className="p-3">20 Data</td>
                <td className="p-3">25</td>
                <td className="p-3">5</td>
                <td className="p-3">2</td>
                <td className="p-3">Mines +5 Data/tick when on Resource Node</td>
              </tr>
              <tr>
                <td className="p-3 font-bold text-sky-700">SCOUT</td>
                <td className="p-3">25 Data</td>
                <td className="p-3">20</td>
                <td className="p-3">8</td>
                <td className="p-3">5</td>
                <td className="p-3">High mobility & wide vision radius for reconnaissance</td>
              </tr>
              <tr>
                <td className="p-3 font-bold text-sky-700">TANK</td>
                <td className="p-3">50 Data</td>
                <td className="p-3">60</td>
                <td className="p-3">20</td>
                <td className="p-3">2</td>
                <td className="p-3">Heavy armor frontline combat unit</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Protocol & Limits Notice */}
      <SpotlightTechCard className="border-l-4 border-amber-500 bg-amber-50/60 space-y-2">
        <div className="flex items-center gap-2 font-mono text-xs font-bold text-amber-800">
          <AlertTriangle className="w-4 h-4 text-amber-600" />
          DISQUALIFICATION & SANDBOX RULES
        </div>
        <ul className="list-disc list-inside space-y-1 text-xs text-amber-900 font-sans">
          <li><strong>Warmup:</strong> Bot must print <code className="bg-amber-100 px-1 py-0.5 rounded font-mono font-bold">READY\n</code> within 1,000 ms on startup.</li>
          <li><strong>Turn Budget:</strong> 50 ms base time + 2,000 ms Time Bank reserve. Exhausting Time Bank causes instant disqualification.</li>
          <li><strong>I/O Caps:</strong> Max 16 KB stdout per turn line. Max 200 chars/tick stderr logging.</li>
          <li><strong>Environment:</strong> Network access is disabled (<code className="font-mono">--unshare-net</code>); file system is read-only except for private tmpfs.</li>
        </ul>
      </SpotlightTechCard>
    </div>
  );
}
