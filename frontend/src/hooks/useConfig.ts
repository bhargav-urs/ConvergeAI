import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";

/** Server runtime config (is the OpenRouter key present, which models run). */
export function useConfig() {
  return useQuery({
    queryKey: ["config"],
    queryFn: api.getConfig,
    staleTime: 60_000,
    retry: 1,
  });
}
