/*-
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
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public class UltrackConnector {
    private final String ultrackPath;
    private Process currentProcess = null;
    private int port = -1;

    public UltrackConnector(String ultrackPath) {
        this.ultrackPath = ultrackPath;
    }

    public void stopServer() {
        if (currentProcess != null) {
            currentProcess.destroy();
        }
    }

    public int getPort() {
        return port;
    }

    public void startServer() {
        int randomPort;
        do {
            randomPort = (int) (Math.random() * 50000 + 10000);
        } while (isPortOpen("127.0.0.1", randomPort, 300));
        System.out.println(randomPort);
        port = randomPort;

        SwingUtilities.invokeLater(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder();
                // Example command that lists directory content. For Windows, you might use "cmd", "/c", "dir".
                processBuilder.command(ultrackPath, "server", "--port", String.valueOf(port));
                processBuilder.redirectErrorStream(true);

                try {
                    currentProcess = processBuilder.start();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }


                // wait for the server to end

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            new Thread(() -> {
                try {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println("Server said: " + line);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        });

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

    public void connectToWebsocket(String url, String message, Consumer<String> onMessage) {
        UltrackWebsocketClient c;
        try {
            c = new UltrackWebsocketClient(
                    new URI("ws://localhost:" + port + url),
                    onMessage
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            System.out.println("Error connecting to the websocket: " + e.getMessage());
            return;
        }
        c.connect();
        CountDownLatch latch = c.latch;
        try {
            latch.await();
            System.out.println(message);
            c.send(message);
        } catch (InterruptedException e) {
            JOptionPane.showMessageDialog(null, "Error connecting to the websocket: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
