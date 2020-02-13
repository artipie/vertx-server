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

import com.jcabi.log.Logger;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServerResponse;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Wrap {@link HttpServerResponse} to behave like a {@link Subscriber}.
 *
 * @since 0.1
 */
final class SubscriberFromResponse implements Subscriber<Buffer> {

    /**
     * The wrapped response.
     */
    private final HttpServerResponse response;

    /**
     * Ctor.
     * @param response The response.
     */
    SubscriberFromResponse(final HttpServerResponse response) {
        this.response = response;
    }

    @Override
    public void onSubscribe(final Subscription subscription) {
        this.response.setChunked(true);
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(final Buffer buffer) {
        this.response.write(buffer);
    }

    @Override
    public void onError(final Throwable thr) {
        Logger.error(SubscriberFromResponse.class, "Error occurred: %s", thr.getMessage());
        final int internal = 500;
        this.response.setStatusCode(internal).end();
    }

    @Override
    public void onComplete() {
        this.response.end();
    }
}
