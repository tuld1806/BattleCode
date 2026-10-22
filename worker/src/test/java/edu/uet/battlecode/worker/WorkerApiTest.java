package edu.uet.battlecode.worker;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class WorkerApiTest {
    @Test void claimSendsWorkerKeyAndReadsMatchId() throws Exception {
        UUID match = UUID.randomUUID();
        AtomicReference<String> supplied = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/worker/claim", exchange -> {
            supplied.set(exchange.getRequestHeaders().getFirst("X-Worker-Key"));
            byte[] response = ("{\"id\":\"" + match + "\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            WorkerApi api = new WorkerApi(URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                    "worker-secret", "worker-a");
            assertEquals(match, api.claim().orElseThrow());
            assertEquals("worker-secret", supplied.get());
        } finally { server.stop(0); }
    }
}
