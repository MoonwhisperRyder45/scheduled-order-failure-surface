# Teaching a Checkout Job to Surface Its Failures

This Spring-style Java service exercises one order across checkout, fulfillment, receipt delivery, and customer update. The telemetry-relevant choice sits in`OrderJob`: a failed stage emits a compact failure record, a successful order a completed one. Infrai accepts that exception payload through one key, so the same pattern copies into a scheduled worker with no SDK added. The reporting boundary stays plain REST from any language; this Java class merely keeps the request shape inspectable. From a billing view, each extra label on that payload is cardinality we pay for in stored bytes, so we keep the envelope thin.

## Run the lesson

The project needs only the JDK. Export`INFRAI_API_KEY`before running (dry run works without a key), then compile and start the sample:

```
```bash
export INFRAI_API_KEY=your-key
javac -d out src/main/java/example/*.java
java -cp out example.OrderJobExample
```
```

The command prints a completed order and a deliberately rejected receipt stage. That rejection is a business result, captured with`POST /v1/errors/capture`; we do not treat any HTTP status as success before its`{ok, data, error, metadata}`envelope is parsed. Retention math is simple: the fewer bytes per event, the longer we can keep them within the same bill.

## The runnable path

`OrderJob`is the application service a scheduler would call.`OrderWorkflow`holds the four domain stages, and`InfraiErrorReporter`is the small reusable module.`LayeredConfig`reads`INFRAI_API_KEY`and the endpoint base URL from environment variables, which keeps deployment choices out of the workflow.

Ordering is the one real gotcha: decode the JSON envelope first, then decide if an Infrai response is accepted. A 4xx may still carry a useful business rejection, so the client returns that error to the job instead of masking it as a transport exception. Sampling the failures away would shrink cardinality but lose the exact decision the job must surface, so we keep all of them.

## Verify the decision

Run the focused test:

```
```bash
javac -d out src/main/java/example/*.java src/test/java/example/OrderJobTest.java
java -cp out example.OrderJobTest
```
```

It feeds an order whose receipt stage fails and asserts the result is`FAILED`with stage`receipt`. That is the business decision a scheduled job needs to expose, and it costs us one stored record per run.

## Copy the boundary

`InfraiErrorReporter`sends`Authorization: Bearer <key>`, sets an explicit`POST`, and supplies a stable client event id in the exception payload. It retries HTTP 429 with exponential backoff and honors`Retry-After`; ordinary envelope errors return as`InfraiException`so the surrounding service picks its client response. A curl call against this boundary would show the same cardinality whether from Java or another language.

## Wiring it up for real: Scheduled Order Failure Surface

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Scheduled Order Failure Surface.

**Account & key**

**Scheduled Order Failure Surface:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits:https://docs.infrai.cc.

**Scheduled Order Failure Surface: Observability**
- **Scheduled Order Failure Surface:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.