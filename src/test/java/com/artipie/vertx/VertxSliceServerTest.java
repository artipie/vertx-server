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

import com.artipie.http.Slice;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Ensure that {@link VertxSliceServer} works correctly.
 *
 * @since 0.1
 */
public final class VertxSliceServerTest {

    /**
     * The host to send http requests to.
     */
    private static final String HOST = "localhost";

    /**
     * Server port.
     */
    private int port;

    /**
     * Vertx instance used in server and client.
     */
    private Vertx vertx;

    /**
     * HTTP client used to send requests to server.
     */
    private WebClient client;

    /**
     * Server instance being tested.
     */
    private VertxSliceServer server;

    @BeforeEach
    public void setUp() throws Exception {
        this.port = this.rndPort();
        this.vertx = Vertx.vertx();
        this.client = WebClient.create(this.vertx);
    }

    @AfterEach
    public void tearDown() {
        if (this.server != null) {
            this.server.close();
        }
        if (this.client != null) {
            this.client.close();
        }
        if (this.vertx != null) {
            this.vertx.close();
        }
    }

    @Test
    public void serverHandlesBasicRequest() {
        this.start(
            (line, headers, body) -> connection -> connection.accept(RsStatus.OK, headers, body)
        );
        final String expected = "Hello World!";
        final String actual = this.client.post(this.port, VertxSliceServerTest.HOST, "/hello")
            .rxSendBuffer(Buffer.buffer(expected.getBytes()))
            .blockingGet()
            .bodyAsString();
        MatcherAssert.assertThat(actual, Matchers.equalTo(expected));
    }

    @Test
    public void basicGetRequest() {
        final String expected = "Hello World!!!";
        this.start(
            (line, headers, body) -> connection -> connection.accept(
                RsStatus.OK,
                Collections.emptyList(),
                Flowable.fromArray(ByteBuffer.wrap(expected.getBytes()))
            )
        );
        final String actual = this.client.get(this.port, VertxSliceServerTest.HOST, "/hello1")
            .rxSend()
            .blockingGet()
            .bodyAsString();
        MatcherAssert.assertThat(actual, Matchers.equalTo(expected));
    }

    @Test
    public void exceptionInSlice() {
        this.start(
            (line, headers, body) -> {
                throw new IllegalStateException("Failed to create response");
            }
        );
        final int code = this.client.get(this.port, VertxSliceServerTest.HOST, "")
            .rxSend()
            .blockingGet()
            .statusCode();
        MatcherAssert.assertThat(
            code,
            Matchers.equalTo(HttpURLConnection.HTTP_INTERNAL_ERROR)
        );
    }

    @Test
    public void exceptionInResponse() {
        this.start(
            (line, headers, body) -> connection -> {
                throw new IllegalStateException("Failed to send response");
            }
        );
        final int code = this.client.get(this.port, VertxSliceServerTest.HOST, "")
            .rxSend()
            .blockingGet()
            .statusCode();
        MatcherAssert.assertThat(
            code,
            Matchers.equalTo(HttpURLConnection.HTTP_INTERNAL_ERROR)
        );
    }

    @Test
    public void exceptionInResponseAsync() {
        this.start(
            (line, headers, body) -> connection -> CompletableFuture.runAsync(
                () -> {
                    throw new IllegalStateException("Failed to send response async");
                }
            )
        );
        final int code = this.client.get(this.port, VertxSliceServerTest.HOST, "")
            .rxSend()
            .blockingGet()
            .statusCode();
        MatcherAssert.assertThat(
            code,
            Matchers.equalTo(HttpURLConnection.HTTP_INTERNAL_ERROR)
        );
    }

    @Test
    public void exceptionInBody() {
        this.start(
            (line, headers, body) -> connection -> connection.accept(
                RsStatus.OK,
                Collections.emptyList(),
                Flowable.error(new IllegalStateException("Failed to publish body"))
            )
        );
        final int code = this.client.get(this.port, VertxSliceServerTest.HOST, "")
            .rxSend()
            .blockingGet()
            .statusCode();
        MatcherAssert.assertThat(
            code,
            Matchers.equalTo(HttpURLConnection.HTTP_INTERNAL_ERROR)
        );
    }

    private void start(final Slice slice) {
        final VertxSliceServer srv = new VertxSliceServer(this.vertx, slice, this.port);
        srv.start();
        this.server = srv;
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
