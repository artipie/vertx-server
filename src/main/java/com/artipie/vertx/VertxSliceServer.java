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
import com.artipie.http.rq.RequestLine;
import io.reactivex.Flowable;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.FlowAdapters;

/**
 * Vert.x Slice.
 *
 * @since 0.1
 */
public final class VertxSliceServer implements Closeable {

    /**
     * The Vert.x.
     */
    private final Vertx vertx;

    /**
     * The slice to be served.
     */
    private final Slice served;

    /**
     * The port to start server on.
     */
    private final Integer port;

    /**
     * The Http server.
     */
    private HttpServer server;

    /**
     * An object to sync on.
     */
    private final Object sync;

    /**
     * Ctor.
     *
     * @param served The slice to be served.
     * @param port The port.
     */
    public VertxSliceServer(final Slice served, final Integer port) {
        this(Vertx.vertx(), served, port);
    }

    /**
     * Ctor.
     * @param vertx The vertx.
     * @param served The slice to be served.
     * @param port The port.
     */
    public VertxSliceServer(final Vertx vertx, final Slice served, final Integer port) {
        this.vertx = vertx;
        this.served = served;
        this.port = port;
        this.sync = new Object();
    }

    /**
     * Start the server.
     */
    public void start() {
        synchronized (this.sync) {
            this.server = this.vertx.createHttpServer();
            this.server.requestHandler(this.proxyHandler());
            this.server.rxListen(this.port).blockingGet();
        }
    }

    /**
     * Stop the server.
     */
    public void stop() {
        synchronized (this.sync) {
            this.server.rxClose().blockingAwait();
        }
    }

    @Override
    public void close() {
        this.stop();
    }

    /**
     * A handler which proxy incoming requests to encapsulated slice.
     * @return The request handler.
     */
    private Handler<HttpServerRequest> proxyHandler() {
        return (HttpServerRequest req) -> {
            this.served.response(
                new RequestLine(
                    req.rawMethod(),
                    req.uri(),
                    req.version().toString()
                ).toString(),
                req.headers(),
                FlowAdapters.toFlowPublisher(
                    req.toFlowable().map(buffer -> ByteBuffer.wrap(buffer.getBytes()))
                )
            ).send(
                (code, headers, body) -> {
                    final HttpServerResponse response = req.response().setStatusCode(code);
                    for (final Map.Entry<String, String> header : headers) {
                        response.putHeader(header.getKey(), header.getValue());
                    }
                    Flowable.fromPublisher(FlowAdapters.toPublisher(body)).map(
                        buf -> {
                            final byte[] bytes = new byte[buf.remaining()];
                            buf.get(bytes);
                            return Buffer.buffer(bytes);
                        }).subscribe(new SubscriberFromResponse(response));
                }
            );
        };
    }
}
