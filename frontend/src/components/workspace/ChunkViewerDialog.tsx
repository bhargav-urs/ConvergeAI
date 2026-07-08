import { useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { useChunks } from "@/hooks/useDocuments";
import type { DocumentDto } from "@/lib/types";

interface ChunkViewerDialogProps {
  document: DocumentDto | null;
  onClose: () => void;
}

/** Paged view of a document's indexed chunks — the "chunk visualization". */
export function ChunkViewerDialog({ document, onClose }: ChunkViewerDialogProps) {
  const [page, setPage] = useState(0);
  const { data, isLoading } = useChunks(document?.id ?? null, page, 8);

  const open = document != null;

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next) {
          setPage(0);
          onClose();
        }
      }}
    >
      <DialogContent className="max-h-[85vh] max-w-3xl">
        <DialogHeader>
          <DialogTitle>Indexed chunks</DialogTitle>
          <DialogDescription>
            {document?.filename} · {document?.chunkCount} chunks · each embedded as a
            384-dimensional vector
          </DialogDescription>
        </DialogHeader>

        <div className="scrollbar-thin max-h-[55vh] space-y-2 overflow-y-auto pr-1">
          {isLoading &&
            Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-20 w-full" />)}
          {data?.content.map((chunk) => (
            <div key={chunk.id} className="rounded-md border p-3">
              <div className="mb-1.5 flex items-center gap-2">
                <Badge variant="secondary" className="text-[10px]">
                  Chunk {chunk.chunkIndex + 1}
                </Badge>
                <span className="text-[11px] text-muted-foreground">{chunk.charCount} chars</span>
              </div>
              <p className="whitespace-pre-wrap text-xs leading-relaxed text-foreground/85">
                {chunk.content}
              </p>
            </div>
          ))}
        </div>

        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
              <ChevronLeft className="h-4 w-4" /> Previous
            </Button>
            <span className="text-xs text-muted-foreground">
              Page {page + 1} of {data.totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={page + 1 >= data.totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              Next <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
