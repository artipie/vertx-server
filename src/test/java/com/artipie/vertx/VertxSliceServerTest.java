/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.vertx;

import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Ensure that {@link VertxSliceServer} works correctly.
 * @since 0.1
 */
public class VertxSliceServerTest {

    /**
     * The host to send http requests to.
     */
    private static final String HOST = "localhost";

    @Test
    public void serverHandlesBasicRequest() throws IOException {
        final int port = this.rndPort();
        final Vertx vertx = Vertx.vertx();
        final String uri = "/hello";
        final String expected = "Hello World!";
        try (VertxSliceServer server = new VertxSliceServer(
            vertx,
            (line, headers, body) -> connection -> {
                final int okay = 200;
                connection.accept(okay, headers, body);
            },
            port
        )) {
            server.start();
            final WebClient web = WebClient.create(vertx);
            final String actual = web.post(port, VertxSliceServerTest.HOST, uri)
                .rxSendBuffer(Buffer.buffer(expected.getBytes()))
                .blockingGet()
                .bodyAsString();
            MatcherAssert.assertThat(actual, Matchers.equalTo(expected));
            web.close();
        }
    }

    @Test
    public void basicGetRequest() throws IOException {
        final int port = this.rndPort();
        final Vertx vertx = Vertx.vertx();
        final String uri = "/hello1";
        final String expected = "Hello World!!!";
        try (VertxSliceServer server = new VertxSliceServer(
            vertx,
            (line, headers, body) -> connection -> {
                final int okay = 200;
                connection.accept(
                    okay,
                    Collections.emptyList(),
                    Flowable.fromArray(ByteBuffer.wrap(expected.getBytes()))
                );
            },
            port
        )) {
            server.start();
            final WebClient web = WebClient.create(vertx);
            final String actual = web.get(port, VertxSliceServerTest.HOST, uri)
                .rxSend()
                .blockingGet()
                .bodyAsString();
            MatcherAssert.assertThat(actual, Matchers.equalTo(expected));
            web.close();
        }
    }

    /**
     * Find a random port.
     * @return The free port.
     * @throws IOException If fails.
     */
    private int rndPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
