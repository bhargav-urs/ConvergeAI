import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { api, ApiError } from "@/lib/api";
import type { SubmitQuestionRequest } from "@/lib/types";
import { useDebateStore } from "@/store/debateStore";

export function useQuestions(documentId?: string) {
  return useQuery({
    queryKey: ["questions", documentId ?? "all"],
    queryFn: () => api.listQuestions(documentId),
  });
}

export function useQuestionDetail(questionId: string | null) {
  return useQuery({
    queryKey: ["question", questionId],
    queryFn: () => api.getQuestion(questionId as string),
    enabled: questionId != null,
  });
}

/**
 * Submits a question over REST, then arms the debate store so the workspace
 * immediately switches into live-streaming mode for the returned question id.
 */
export function useSubmitQuestion() {
  const queryClient = useQueryClient();
  const startDebate = useDebateStore((s) => s.startDebate);

  return useMutation({
    mutationFn: (request: SubmitQuestionRequest) => api.submitQuestion(request),
    onSuccess: (summary) => {
      startDebate(summary.id, summary.questionText, summary.mode);
      void queryClient.invalidateQueries({ queryKey: ["questions"] });
    },
    onError: (error) => {
      toast.error(
        error instanceof ApiError ? error.message : "Could not submit the question",
      );
    },
  });
}
