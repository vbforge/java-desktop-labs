package com.vbforge.chatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A minimal TCP chat server.
 *
 * Threading model:
 *   - One accept thread loops on ServerSocket.accept().
 *   - Each accepted connection spawns its own reader thread.
 *   - Broadcasts come from reader threads, so writes to any given client
 *     are serialised with a per-handler lock.
 *
 * Deliberately has no JavaFX dependency. The log callback is a plain
 * Consumer, so this class is testable without a toolkit.
 */
public final class ChatServer {

    private final int port;
    private final Consumer<String> log;

    // Reads are frequent (every broadcast), writes are rare
    // (connect / disconnect). CopyOnWriteArrayList makes broadcast
    // lock-free at the cost of O(n) copies on mutation — exactly right.
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    private ServerSocket serverSocket;
    private volatile boolean running;

    public ChatServer(int port, Consumer<String> log) {
        this.port = port;
        this.log = log;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;

        Thread t = new Thread(this::acceptLoop, "chatter-accept");
        t.setDaemon(true);
        t.start();

        log.accept("Server listening on port " + port);
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); }
        catch (IOException ignored) { }

        for (ClientHandler h : clients) h.close();
        clients.clear();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket s = serverSocket.accept();
                ClientHandler h = new ClientHandler(s);
                clients.add(h);

                Thread t = new Thread(h::run, "chatter-client-" + s.getPort());
                t.setDaemon(true);
                t.start();

            } catch (IOException e) {
                // If we're shutting down, accept() throws because we closed
                // the ServerSocket. That's expected — don't log it.
                if (running) log.accept("Accept failed: " + e.getMessage());
            }
        }
    }

    private void broadcast(String line) {
        for (ClientHandler h : clients) h.send(line);
    }

    // -----------------------------------------------------------------

    private final class ClientHandler {
        private final Socket socket;
        private PrintWriter out;
        private String nickname;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        void run() {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

                String line;
                while ((line = in.readLine()) != null) {
                    handle(line);
                }
            } catch (IOException ignored) {
                // Peer died — normal.
            } finally {
                disconnect();
            }
        }

        private void handle(String line) {
            if (nickname == null) {
                // First line must be HELLO <nick>. Anything else → ignore.
                if (line.startsWith("HELLO ")) {
                    String n = line.substring(6).trim();
                    nickname = n.isEmpty() ? "anon" : n;
                    broadcast("* " + nickname + " joined");
                    log.accept(nickname + " joined from "
                            + socket.getInetAddress().getHostAddress());
                }
                return;
            }
            broadcast(nickname + ": " + line);
        }

        /**
         * Serialised write. Multiple reader threads call this concurrently,
         * and PrintWriter is not thread-safe.
         */
        synchronized void send(String line) {
            if (out != null && !socket.isClosed()) {
                out.println(line);
            }
        }

        void disconnect() {
            boolean wasKnown = nickname != null;
            String who = nickname;
            nickname = null;

            // Remove before broadcasting so we don't try to write to
            // our own (already-dead) socket.
            clients.remove(this);

            if (wasKnown) {
                broadcast("* " + who + " left");
                log.accept(who + " left");
            }
            close();
        }

        void close() {
            try { if (!socket.isClosed()) socket.close(); }
            catch (IOException ignored) { }
        }
    }
}