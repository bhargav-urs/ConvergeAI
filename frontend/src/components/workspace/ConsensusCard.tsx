import { CheckCircle2, GitMerge, MinusCircle } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { MarkdownContent } from "./MarkdownContent";
import type { Consensus } from "@/lib/types";
import { cn } from "@/lib/utils";

interface ConsensusCardProps {
  consensus: Consensus;
}

function confidenceColor(score: number): string {
  if (score >= 75) return "bg-emerald-500";
  if (score >= 50) return "bg-amber-500";
  return "bg-red-500";
}

export function ConsensusCard({ consensus }: ConsensusCardProps) {
  return (
    <Card className="border-primary/30 bg-gradient-to-b from-accent/50 to-card">
      <CardHeader className="pb-3">
        <CardTitle className="flex items-center gap-2 text-sm">
          <GitMerge className="h-4 w-4 text-primary" />
          Consensus answer
        </CardTitle>
        <div className="flex items-center gap-2 pt-1">
          <Progress
            value={consensus.confidenceScore}
            className="h-1.5"
            indicatorClassName={cn(confidenceColor(consensus.confidenceScore))}
          />
          <span className="shrink-0 text-xs font-semibold">
            {consensus.confidenceScore}% confidence
          </span>
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        <MarkdownContent content={consensus.finalAnswer} />

        {(consensus.agreementPoints?.length ?? 0) > 0 && (
          <div>
            <p className="mb-1 text-xs font-semibold text-emerald-700">Areas of agreement</p>
            <ul className="space-y-1">
              {consensus.agreementPoints?.map((point, i) => (
                <li key={i} className="flex items-start gap-1.5 text-xs text-foreground/90">
                  <CheckCircle2 className="mt-0.5 h-3 w-3 shrink-0 text-emerald-600" />
                  {point}
                </li>
              ))}
            </ul>
          </div>
        )}

        {(consensus.disagreementPoints?.length ?? 0) > 0 && (
          <div>
            <p className="mb-1 text-xs font-semibold text-amber-700">Areas of disagreement</p>
            <ul className="space-y-1">
              {consensus.disagreementPoints?.map((point, i) => (
                <li key={i} className="flex items-start gap-1.5 text-xs text-foreground/90">
                  <MinusCircle className="mt-0.5 h-3 w-3 shrink-0 text-amber-600" />
                  {point}
                </li>
              ))}
            </ul>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
