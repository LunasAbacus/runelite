package net.runelite.client.owo;

import com.sun.net.httpserver.HttpServer;
import net.runelite.client.owo.instruction.Command;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OwoServer {
    private HttpServer server;
    private ExecutorService httpExecutor;
    private ObjectMapper objectMapper;

    private Command command = new Command();


    public OwoServer() throws Exception {
        objectMapper = new ObjectMapper();

        // Bind to localhost only
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 8080);
        server = HttpServer.create(addr, 0);

        httpExecutor = Executors.newSingleThreadExecutor();
        server.setExecutor(httpExecutor);

        // GET /status -> {"idle":true,"idleMs":1234}
        server.createContext("/status", exchange -> {
            try
            {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()))
                {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                byte[] bytes = objectMapper.writeValueAsBytes(command);

                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);

                try (OutputStream os = exchange.getResponseBody())
                {
                    os.write(bytes);
                }
            }
            finally
            {
                exchange.close();
            }
        });

        start();
    }

    public void stop() {
        server.stop(0);
    }

    public void start() {
        server.start();
    }

    public void updateCommand(Command command) {
        this.command = command;
    }
}
