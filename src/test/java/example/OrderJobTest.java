package example;

public final class OrderJobTest {
    public static void main(String[] args) {
        OrderJob job = new OrderJob(new OrderWorkflow(), (id, stage, detail) -> {});
        var result = job.execute(new OrderWorkflow.Order("test-1", true, true, false, true));
        if (!result.status().equals("FAILED") || !result.failedStage().equals("receipt")) {
            throw new AssertionError("receipt failure must stop the order: " + result);
        }
        System.out.println("OrderJobTest passed");
    }
}
