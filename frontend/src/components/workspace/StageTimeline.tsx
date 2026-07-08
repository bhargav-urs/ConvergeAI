import { Check, CircleDashed, Loader2, X } from "lucide-react";
import { cn } from "@/lib/utils";
import type { DebateMode, QuestionStatus } from "@/lib/types";

type Stage = QuestionStatus | "IDLE";

interface StageDef {
  key: QuestionStatus;
  label: string;
  description: string;
}

const NORMAL_STAGES: StageDef[] = [
  { key: "RETRIEVING", label: "Retrieving context", description: "Vector similarity search" },
  { key: "DEBATING_ROUND_1", label: "Round 1 · Initial answers", description: "Agents answer independently" },
  { key: "DEBATING_ROUND_2", label: "Round 2 · Cross-critique", description: "Agents challenge each other" },
  { key: "DEBATING_ROUND_3", label: "Round 3 · Revision", description: "Agents revise under critique" },
  { key: "CONSENSUS", label: "Consensus", description: "Synthesizing the final answer" },
];

// Fast mode skips critique and revision entirely.
const FAST_STAGES: StageDef[] = [
  { key: "RETRIEVING", label: "Retrieving context", description: "Vector similarity search" },
  { key: "DEBATING_ROUND_1", label: "Independent answers", description: "Agents answer concisely" },
  { key: "CONSENSUS", label: "Consensus", description: "Synthesizing the final answer" },
];

const ORDER: Record<Stage, number> = {
  IDLE: -1,
  PENDING: 0,
  RETRIEVING: 1,
  DEBATING_ROUND_1: 2,
  DEBATING_ROUND_2: 3,
  DEBATING_ROUND_3: 4,
  CONSENSUS: 5,
  COMPLETED: 6,
  FAILED: 6,
};

interface StageTimelineProps {
  stage: Stage;
  failed: boolean;
  mode: DebateMode;
}

export function StageTimeline({ stage, failed, mode }: StageTimelineProps) {
  const current = ORDER[stage];
  const stages = mode === "FAST" ? FAST_STAGES : NORMAL_STAGES;

  return (
    <ol className="space-y-1">
      {stages.map((item) => {
        const position = ORDER[item.key];
        // In fast mode COMPLETED (6) must mark CONSENSUS (5) done, and skipped
        // rounds never appear, so position comparison stays correct.
        const isDone = current > position;
        const isActive = current === position;
        const isFailedHere = failed && isActive;

        return (
          <li key={item.key} className="flex items-start gap-2.5">
            <span
              className={cn(
                "mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full border",
                isDone && "border-emerald-500 bg-emerald-500 text-white",
                isActive && !isFailedHere && "border-primary bg-primary text-primary-foreground",
                isFailedHere && "border-destructive bg-destructive text-white",
                !isDone && !isActive && "border-border bg-card text-muted-foreground",
              )}
            >
              {isDone ? (
                <Check className="h-3 w-3" />
              ) : isFailedHere ? (
                <X className="h-3 w-3" />
              ) : isActive ? (
                <Loader2 className="h-3 w-3 animate-spin" />
              ) : (
                <CircleDashed className="h-3 w-3" />
              )}
            </span>
            <div className="min-w-0 pb-1.5">
              <p
                className={cn(
                  "text-sm font-medium leading-5",
                  isActive && !isFailedHere && "text-primary",
                  isFailedHere && "text-destructive",
                  !isActive && !isDone && "text-muted-foreground",
                )}
              >
                {item.label}
              </p>
              <p className="text-xs text-muted-foreground">{item.description}</p>
            </div>
          </li>
        );
      })}
    </ol>
  );
}
