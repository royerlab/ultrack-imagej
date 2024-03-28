/*
 * Copyright (c) 2010-2020 Nathan Rajlich
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 */

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

/**
 * This example demonstrates how to create a websocket connection to a server. Only the most
 * important callbacks are overloaded.
 */
public class UltrackWebsocketClient extends WebSocketClient {

    public final CountDownLatch latch = new CountDownLatch(1);
    public final Consumer<String> onMessage;

    public UltrackWebsocketClient(URI serverUri, Draft draft, Consumer<String> onMessage) {
        super(serverUri, draft);
        this.onMessage = onMessage;
        this.setConnectionLostTimeout(-1);
    }

    public UltrackWebsocketClient(URI serverURI, Consumer<String> onMessage) {
        super(serverURI);
        this.onMessage = onMessage;
        this.setConnectionLostTimeout(-1);
    }

    public UltrackWebsocketClient(URI serverUri, Map<String, String> httpHeaders, Consumer<String> onMessage) {
        super(serverUri, httpHeaders);
        this.onMessage = onMessage;
        this.setConnectionLostTimeout(-1);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        latch.countDown();
    }

    @Override
    public void onMessage(String message) {
        this.onMessage.accept(message);
        //System.out.println("received: " + message);
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        System.out.println("closed " + s);
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("error " + ex.getMessage());
    }

}