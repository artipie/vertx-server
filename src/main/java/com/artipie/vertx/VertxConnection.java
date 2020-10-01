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

import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServerResponse;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Vertx connection accepts Artipie response and send it to {@link HttpServerResponse}.
 * @since 0.2
 */
final class VertxConnection implements Connection {

    /**
     * Vertx server response output.
     */
    private final HttpServerResponse rsp;

    /**
     * New connection for response.
     * @param rsp Response output
     */
    VertxConnection(final HttpServerResponse rsp) {
        this.rsp = rsp;
    }

    @Override
    public CompletionStage<Void> accept(final RsStatus status,
        final Headers headers, final Publisher<ByteBuffer> body) {
        final int code = Integer.parseInt(status.code());
        this.rsp.setStatusCode(code);
        for (final Map.Entry<String, String> header : headers) {
            this.rsp.putHeader(header.getKey(), header.getValue());
        }
        final CompletableFuture<HttpServerResponse> promise = new CompletableFuture<>();
        final Flowable<Buffer> vpb = Flowable.fromPublisher(body)
            .map(VertxConnection::mapBuffer)
            .doOnError(promise::completeExceptionally);
        if (this.rsp.headers().contains("Content-Length")) {
            this.rsp.setChunked(false);
            vpb.doOnComplete(
                    () -> {
                        this.rsp.end();
                        promise.complete(this.rsp);
                    }
                )
                .forEach(this.rsp::write);
        } else {
            this.rsp.setChunked(true);
            vpb.doOnComplete(() -> promise.complete(this.rsp))
                .subscribe(this.rsp.toSubscriber());
        }
        return promise.thenCompose(ignored -> CompletableFuture.allOf());
    }

    /**
     * Map {@link ByteBuffer} to {@link Buffer}.
     * @param buffer Java byte buffer
     * @return Vertx buffer
     */
    private static Buffer mapBuffer(final ByteBuffer buffer) {
        final byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return Buffer.buffer(bytes);
    }
}
