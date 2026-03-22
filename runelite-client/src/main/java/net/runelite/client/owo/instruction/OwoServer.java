package net.runelite.client.owo.instruction;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.runelite.client.plugins.owo.OwoPlugin;

@Slf4j
public class OwoServer {
    private HttpServer server;
    private ExecutorService httpExecutor;
    private ObjectMapper objectMapper;

    private Command command = InstructionFactory.createDefaultIdle();

    private final String token;
    private final String channelID;


    public OwoServer(OwoPlugin plugin) throws Exception {
        objectMapper = new ObjectMapper();

        token = System.getenv("DISCORD_TOKEN");
        channelID = System.getenv("DISCORD_CHANNEL_ID");
        String guildId = System.getenv("DISCORD_GUILD_ID");

        // Bind to localhost only
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 8081);
        server = HttpServer.create(addr, 0);

        httpExecutor = Executors.newSingleThreadExecutor();
        server.setExecutor(httpExecutor);

        // GET /status -> {"idle":true,"idleMs":1234}
        server.createContext("/status", exchange -> {
            try {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                byte[] bytes = objectMapper.writeValueAsBytes(command);

                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } finally {
                exchange.close();
            }
        });

        JDA jda = JDABuilder.createDefault(token).build();
        jda.awaitReady();

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalStateException("Guild not found");
        }

        // TODO Add listener for commands like !stop
        guild.upsertCommand("ping", "Replies with pong").queue();

        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
                if (event.getName().equals("ping")) {
                    log.debug("Received ping event");
                    event.reply("pong").queue();
                }
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

    public void updateCommand(final Command command) {
        this.command = command;
    }

    public void postDiscordMessage(final String message) {
        log.debug("Sending message to Discord: {}", message);
        log.debug("Token: {}", token);
        log.debug("ChannelID: {}", channelID);
        String payload = String.format("{\"content\":\"%s\"}", message);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/v10/channels/" + channelID + "/messages"))
                .header("Authorization", "Bot " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("Status code: {}", response.statusCode());
            log.debug("Response: {}", response.body());
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }
}
