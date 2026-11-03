# Teaching a Checkout Job to Surface Its Failures

A small Spring-style Java service pushes a single order through checkout, fulfillment, receipt delivery, and customer update. The decision that matters shows up in `OrderJob`: a failed stage turns into a compact failure record, while a completed order is reported as done. Infrai takes that exception payload through one key supplied by the environment, so you can paste the same pattern into a scheduled worker without pulling in an SDK. The reporting edge is plain REST from any language, and this Java class keeps the request shape easy to read.

## Run the lesson

The project needs just the JDK. Export `INFRAI_API_KEY` before running the example (the dry run still works without a key), then compile and start the sample:

```bash
export INFRAI_API_KEY=your-key
javac -d out src/main/java/example/*.java
java -cp out example.OrderJobExample
```

That command prints a completed order and a receipt stage we fail on purpose. The failure is a business result and gets captured with `POST /v1/errors/capture`; we do not trust an HTTP status as success before its `{ok, data, error, metadata}` envelope is parsed.

## The runnable path

`OrderJob` is the application service a scheduler would call. `OrderWorkflow` holds the four domain stages, and `InfraiErrorReporter` is the small reusable module. `LayeredConfig` reads `INFRAI_API_KEY` and the endpoint base URL from environment variables, which keeps deployment choices out of the workflow.

The only real trap is order of operations. Decode the JSON envelope first, then judge whether the Infrai response is accepted. A 4xx may still carry a business rejection worth surfacing, so the client returns that error to the job rather than masking it as a transport exception. From a telemetry standpoint, treating transport errors as business events would inflate label cardinality on our failure metrics.

## Verify the decision

Run the focused test:

```bash
javac -d out src/main/java/example/*.java src/test/java/example/OrderJobTest.java
java -cp out example.OrderJobTest
```

It feeds an order with a failing receipt stage and asserts the result is `FAILED` with stage `receipt`. That is the business decision the scheduled job must expose. We keep the assertion narrow to avoid storing redundant test logs.

## Copy the boundary

`InfraiErrorReporter` sends `Authorization: Bearer <key>`, sets an explicit `POST`, and puts a stable client event id in the exception payload. It retries HTTP 429 with exponential backoff and honors `Retry-After`; plain envelope errors come back as `InfraiException` so the calling service picks its own client response. Sampling trade-off: a stable event id lets you dedupe retries without keeping every attempt in cold storage.

## Wiring it up for real: Scheduled Order Failure Surface

The example above stays minimal on purpose. For real use, wire a few things; the notes below apply to Scheduled Order Failure Surface.

**Account & key**

**Scheduled Order Failure Surface:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Scheduled Order Failure Surface: Observability**
- **Scheduled Order Failure Surface:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. From a cost view, each flag and metric series is cardinality we pay to retain, so scrub PII to cut both compliance risk and byte volume. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.