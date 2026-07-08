package com.convergeai.domain;

/**
 * The three debate participants. Display names and role blurbs live here so the
 * backend is the single source of truth for agent identity.
 */
public enum AgentName {
    ANALYST("The Analyst", "Deep logical reasoning and step-by-step fact extraction"),
    ENGINEER("The Engineer", "Practical synthesis into a direct, actionable answer"),
    REVIEWER("The Reviewer", "Adversarial fact-checking against the document context");

    private final String displayName;
    private final String roleDescription;

    AgentName(String displayName, String roleDescription) {
        this.displayName = displayName;
        this.roleDescription = roleDescription;
    }

    public String displayName() {
        return displayName;
    }

    public String roleDescription() {
        return roleDescription;
    }
}
