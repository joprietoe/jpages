/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2018-2019 Yegor Bugayenko
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
package org.elegantobjects.jpages;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The app.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @since 0.1
 */
public final class App {

    private final Page page;

    public App(final Page pge) {
        this.page = pge;
    }

    public void start(final int port) throws IOException, InterruptedException {
        final List<Thread> pool = new ArrayList<>(0);
        try (final ServerSocket server = new ServerSocket(port)) {
            server.setSoTimeout(1000);
            for (int i = 0; i < 10; ++i) {
                final Thread t = new Thread(
                    () -> {
                        try {
                            while (true) {
                                if (Thread.currentThread().isInterrupted()) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                                try (final Socket socket = server.accept()) {
                                    this.process(socket);
                                } catch (final SocketTimeoutException ex) {
                                    continue;
                                }
                            }
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                );
                pool.add(t);
            }
            for (int i = 0; i < pool.size(); ++i) {
                pool.get(i).start();
            }
            for (int i = 0; i < pool.size(); ++i) {
                pool.get(i).join();
            }
        }
    }

    private void process(final Socket socket) throws IOException {
        try (final InputStream input = socket.getInputStream();
             final OutputStream output = socket.getOutputStream()) {
            final byte[] buffer = new byte[10000];
            final int total = input.read(buffer);
            new Session(this.page).with(
                new String(Arrays.copyOfRange(buffer, 0, total))
            ).via(new SimpleOutput("")).writeTo(output);
        }
    }

}
