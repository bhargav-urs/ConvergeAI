import { useState } from "react";
import { AlertTriangle, Clock, Loader2, Maximize2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { AGENTS } from "@/lib/agents";
import type { AgentName, QuestionStatus } from "@/lib/types";
import type { AgentSlot } from "@/store/debateStore";
import { cn, formatDuration, truncate } from "@/lib/utils";
import { AgentDetailDialog } from "./AgentDetailDialog";

type Stage = QuestionStatus | "IDLE";

interface AgentCardProps {
  agent: AgentName;
  slot: AgentSlot;
  stage: Stage;
}

/** Which phase output the card previews, given how far the debate has advanced. */
function latestActivity(
  slot: AgentSlot,
): { label: string; content: string | null; failure: string | null } | null {
  const describe = (label: string, message: typeof slot.response) =>
    message && {
      label,
      content: message.status === "OK" ? message.content : null,
      failure:
        message.status === "FAILED"
          ? (message.errorMessage ?? "This step failed.")
          : null,
    };
  return (
    describe("Revised answer", slot.revision) ??
    describe("Critique of peers", slot.critique) ??
    describe("Initial answer", slot.response) ??
    null
  );
}

function isThinking(slot: AgentSlot, stage: Stage): boolean {
  if (stage === "DEBATING_ROUND_1") return !slot.response;
  if (stage === "DEBATING_ROUND_2") return !slot.critique && slot.response?.status === "OK";
  if (stage === "DEBATING_ROUND_3") return !slot.revision && slot.response?.status === "OK";
  return false;
}

export function AgentCard({ agent, slot, stage }: AgentCardProps) {
  const [detailOpen, setDetailOpen] = useState(false);
  const meta = AGENTS[agent];
  const Icon = meta.icon;
  const activity = latestActivity(slot);
  const thinking = isThinking(slot, stage);
  const hasFailure =
    slot.response?.status === "FAILED" ||
    slot.critique?.status === "FAILED" ||
    slot.revision?.status === "FAILED";
  const hasAnything = slot.response || slot.critique || slot.revision;

  return (
    <>
      <Card className={cn("transition-shadow", thinking && "shadow-md ring-1 ring-primary/20")}>
        <CardHeader className="p-3 pb-2">
          <div className="flex items-center gap-2">
            <span
              className={cn(
                "flex h-7 w-7 shrink-0 items-center justify-center rounded-md",
                meta.colors.bg,
              )}
            >
              <Icon className={cn("h-4 w-4", meta.colors.text)} />
            </span>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-semibold leading-4">{meta.displayName}</p>
              <p className="truncate text-[11px] text-muted-foreground">{meta.model}</p>
            </div>
            {thinking && <Loader2 className="h-4 w-4 animate-spin text-primary" />}
            {hasFailure && !thinking && <AlertTriangle className="h-4 w-4 text-amber-500" />}
            {hasAnything && (
              <Button
                variant="ghost"
                size="icon"
                className="h-7 w-7"
                onClick={() => setDetailOpen(true)}
                title="View full debate trail"
              >
                <Maximize2 className="h-3.5 w-3.5" />
              </Button>
            )}
          </div>
        </CardHeader>
        <CardContent className="p-3 pt-0">
          {activity ? (
            <div>
              <div className="mb-1 flex items-center gap-1.5">
                <Badge variant="secondary" className="text-[10px]">
                  {activity.label}
                </Badge>
                {slot.response?.latencyMs != null && (
                  <span className="flex items-center gap-0.5 text-[10px] text-muted-foreground">
                    <Clock className="h-2.5 w-2.5" />
                    {formatDuration(slot.response.latencyMs)}
                  </span>
                )}
              </div>
              {activity.content ? (
                <p className="text-xs leading-relaxed text-foreground/85">
                  {truncate(activity.content, 220)}
                </p>
              ) : (
                <p className="text-xs leading-relaxed text-amber-700">
                  {truncate(
                    activity.failure ?? "This step failed.",
                    180,
                  )}
                </p>
              )}
            </div>
          ) : thinking ? (
            <p className="text-xs italic text-muted-foreground animate-pulse-soft">
              Reading the retrieved context…
            </p>
          ) : (
            <p className="text-xs italic text-muted-foreground">Waiting for the debate to start.</p>
          )}
        </CardContent>
      </Card>

      <AgentDetailDialog
        open={detailOpen}
        onOpenChange={setDetailOpen}
        agent={agent}
        slot={slot}
      />
    </>
  );
}
