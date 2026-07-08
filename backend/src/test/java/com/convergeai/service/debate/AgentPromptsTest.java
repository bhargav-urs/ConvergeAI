package com.convergeai.service.debate;

import com.convergeai.domain.AgentName;
import com.convergeai.dto.ContextSnippetDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AgentPromptsTest {

    private final List<ContextSnippetDto> snippets = List.of(
            new ContextSnippetDto(UUID.randomUUID(), 0, 1, "First chunk content.", 0.12),
            new ContextSnippetDto(UUID.randomUUID(), 3, 2, "Second chunk content.", 0.19));

    @Test
    void contextBlockNumbersChunksByRank() {
        String block = AgentPrompts.contextBlock(snippets);
        assertThat(block).contains("[Chunk 1]", "First chunk content.", "[Chunk 2]", "Second chunk content.");
    }

    @Test
    void round2ExcludesSelfAnswer() {
        Map<AgentName, String> answers = Map.of(
                AgentName.ANALYST, "analyst answer marker",
                AgentName.ENGINEER, "engineer answer marker",
                AgentName.REVIEWER, "reviewer answer marker");
        String prompt = AgentPrompts.round2(AgentName.ANALYST, "Q?", "ctx", answers);

        assertThat(prompt).contains("engineer answer marker", "reviewer answer marker");
        assertThat(prompt).doesNotContain("analyst answer marker");
    }

    @Test
    void everyAgentHasDistinctSystemPrompt() {
        String analyst = AgentPrompts.systemPrompt(AgentName.ANALYST);
        String engineer = AgentPrompts.systemPrompt(AgentName.ENGINEER);
        String reviewer = AgentPrompts.systemPrompt(AgentName.REVIEWER);

        assertThat(analyst).contains("The Analyst").isNotEqualTo(engineer);
        assertThat(engineer).contains("The Engineer").isNotEqualTo(reviewer);
        assertThat(reviewer).contains("The Reviewer");
    }

    @Test
    void consensusPromptDemandsStrictJson() {
        String prompt = AgentPrompts.consensus("Q?", "ctx",
                Map.of(AgentName.ANALYST, "revised A"), true);
        assertThat(prompt)
                .contains("final_answer")
                .contains("areas_of_agreement")
                .contains("areas_of_disagreement")
                .contains("confidence_score")
                .contains("ONLY a JSON object");
    }

    @Test
    void consensusPromptReflectsFastModeInputs() {
        String normal = AgentPrompts.consensus("Q?", "ctx", Map.of(AgentName.ANALYST, "a"), true);
        String fast = AgentPrompts.consensus("Q?", "ctx", Map.of(AgentName.ANALYST, "a"), false);
        assertThat(normal).contains("Revised answer from");
        assertThat(fast).contains("Answer from").doesNotContain("Revised answer from");
    }

    @Test
    void round1ConciseVariantCapsLength() {
        String normal = AgentPrompts.round1("Q?", "ctx", false);
        String fast = AgentPrompts.round1("Q?", "ctx", true);
        assertThat(normal).doesNotContain("under 150 words");
        assertThat(fast).contains("under 150 words");
    }
}
