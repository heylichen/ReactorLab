package com.reactor.direct.singlereactor;

import com.reactor.direct.singlethread.Reactor;

import java.io.IOException;

public class Main {
  public static final int PORT = 8080;

  public static void main(String[] args) throws IOException {
    runMultiThreadReactor();
  }

  public static void runMultiThreadReactor() throws IOException {
    final Reactor reactor = new Reactor(PORT, MultiThreadNioHandler.class);

    reactor.startThread();
    reactor.executor.shutdown();
  }
}