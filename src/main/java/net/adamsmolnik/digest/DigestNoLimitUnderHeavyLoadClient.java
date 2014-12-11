package net.adamsmolnik.digest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.adamsmolnik.client.SimpleJsonDigestNoLimitClient;

/**
 * @author ASmolnik
 *
 */
public class DigestNoLimitUnderHeavyLoadClient implements AutoCloseable {

    public static class ProgressEvent {

        public final int submitted;

        public final int succeeded;

        public final int failed;

        public final boolean completed;

        public final String result;

        public ProgressEvent(int submitted, int succeeded, int failed, String result) {
            this.submitted = submitted;
            this.succeeded = succeeded;
            this.failed = failed;
            this.result = result;
            this.completed = false;
        }

        public ProgressEvent(boolean completed, int submitted, int succeeded, int failed) {
            this.submitted = submitted;
            this.succeeded = succeeded;
            this.failed = failed;
            this.result = "Finished";
            this.completed = completed;
        }

    }

    public static class Builder {

        private String host;

        private String algorithm = "SHA-256";

        private String objectKey;

        private int workersNumber = 2 * Runtime.getRuntime().availableProcessors();

        private int requestsNumber = 1000;

        private int suspensionInMs = 300;

        public Builder(String host, String objectKey) {
            this.host = host;
            this.objectKey = objectKey;
        }

        public Builder algorithm(String val) {
            algorithm = val;
            return this;
        }

        public Builder workersNumber(int val) {
            workersNumber = val;
            return this;
        }

        public Builder suspensionInMs(int val) {
            suspensionInMs = val;
            return this;
        }

        public Builder requestsNumber(int val) {
            requestsNumber = val;
            return this;
        }

        public DigestNoLimitUnderHeavyLoadClient build() {
            return new DigestNoLimitUnderHeavyLoadClient(this);
        }

    }

    private final String host;

    private final String algorithm;

    private final String objectKey;

    private final int workersNumber;

    private final int requestsNumber;

    private final int suspensionInMs;

    private final ExecutorService workers;

    private final ExecutorService launchers;

    private final AtomicBoolean stop = new AtomicBoolean();

    private final SimpleJsonDigestNoLimitClient client;

    private DigestNoLimitUnderHeavyLoadClient(Builder builder) {
        host = builder.host.trim();
        algorithm = builder.algorithm;
        objectKey = builder.objectKey;
        requestsNumber = builder.requestsNumber;
        workersNumber = builder.workersNumber;
        suspensionInMs = builder.suspensionInMs;
        workers = Executors.newFixedThreadPool(workersNumber);
        launchers = Executors.newCachedThreadPool();
        client = new SimpleJsonDigestNoLimitClient(host);
    }

    public final void send(Optional<Consumer<ProgressEvent>> progressEventConsumer) {
        launchers.submit(() -> {
            final AtomicInteger submitted = new AtomicInteger();
            final AtomicInteger succeeded = new AtomicInteger();
            final AtomicInteger failed = new AtomicInteger();

            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < requestsNumber; i++) {
                if (workers.isShutdown() || stop.get()) {
                    break;
                }
                futures.add(workers.submit(() -> {
                    if (stop.get()) {
                        return;
                    }
                    int seq = submitted.incrementAndGet();
                    final AtomicReference<String> responseMessage = new AtomicReference<>();
                    try {
                        System.out.println("before sending (seq " + seq + ")");
                        String response = client.send(algorithm, objectKey, 15);
                        System.out.println("after sending (seq " + seq + ")");
                        responseMessage.set(response);
                        succeeded.incrementAndGet();
                        TimeUnit.MILLISECONDS.sleep(suspensionInMs);
                    } catch (InterruptedException iex) {
                        // deliberately ignored
                        Thread.currentThread().interrupt();
                    } catch (Exception ex) {
                        responseMessage.set(ex.getClass() + ": " + ex.getLocalizedMessage());
                        failed.incrementAndGet();
                        ex.printStackTrace();
                    } finally {
                        progressEventConsumer.ifPresent(consumer -> consumer.accept(new ProgressEvent(seq, succeeded.get(), failed.get(),
                                responseMessage.get())));
                    }
                }));

            };
            futures.forEach(f -> {
                try {
                    f.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            });
            progressEventConsumer.ifPresent(consumer -> consumer.accept(new ProgressEvent(true, submitted.get(), succeeded.get(), failed.get())));

        });
    }

    public final void send() {
        send(Optional.empty());
    }

    @Override
    public final void close() {
        stop.set(true);
        workers.shutdown();
        launchers.shutdown();
    }

    @Override
    public String toString() {
        return "DigestNoLimitUnderHeavyLoadClient [host=" + host + ", algorithm=" + algorithm + ", objectKey=" + objectKey + ", workersNumber="
                + workersNumber + ", requestsNumber=" + requestsNumber + ", suspensionInMs=" + suspensionInMs + ", stop=" + stop + "]";
    }

}
