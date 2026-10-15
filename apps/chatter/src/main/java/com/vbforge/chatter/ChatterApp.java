package com.vbforge.chatter;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Chatter — LAN chat.
 *
 * This class is the FX boundary. It talks to ChatServer and ChatClient,
 * both of which are plain Java with no toolkit dependency, and marshalls
 * every callback onto the JavaFX Application Thread via Platform.runLater.
 */
public class ChatterApp extends Application {

    private static final int DEFAULT_PORT = 5050;
    private static final String DEFAULT_HOST = "127.0.0.1";

    private TextField nickField;
    private TextField hostField;
    private TextField portField;
    private Button hostBtn;
    private Button connectBtn;
    private Button disconnectBtn;
    private TextArea log;
    private TextField input;
    private Button sendBtn;
    private Label status;

    private ChatServer server;   // non-null only when we're hosting
    private ChatClient client;   // non-null only while connected

    @Override
    public void start(Stage stage) {
        // ---- top bar ----
        nickField = new TextField("user" + (System.currentTimeMillis() % 1000));
        nickField.setPrefColumnCount(8);

        hostField = new TextField(DEFAULT_HOST);
        hostField.setPrefColumnCount(12);

        portField = new TextField(String.valueOf(DEFAULT_PORT));
        portField.setPrefColumnCount(5);

        hostBtn = new Button("Host server");
        hostBtn.setOnAction(e -> startHosting());

        connectBtn = new Button("Connect");
        connectBtn.setOnAction(e -> connect());

        disconnectBtn = new Button("Disconnect");
        disconnectBtn.setDisable(true);
        disconnectBtn.setOnAction(e -> disconnect());

        status = new Label("Not connected.");

        HBox topBar = new HBox(10,
                new Label("Nick"), nickField,
                new Label("Host"), hostField,
                new Label("Port"), portField,
                hostBtn, connectBtn, disconnectBtn,
                spacer(), status);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 12, 0));

        // ---- centre: log ----
        log = new TextArea();
        log.setEditable(false);
        log.setWrapText(true);

        // ---- bottom: compose ----
        input = new TextField();
        input.setPromptText("Type a message and press Enter");
        input.setOnAction(e -> sendMessage());
        HBox.setHgrow(input, Priority.ALWAYS);

        sendBtn = new Button("Send");
        sendBtn.setDisable(true);
        sendBtn.setOnAction(e -> sendMessage());

        HBox bottomBar = new HBox(10, input, sendBtn);
        bottomBar.setPadding(new Insets(12, 0, 0, 0));

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0d1117;");
        root.setPadding(new Insets(16));
        root.setTop(topBar);
        root.setCenter(log);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root, 840, 560);
        scene.getStylesheets().add(
                ChatterApp.class.getResource("/chatter.css").toExternalForm());

        stage.setTitle("Chatter — LAN chat");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> shutdown());
        stage.show();
    }

    // ---------- lifecycle ----------

    private void startHosting() {
        int port = parsePort();
        if (port < 0) return;

        try {
            server = new ChatServer(port, this::appendFromAnyThread);
            server.start();

            // We're always connecting to the server we just started,
            // so force localhost regardless of what's in the host field.
            hostField.setText(DEFAULT_HOST);
            connect();

        } catch (IOException e) {
            appendLine("Could not start server: " + e.getMessage());
            server = null;
        }
    }

    private void connect() {
        if (client != null) return;

        String nick = nickField.getText().trim();
        if (nick.isEmpty()) { appendLine("Nickname required."); return; }

        String host = hostField.getText().trim();
        int port = parsePort();
        if (port < 0) return;

        ChatClient c = new ChatClient(
                host, port, nick,
                this::appendFromAnyThread,
                this::onClientDisconnect);

        try {
            c.connect();
        } catch (IOException e) {
            appendLine("Could not connect: " + e.getMessage());
            if (server != null) { server.stop(); server = null; }
            return;
        }

        client = c;
        setConnected(true);
        appendLine("Connected to " + host + ":" + port + " as " + nick + ".");
    }

    private void disconnect() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
        if (server != null) {
            server.stop();
            server = null;
        }
        setConnected(false);
        appendLine("Disconnected.");
    }

    private void shutdown() {
        disconnect();
        Platform.exit();
    }

    // ---------- actions ----------

    private void sendMessage() {
        if (client == null) return;
        String text = input.getText().trim();
        if (text.isEmpty()) return;

        client.send(text);
        // Don't echo locally — the server broadcasts back to us, and we
        // want to see the message in the same form everyone else does.
        input.clear();
    }

    /** Called from the reader thread when the server drops us. */
    private void onClientDisconnect(String reason) {
        Platform.runLater(() -> {
            appendLine(reason);
            disconnect();
        });
    }

    // ---------- helpers ----------

    private int parsePort() {
        try {
            int p = Integer.parseInt(portField.getText().trim());
            if (p < 1 || p > 65535) throw new NumberFormatException();
            return p;
        } catch (NumberFormatException e) {
            appendLine("Invalid port.");
            return -1;
        }
    }

    private void setConnected(boolean connected) {
        connectBtn.setDisable(connected);
        hostBtn.setDisable(connected);
        disconnectBtn.setDisable(!connected);
        sendBtn.setDisable(!connected);
        status.setText(connected ? "Connected" : "Not connected.");
    }

    /**
     * Called from arbitrary threads (server reader threads, client
     * reader thread). Marshalls onto the FX thread.
     *
     * This is the raw primitive. PixelForge used Task, which wraps
     * this same mechanism — a single background operation whose result
     * we wanted delivered to the FX thread. Here we have N threads and
     * just want to append text, so runLater is the right tool.
     */
    private void appendFromAnyThread(String line) {
        Platform.runLater(() -> appendLine(line));
    }

    private void appendLine(String line) {
        log.appendText(line + "\n");
        log.setScrollTop(Double.MAX_VALUE);
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static void main(String[] args) {
        launch(args);
    }
}