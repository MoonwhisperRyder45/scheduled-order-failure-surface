package example;

public final class OrderJobExample {
    public static void main(String[] args) {
        InfraiErrorReporter reporter = new InfraiErrorReporter(LayeredConfig.fromEnvironment());
        OrderJob job = new OrderJob(new OrderWorkflow(), reporter);
        var completed = job.execute(new OrderWorkflow.Order("order-100", true, true, true, true));
        var failed = job.execute(new OrderWorkflow.Order("order-101", true, true, false, true));
        System.out.println(completed);
        System.out.println(failed);
    }
}
