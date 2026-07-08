import type { AgentName } from "./types";
import { Brain, Wrench, ShieldCheck, type LucideIcon } from "lucide-react";

export interface AgentMeta {
  name: AgentName;
  displayName: string;
  role: string;
  tagline: string;
  model: string;
  icon: LucideIcon;
  /** Tailwind utility fragments so each agent has a consistent visual identity. */
  colors: {
    text: string;
    bg: string;
    border: string;
    solid: string;
  };
  chartColor: string;
}

export const AGENTS: Record<AgentName, AgentMeta> = {
  ANALYST: {
    name: "ANALYST",
    displayName: "The Analyst",
    role: "Deep logical reasoning",
    tagline: "Breaks the retrieved context down into verified core facts.",
    model: "Gemini 2.5 Flash",
    icon: Brain,
    colors: {
      text: "text-violet-700",
      bg: "bg-violet-50",
      border: "border-violet-200",
      solid: "bg-violet-600",
    },
    chartColor: "#7c3aed",
  },
  ENGINEER: {
    name: "ENGINEER",
    displayName: "The Engineer",
    role: "Practical synthesis",
    tagline: "Turns extracted facts into a direct, actionable answer.",
    model: "GLM-4.7",
    icon: Wrench,
    colors: {
      text: "text-cyan-700",
      bg: "bg-cyan-50",
      border: "border-cyan-200",
      solid: "bg-cyan-600",
    },
    chartColor: "#0891b2",
  },
  REVIEWER: {
    name: "REVIEWER",
    displayName: "The Reviewer",
    role: "Adversarial fact-checking",
    tagline: "Hunts hallucinations and unsupported claims in peer answers.",
    model: "Llama 3.3",
    icon: ShieldCheck,
    colors: {
      text: "text-amber-700",
      bg: "bg-amber-50",
      border: "border-amber-200",
      solid: "bg-amber-600",
    },
    chartColor: "#d97706",
  },
};

export const AGENT_ORDER: AgentName[] = ["ANALYST", "ENGINEER", "REVIEWER"];
