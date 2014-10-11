package net.adamsmolnik.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import net.adamsmolnik.model.digest.DigestRequest;
import net.adamsmolnik.model.digest.DigestResponse;

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

        public ProgressEvent(int submitted, int succeeded, int failed) {
            this.submitted = submitted;
            this.succeeded = succeeded;
            this.failed = failed;
            this.completed = false;
        }

        public ProgressEvent(boolean completed, int submitted, int succeeded, int failed) {
            this.submitted = submitted;
            this.succeeded = succeeded;
            this.failed = failed;
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

    private final Client client = ClientBuilder.newClient();

    private final ExecutorService workers;

    private final ExecutorService launchers;

    private final AtomicBoolean stop = new AtomicBoolean();

    private DigestNoLimitUnderHeavyLoadClient(Builder builder) {
        host = builder.host;
        algorithm = builder.algorithm;
        objectKey = builder.objectKey;
        requestsNumber = builder.requestsNumber;
        workersNumber = builder.workersNumber;
        suspensionInMs = builder.suspensionInMs;
        workers = Executors.newFixedThreadPool(workersNumber);
        launchers = Executors.newCachedThreadPool();
    }

    public final void send(Optional<Consumer<ProgressEvent>> progressEventConsumer) {
        launchers
                .submit(() -> {
                    final Entity<DigestRequest> request = Entity.json(new DigestRequest(algorithm, objectKey));
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

                            try {
                                submitted.incrementAndGet();
                                System.out.println("before sending (submitted " + submitted.get() + ")");
                                Response response = client.target("http://" + host + "/digest-service-no-limit/ds/digest").request().post(request);
                                System.out.println("after sending (submitted " + submitted.get() + ")");
                                response.readEntity(DigestResponse.class);
                                succeeded.incrementAndGet();
                                TimeUnit.MILLISECONDS.sleep(suspensionInMs);
                            } catch (InterruptedException iex) {
                                // deliberately ignored
                            } catch (Exception iex) {
                                failed.incrementAndGet();
                                iex.printStackTrace();
                            } finally {
                                progressEventConsumer.ifPresent(consumer -> consumer.accept(new ProgressEvent(submitted.get(), succeeded.get(),
                                        failed.get())));
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
                    progressEventConsumer.ifPresent(consumer -> consumer.accept(new ProgressEvent(true, submitted.get(), succeeded.get(), failed
                            .get())));

                });
    }

    public final void send() {
        send(Optional.empty());
    }

    @Override
    public final void close() {
        stop.set(true);
        workers.shutdownNow();
        launchers.shutdownNow();
    }

    @Override
    public String toString() {
        return "DigestNoLimitUnderHeavyLoadClient [host=" + host + ", algorithm=" + algorithm + ", objectKey=" + objectKey + ", workersNumber="
                + workersNumber + ", requestsNumber=" + requestsNumber + ", suspensionInMs=" + suspensionInMs + ", stop=" + stop + "]";
    }

}
