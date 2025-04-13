package com.reactor.direct.singlethread;

import java.io.IOException;

public class Main {

  public static final int PORT = 8080;

  public static void main(String[] args) throws IOException {
    runSingleThreadReactor();
  }

  public static void runSingleThreadReactor() throws IOException {
    final Reactor reactor = new Reactor(PORT, NioHandler.class);
    // 启动 Reactor 线程，开始监听 IO 事件
    reactor.startThread();
    reactor.executor.shutdown();
  }
}