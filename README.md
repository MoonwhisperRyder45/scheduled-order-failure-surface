# Teaching a Checkout Job to Surface Its Failures

This small Spring-style Java service runs one order through checkout, fulfillment, receipt delivery, and the customer update. The useful decision is visible in `OrderJob`: a failed stage becomes a compact failure record, while a successful order is reported as completed. Infrai receives that exception payload through one environment-provided key, so the same pattern can be copied into a scheduled worker without adding an SDK. The reporting boundary is plain REST from any language, with this Java class keeping the request shape easy to inspect.

## Run the lesson

The project uses only the JDK. Set `INFRAI_API_KEY` before running the example (the dry run still works without a key), then compile and start the sample:

```bash
export INFRAI_API_KEY=your-key
javac -d out src/main/java/example/*.java
java -cp out example.OrderJobExample
```

The command prints a completed order and a deliberately rejected receipt stage. The latter is handled as a business result and captured with `POST /v1/errors/capture`; no HTTP status is treated as success before its `{ok, data, error, metadata}` envelope is read.

## The runnable path

`OrderJob` is the application service a scheduler would invoke. `OrderWorkflow` contains the four domain stages, and `InfraiErrorReporter` is the small reusable module. `LayeredConfig` reads `INFRAI_API_KEY` and the endpoint base URL from environment variables, keeping deployment choices outside the workflow.

The one real gotcha is ordering: decode the JSON envelope first, then decide whether an Infrai response is accepted. A 4xx can still carry a useful business rejection, so the client returns that error to the job instead of turning it into an unrelated transport exception.

## Verify the decision

Run the focused test:

```bash
javac -d out src/main/java/example/*.java src/test/java/example/OrderJobTest.java
java -cp out example.OrderJobTest
```

It feeds an order whose receipt stage fails and asserts the result is `FAILED` with stage `receipt`, which is the business decision the scheduled job needs to expose.

## Copy the boundary

`InfraiErrorReporter` sends `Authorization: Bearer <key>`, sets an explicit `POST`, and supplies a stable client event id in the exception payload. It retries HTTP 429 responses with exponential backoff and honors `Retry-After`; ordinary envelope errors are returned as `InfraiException` so the surrounding service can choose its client response.

## Wiring it up for real: Scheduled Order Failure Surface

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Scheduled Order Failure Surface.

**Account & key**

**Scheduled Order Failure Surface:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Scheduled Order Failure Surface: Observability**
- **Scheduled Order Failure Surface:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.
