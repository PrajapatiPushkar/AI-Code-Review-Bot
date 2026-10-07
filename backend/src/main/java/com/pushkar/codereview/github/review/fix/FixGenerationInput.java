package com.pushkar.codereview.github.review.fix;

public class FixGenerationInput {

    private Long findingId;
    private String filePath;
    private Integer lineNumber;
    private Integer endLineNumber;
    private String severity;
    private String category;
    private String message;
    private String suggestion;
    private String source;
    private String ruleId;
    private String codeContext;
    private String developerInstructions;

    public FixGenerationInput() {
    }

    public FixGenerationInput(Long findingId, String filePath, Integer lineNumber, Integer endLineNumber,
                              String severity, String category, String message, String suggestion,
                              String source, String ruleId, String codeContext, String developerInstructions) {
        this.findingId = findingId;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.endLineNumber = endLineNumber;
        this.severity = severity;
        this.category = category;
        this.message = message;
        this.suggestion = suggestion;
        this.source = source;
        this.ruleId = ruleId;
        this.codeContext = codeContext;
        this.developerInstructions = developerInstructions;
    }

    public Long getFindingId() {
        return findingId;
    }

    public void setFindingId(Long findingId) {
        this.findingId = findingId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public Integer getEndLineNumber() {
        return endLineNumber;
    }

    public void setEndLineNumber(Integer endLineNumber) {
        this.endLineNumber = endLineNumber;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getCodeContext() {
        return codeContext;
    }

    public void setCodeContext(String codeContext) {
        this.codeContext = codeContext;
    }

    public String getDeveloperInstructions() {
        return developerInstructions;
    }

    public void setDeveloperInstructions(String developerInstructions) {
        this.developerInstructions = developerInstructions;
    }
}
