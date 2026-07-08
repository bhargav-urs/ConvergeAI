import { useEffect } from "react";
import { stompManager, topics } from "@/lib/stomp";
import { useDebateStore } from "@/store/debateStore";
import { api } from "@/lib/api";
import { useQueryClient } from "@tanstack/react-query";

/**
 * Streams live debate events for the active question into the debate store.
 *
 * On subscribe it also fetches the question once via REST: any event that fired
 * between submission and the subscription landing is recovered from the DB, so
 * the UI can never miss a phase.
 */
export function useDebateSocket(questionId: string | null, isReplay: boolean): void {
  const handleEvent = useDebateStore((s) => s.handleEvent);
  const hydrateFromDetail = useDebateStore((s) => s.hydrateFromDetail);
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!questionId || isReplay) {
      return;
    }
    const unsubscribe = stompManager.subscribe(topics.debate(questionId), (event) => {
      handleEvent(event);
      if (event.type === "debate.completed" || event.type === "debate.error") {
        void queryClient.invalidateQueries({ queryKey: ["questions"] });
        void queryClient.invalidateQueries({ queryKey: ["analytics"] });
      }
    });

    // Recover anything that happened before the subscription was established.
    let cancelled = false;
    api
      .getQuestion(questionId)
      .then((detail) => {
        if (cancelled) return;
        const state = useDebateStore.getState();
        if (state.activeQuestionId !== questionId || detail.status === "PENDING") {
          return;
        }
        // Guard against the hydration race: this REST snapshot may be *older*
        // than live events that already streamed in. Only apply it when the
        // store hasn't progressed yet, or when the snapshot is terminal
        // (page-refresh recovery) — never regress a live timeline.
        const storeHasProgressed = state.stage !== "PENDING" && state.stage !== "IDLE";
        const snapshotIsTerminal = detail.status === "COMPLETED" || detail.status === "FAILED";
        if (!storeHasProgressed || snapshotIsTerminal) {
          hydrateFromDetail(detail, { replay: false });
        }
      })
      .catch(() => {
        // Best-effort sync; live events remain the primary channel.
      });

    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, [questionId, isReplay, handleEvent, hydrateFromDetail, queryClient]);
}
