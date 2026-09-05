package example;

import java.util.List;

public final class OrderWorkflow {
    public record Order(String orderId, boolean paymentAccepted, boolean stockAvailable,
                        boolean receiptDelivered, boolean customerUpdated) {}
    public record StageResult(String stage, boolean ok, String detail) {}

    public List<StageResult> run(Order order) {
        return List.of(
                new StageResult("checkout", order.paymentAccepted(), order.paymentAccepted() ? "payment accepted" : "payment declined"),
                new StageResult("fulfillment", order.stockAvailable(), order.stockAvailable() ? "stock reserved" : "stock unavailable"),
                new StageResult("receipt", order.receiptDelivered(), order.receiptDelivered() ? "receipt delivered" : "receipt delivery rejected"),
                new StageResult("customer-update", order.customerUpdated(), order.customerUpdated() ? "customer notified" : "customer update rejected")
        );
    }
}
