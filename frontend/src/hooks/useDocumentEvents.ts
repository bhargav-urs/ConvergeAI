import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { stompManager, topics } from "@/lib/stomp";
import type { DocumentFailedPayload, DocumentIndexedPayload } from "@/lib/types";

/**
 * Listens on /topic/documents for ingestion outcomes and refreshes the
 * document list (with a toast) when indexing finishes or fails.
 */
export function useDocumentEvents(): void {
  const queryClient = useQueryClient();

  useEffect(() => {
    const unsubscribe = stompManager.subscribe(topics.documents, (event) => {
      if (event.type === "document.indexed") {
        const payload = event.payload as DocumentIndexedPayload;
        toast.success(`Document indexed into ${payload.chunkCount} chunks`);
        void queryClient.invalidateQueries({ queryKey: ["documents"] });
      } else if (event.type === "document.failed") {
        const payload = event.payload as DocumentFailedPayload;
        toast.error(`Document indexing failed: ${payload.error}`);
        void queryClient.invalidateQueries({ queryKey: ["documents"] });
      }
    });
    return unsubscribe;
  }, [queryClient]);
}
