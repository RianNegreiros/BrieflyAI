package com.brieflyai.BrieflyAI.model.enums;

public enum ResearchOperation {
    SUMMARIZE("summarize",
            "You are an expert summarizer. Summarize the following article into a highly concise overview, focusing only on the main points and key details. Aim for a summary that is no more than 10% of the original content's length. Avoid personal opinion and ensure clarity. Match the language of the input text in your response."), SUGGEST(
                    "suggest",
                    """
                            You are a research assistant helping users explore topics deeper. Based on the article below, provide exactly 3 suggestions in the following format:

                            **DEEPER DIVE:**
                            [Suggest one authoritative source, academic paper, book, or expert resource for comprehensive understanding. Include specific URLs, DOIs, or publication details when possible.]

                            **CRITICAL THINKING:**
                            [Pose one thought-provoking question that challenges assumptions, explores counterarguments, or examines potential limitations of the article's claims.]

                            **ACTIONABLE NEXT STEP:**
                            [Recommend one concrete, practical action the reader can take - such as a specific tool to try, experiment to conduct, community to join, or skill to develop.]

                            Keep each suggestion concise (2-3 sentences max) and directly relevant to the article's content. Avoid generic recommendations. Match the language of the input text in your response.""");

    private final String operation;
    private final String promptTemplate;

    ResearchOperation(String operation, String promptTemplate) {
        this.operation = operation;
        this.promptTemplate = promptTemplate;
    }

    public String getOperation() {
        return operation;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public static ResearchOperation fromString(String operation) {
        for (ResearchOperation op : values()) {
            if (op.operation.equalsIgnoreCase(operation)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown operation: " + operation);
    }
}
