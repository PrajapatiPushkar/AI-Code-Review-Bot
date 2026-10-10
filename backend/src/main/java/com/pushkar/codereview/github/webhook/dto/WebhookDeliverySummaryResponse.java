package com.pushkar.codereview.github.webhook.dto;

public class WebhookDeliverySummaryResponse {

    private long totalDeliveries;
    private long completedDeliveries;
    private long processingDeliveries;
    private long failedDeliveries;
    private long ignoredDeliveries;

    public WebhookDeliverySummaryResponse() {
    }

    public WebhookDeliverySummaryResponse(long totalDeliveries,
                                          long completedDeliveries,
                                          long processingDeliveries,
                                          long failedDeliveries,
                                          long ignoredDeliveries) {
        this.totalDeliveries = totalDeliveries;
        this.completedDeliveries = completedDeliveries;
        this.processingDeliveries = processingDeliveries;
        this.failedDeliveries = failedDeliveries;
        this.ignoredDeliveries = ignoredDeliveries;
    }

    public long getTotalDeliveries() {
        return totalDeliveries;
    }

    public void setTotalDeliveries(long totalDeliveries) {
        this.totalDeliveries = totalDeliveries;
    }

    public long getCompletedDeliveries() {
        return completedDeliveries;
    }

    public void setCompletedDeliveries(long completedDeliveries) {
        this.completedDeliveries = completedDeliveries;
    }

    public long getProcessingDeliveries() {
        return processingDeliveries;
    }

    public void setProcessingDeliveries(long processingDeliveries) {
        this.processingDeliveries = processingDeliveries;
    }

    public long getFailedDeliveries() {
        return failedDeliveries;
    }

    public void setFailedDeliveries(long failedDeliveries) {
        this.failedDeliveries = failedDeliveries;
    }

    public long getIgnoredDeliveries() {
        return ignoredDeliveries;
    }

    public void setIgnoredDeliveries(long ignoredDeliveries) {
        this.ignoredDeliveries = ignoredDeliveries;
    }
}
