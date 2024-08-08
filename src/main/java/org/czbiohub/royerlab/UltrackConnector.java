package org.czbiohub.royerlab;/*-
 * #%L
 * Ultrack: Large-Scale Multi-Hypotheses Cell Tracking Using Ultrametric Contours Maps.
 * %%
 * Copyright (C) 2010 - 2024 RoyerLab.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public abstract class UltrackConnector {
    private final String ultrackPath;
    private final Consumer<String> onLog;
    private Process currentProcess = null;
    private int port = -1;
    private Thread serverListenerThread;
    private volatile boolean interruptionRequested = false;

    public UltrackConnector(String ultrackPath, Consumer<String> onLog) {
        this.ultrackPath = ultrackPath;
        this.onLog = onLog;
    }

    public static boolean isPortOpen(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), timeout);
            return true; // Connection successful, port is open
        } catch (IOException e) {
            // Either timeout or unreachable or failed to connect.
            return false; // Port is closed or not reachable
        }
    }

    public void stopServer() {
        if (currentProcess != null) {
            if (serverListenerThread != null) {
                interruptionRequested = true;
                serverListenerThread.interrupt();
            }
            currentProcess.destroy();
            currentProcess = null;
            try {
                serverListenerThread.join();
                serverListenerThread = null;
            } catch (Exception e) {
                System.out.println("Error waiting for the server to end: " + e.getMessage());
                throw new RuntimeException(e);
            }
            interruptionRequested = false;
        }
    }

    private void onExecutionError(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
        onExecutionErrorAction();
    }

    public abstract void onExecutionErrorAction();

    public int getPort() {
        return port;
    }

    private String loadTemporaryResource(String resourceName) {
        String targetPath = System.getProperty("java.io.tmpdir") + File.separator + resourceName;
        try (InputStream is = getClass().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + resourceName);
            }
            try (OutputStream os = new FileOutputStream(targetPath)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return targetPath;
    }

    public void startServer() {
        int randomPort;
        do {
            randomPort = (int) (Math.random() * 50000 + 10000);
        } while (isPortOpen("127.0.0.1", randomPort, 300));
        System.out.println(randomPort);
        port = randomPort;

        new Thread(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder();
                // Example command that lists directory content. For Windows, you might use "cmd", "/c", "dir".
                String resource_path = loadTemporaryResource("/config.toml");
                processBuilder.command(ultrackPath, "server", "--port", String.valueOf(port), "--config", resource_path);
                processBuilder.redirectErrorStream(true);

                try {
                    currentProcess = processBuilder.start();
                    System.out.println("Server started");
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> onExecutionError("Error starting the server: " + ex.getMessage()));
                    return;
                }

                // wait for the server to end

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            this.serverListenerThread = new Thread(() -> {
                try {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            this.onLog.accept(line);
                        }
                    }

                    int exitCode = currentProcess.waitFor();
                    if (exitCode != 0) {
                        SwingUtilities.invokeLater(() -> onExecutionError("Server ended with exit code " + exitCode));
                    }
                } catch (IOException e) {
                    if (!this.interruptionRequested) {
                        SwingUtilities.invokeLater(() -> onExecutionError("Error reading the server output: " + e.getMessage()));
                    }
                } catch (InterruptedException e) {
                    if (!this.interruptionRequested) {
                        throw new RuntimeException(e);
                    }
                }
            });
            this.serverListenerThread.start();
        }).start();

    }

    public void connectToWebsocket(String url, String message, Consumer<String> onMessage, Consumer<String> onError, Consumer<String> onClose) {
        UltrackWebsocketClient c;
        try {
            c = new UltrackWebsocketClient(
                    new URI("ws://localhost:" + port + url),
                    onMessage,
                    onError,
                    onClose
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            onExecutionError("Error connecting to the websocket: " + e.getMessage());
            return;
        }
        c.connect();
        CountDownLatch latch = c.latch;
        try {
            latch.await();
            System.out.println(message);
            c.send(message);
        } catch (InterruptedException e) {
            onExecutionError("Error connecting to the websocket: " + e.getMessage());
        }
    }
}
