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

import org.junit.jupiter.api.Test;

import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UltrackConnectorTest {

    @Test
    void isPortOpen_closedPort_returnsFalse() {
        // Port 1 is reserved and never open in practice; use a very short timeout.
        assertFalse(UltrackConnector.isPortOpen("127.0.0.1", 1, 100));
    }

    @Test
    void isPortOpen_openPort_returnsTrue() throws Exception {
        // Bind a real ServerSocket so the port is definitely open.
        try (ServerSocket server = new ServerSocket(0)) {
            int port = server.getLocalPort();
            assertTrue(UltrackConnector.isPortOpen("127.0.0.1", port, 500));
        }
    }

    @Test
    void startServer_nullPath_doesNotSetPort() {
        // When ultrackPath is null startServer() must return early without
        // assigning a port (port stays -1).  The error dialog is dispatched to
        // Swing EDT asynchronously so the test doesn't need a display.
        UltrackConnector connector = new UltrackConnector(null, line -> {}) {
            @Override
            public void onExecutionErrorAction() {}
        };
        connector.startServer();
        // Port must remain -1 — callers (JavaConnector.startUltrackServer) guard
        // against this to avoid passing -1 to the JS polling loop.
        assertEquals(-1, connector.getPort(), "Port should stay -1 when ultrackPath is null");
    }

    @Test
    void startServer_emptyPath_doesNotSetPort() {
        UltrackConnector connector = new UltrackConnector("", line -> {}) {
            @Override
            public void onExecutionErrorAction() {}
        };
        connector.startServer();
        assertEquals(-1, connector.getPort(), "Port should stay -1 when ultrackPath is empty");
    }
}
