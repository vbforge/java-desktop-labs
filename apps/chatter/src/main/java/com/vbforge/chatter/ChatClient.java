package com.vbforge.chatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * A single TCP client connection.
 *
 * The reader thread calls onLine for every inbound line. It runs on the
 * reader thread, NOT the FX thread — the UI layer is responsible for
 * marshalling. Kept FX-free on purpose so this class is testable alone.
 */
public final class ChatClient {

    private final String host;
    private final int port;
    private final String nickname;
    private final Consumer<String> onLine;
    private final Consumer<String> onDisconnect;

    private Socket socket;
    private PrintWriter out;
    private volatile boolean running;

    public ChatClient(String host, int port, String nickname,
                      Consumer<String> onLine,
                      Consumer<String> onDisconnect) {
        this.host = host;
        this.port = port;
        this.nickname = nickname;
        this.onLine = onLine;
        this.onDisconnect = onDisconnect;
    }

    /** Blocking. Called from the FX thread in this app — see notes below. */
    public void connect() throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

        // Bootstraps the protocol: server treats the first line as HELLO.
        out.println("HELLO " + nickname);

        running = true;
        Thread t = new Thread(this::readLoop, "chatter-reader");
        t.setDaemon(true);
        t.start();
    }

    public void send(String text) {
        if (out != null) out.println(text);
    }

    public void disconnect() {
        running = false;   // set BEFORE closing so readLoop's finally sees it
        try { if (socket != null && !socket.isClosed()) socket.close(); }
        catch (IOException ignored) { }
    }

    private void readLoop() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = in.readLine()) != null) {
                onLine.accept(line);
            }
        } catch (IOException ignored) {
            // Expected when we close the socket ourselves.
        } finally {
            // If running is still true, the server dropped us — that's
            // worth telling the user about. If we called disconnect()
            // ourselves, running is false and we stay quiet.
            if (running) onDisconnect.accept("Connection closed by server.");
        }
    }
}