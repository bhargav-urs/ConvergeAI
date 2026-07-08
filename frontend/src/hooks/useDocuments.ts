import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { api, ApiError } from "@/lib/api";

export function useDocuments() {
  return useQuery({
    queryKey: ["documents"],
    queryFn: api.listDocuments,
    // Poll while any document is still processing, as a fallback to the
    // WebSocket notification.
    refetchInterval: (query) =>
      query.state.data?.some((d) => d.status === "PROCESSING") ? 3000 : false,
  });
}

export function useUploadDocument() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => api.uploadDocument(file),
    onSuccess: (doc) => {
      toast.info(`Indexing "${doc.filename}"…`);
      void queryClient.invalidateQueries({ queryKey: ["documents"] });
    },
    onError: (error) => {
      toast.error(error instanceof ApiError ? error.message : "Upload failed");
    },
  });
}

export function useDeleteDocument() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.deleteDocument(id),
    onSuccess: () => {
      toast.success("Document deleted");
      void queryClient.invalidateQueries({ queryKey: ["documents"] });
      void queryClient.invalidateQueries({ queryKey: ["questions"] });
    },
    onError: (error) => {
      toast.error(error instanceof ApiError ? error.message : "Delete failed");
    },
  });
}

export function useChunks(documentId: string | null, page: number, size = 10) {
  return useQuery({
    queryKey: ["chunks", documentId, page, size],
    queryFn: () => api.getChunks(documentId as string, page, size),
    enabled: documentId != null,
  });
}
