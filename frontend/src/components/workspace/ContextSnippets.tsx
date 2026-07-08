import { useState } from "react";
import { ChevronDown, FileSearch } from "lucide-react";
import { cn } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import type { ContextSnippet } from "@/lib/types";

interface ContextSnippetsProps {
  snippets: ContextSnippet[];
}

/**
 * The retrieved chunks grounding the debate. Distance is cosine distance, so
 * similarity ≈ 1 - distance is shown as a match percentage.
 */
export function ContextSnippets({ snippets }: ContextSnippetsProps) {
  const [openRank, setOpenRank] = useState<number | null>(null);

  if (snippets.length === 0) {
    return null;
  }

  return (
    <div className="space-y-1.5">
      {snippets.map((snippet) => {
        const isOpen = openRank === snippet.rank;
        const similarity = Math.max(0, Math.min(1, 1 - snippet.distance));
        return (
          <div key={snippet.rank} className="rounded-md border bg-card">
            <button
              type="button"
              onClick={() => setOpenRank(isOpen ? null : snippet.rank)}
              className="flex w-full items-center gap-2 px-2.5 py-2 text-left"
            >
              <FileSearch className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
              <span className="text-xs font-medium">Chunk {snippet.rank}</span>
              <Badge variant="secondary" className="text-[10px]">
                {(similarity * 100).toFixed(0)}% match
              </Badge>
              <span className="min-w-0 flex-1 truncate text-xs text-muted-foreground">
                {snippet.content}
              </span>
              <ChevronDown
                className={cn(
                  "h-3.5 w-3.5 shrink-0 text-muted-foreground transition-transform",
                  isOpen && "rotate-180",
                )}
              />
            </button>
            {isOpen && (
              <div className="border-t px-3 py-2">
                <p className="whitespace-pre-wrap text-xs leading-relaxed text-foreground/90">
                  {snippet.content}
                </p>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}
