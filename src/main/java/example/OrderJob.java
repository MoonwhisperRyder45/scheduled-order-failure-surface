package example;

public final class OrderJob {
    public record JobResult(String orderId, String status, String failedStage) {}
    private final OrderWorkflow workflow;
    public interface FailureReporter { void capture(String orderId, String stage, String detail); }
    private final FailureReporter reporter;

    public OrderJob(OrderWorkflow workflow, FailureReporter reporter) {
        this.workflow = workflow; this.reporter = reporter;
    }

    public JobResult execute(OrderWorkflow.Order order) {
        for (OrderWorkflow.StageResult stage : workflow.run(order)) {
            if (!stage.ok()) {
                reporter.capture(order.orderId(), stage.stage(), stage.detail());
                return new JobResult(order.orderId(), "FAILED", stage.stage());
            }
        }
        return new JobResult(order.orderId(), "COMPLETED", "");
    }
}
