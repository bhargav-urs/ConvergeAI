import { AlertTriangle, Clock } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { AGENTS } from "@/lib/agents";
import type { AgentMessage, AgentName } from "@/lib/types";
import type { AgentSlot } from "@/store/debateStore";
import { cn, formatDuration } from "@/lib/utils";
import { MarkdownContent } from "./MarkdownContent";

interface AgentDetailDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  agent: AgentName;
  slot: AgentSlot;
}

function MessageBlock({ message, emptyText }: { message: AgentMessage | null; emptyText: string }) {
  if (!message) {
    return <p className="text-sm italic text-muted-foreground">{emptyText}</p>;
  }
  if (message.status === "FAILED") {
    return (
      <div className="flex items-start gap-2 rounded-md border border-amber-200 bg-amber-50 p-3">
        <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />
        <div>
          <p className="text-sm font-medium text-amber-800">This call failed</p>
          <p className="text-xs text-amber-700">{message.errorMessage ?? "Unknown error"}</p>
        </div>
      </div>
    );
  }
  return (
    <div className="space-y-2">
      {message.latencyMs != null && (
        <span className="flex w-fit items-center gap-1 text-xs text-muted-foreground">
          <Clock className="h-3 w-3" />
          {formatDuration(message.latencyMs)}
        </span>
      )}
      <MarkdownContent content={message.content ?? ""} />
    </div>
  );
}

/**
 * The expandable "Agent Detail Panel": full trail of the agent's initial
 * answer, the critique it received, the critique it authored, and its revision.
 */
export function AgentDetailDialog({ open, onOpenChange, agent, slot }: AgentDetailDialogProps) {
  const meta = AGENTS[agent];
  const Icon = meta.icon;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] max-w-3xl overflow-hidden">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <span
              className={cn(
                "flex h-8 w-8 items-center justify-center rounded-md",
                meta.colors.bg,
              )}
            >
              <Icon className={cn("h-4 w-4", meta.colors.text)} />
            </span>
            {meta.displayName}
            <Badge variant="outline">{meta.model}</Badge>
          </DialogTitle>
          <DialogDescription>{meta.tagline}</DialogDescription>
        </DialogHeader>

        <Tabs defaultValue="initial">
          <TabsList className="grid w-full grid-cols-4">
            <TabsTrigger value="initial">Initial answer</TabsTrigger>
            <TabsTrigger value="received">Critique received</TabsTrigger>
            <TabsTrigger value="authored">Critique authored</TabsTrigger>
            <TabsTrigger value="revision">Revised answer</TabsTrigger>
          </TabsList>

          <div className="scrollbar-thin max-h-[52vh] overflow-y-auto pr-1">
            <TabsContent value="initial">
              <MessageBlock message={slot.response} emptyText="No initial answer yet." />
            </TabsContent>
            <TabsContent value="received">
              {slot.revision?.critiqueReceived ? (
                <MarkdownContent content={slot.revision.critiqueReceived} />
              ) : (
                <p className="text-sm italic text-muted-foreground">
                  No peer critiques received yet.
                </p>
              )}
            </TabsContent>
            <TabsContent value="authored">
              <MessageBlock message={slot.critique} emptyText="No critique authored yet." />
            </TabsContent>
            <TabsContent value="revision">
              <MessageBlock message={slot.revision} emptyText="No revised answer yet." />
            </TabsContent>
          </div>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
}
