package net.adamsmolnik.ws;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import net.adamsmolnik.digest.DigestNoLimitUnderHeavyLoadClient;

/**
 * @author ASmolnik
 *
 */
@ServerEndpoint("/ws")
public class WsServer {

    private String localHost = "unknown";

    {
        try {
            localHost = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    private ConcurrentMap<String, DigestNoLimitUnderHeavyLoadClient> clientMap = new ConcurrentHashMap<>();

    @OnOpen
    public void open(Session session) {
        System.out.println("Session is coming on " + session);
    }

    @OnMessage
    public void handleMessage(String message, Session session) {
        System.out.println("Message received: " + message);
        String data[] = message.split(";");
        String command = data[0];
        if ("launch".equals(command)) {
            launch(session, data);
        } else if ("stop".equals(command)) {
            System.out.println("Stopping...");
            close(session.getId());
        }
    }

    @OnClose
    public void close(Session session) {
        System.out.println("Closing...");
        close(session.getId());
    }

    private void close(String sessionId) {
        DigestNoLimitUnderHeavyLoadClient client = clientMap.remove(sessionId);
        closeQuietly(client);
    }

    private void closeQuietly(DigestNoLimitUnderHeavyLoadClient client) {
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception ex) {
            // deliberately ignored
        }
    }

    private void launch(Session session, String[] data) {
        final String destHost = data[1];
        final String objectKey = data[2];
        final int requestsNumber = Integer.valueOf(data[3]);
        final int suspensionInMs = Integer.valueOf(data[4]);
        final int workersNumber = Integer.valueOf(data[5]);
        final String sessionId = session.getId();
        DigestNoLimitUnderHeavyLoadClient.Builder builder = new DigestNoLimitUnderHeavyLoadClient.Builder(destHost, objectKey)
                .requestsNumber(requestsNumber).suspensionInMs(suspensionInMs).workersNumber(workersNumber);
        DigestNoLimitUnderHeavyLoadClient client = null;
        try {
            client = clientMap.computeIfAbsent(sessionId, key -> builder.build());
            client.send(Optional.of(progressEvent -> {
                try {
                    if (progressEvent.completed) {
                        session.getBasicRemote().sendText("Completed");
                        session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Completed"));
                        return;
                    }
                    session.getBasicRemote().sendText(
                            "Submitted " + progressEvent.submitted + ", succeeded " + progressEvent.succeeded + ", failed " + progressEvent.failed
                                    + ", sent from " + localHost);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    try {
                        session.getBasicRemote().sendText(ex.getLocalizedMessage());
                    } catch (IOException ioex) {
                        ioex.printStackTrace();
                    }
                }
            }));
        } catch (Exception ex) {
            try {
                ex.printStackTrace();
                closeQuietly(client);
                clientMap.remove(sessionId);
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, ex.getLocalizedMessage()));
            } catch (IOException ioex) {
                ioex.printStackTrace();
            }
        }
    }
}
