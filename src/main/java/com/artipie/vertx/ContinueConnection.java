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
import io.vertx.reactivex.core.http.HttpServerResponse;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Connection which supports {@code 100 Continue} status code responses.
 * <p>
 * It sends continue status to client and then delegates other work to origin connection.
 * </p>
 * @since 0.2
 */
final class ContinueConnection implements Connection {

    /**
     * Vertx response output.
     */
    private final HttpServerResponse response;

    /**
     * Origin HTTP connection.
     */
    private final Connection origin;

    /**
     * Wraps origin connection with continue responses support.
     * @param response Vertx response output
     * @param origin Origin connection
     */
    ContinueConnection(final HttpServerResponse response, final Connection origin) {
        this.response = response;
        this.origin = origin;
    }

    @Override
    public CompletionStage<Void> accept(final RsStatus status, final Headers headers,
        final Publisher<ByteBuffer> body) {
        final CompletionStage<Void> res;
        if (status == RsStatus.CONTINUE) {
            this.response.writeContinue();
            res = CompletableFuture.completedFuture(null);
        } else {
            res = this.origin.accept(status, headers, body);
        }
        return res;
    }
}
