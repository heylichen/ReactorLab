package com.reactor.v2.singlereactor;

import com.reactor.v2.singlethread.Bootstrap;
import com.reactor.v2.singlethread.Reactor;

import java.io.IOException;

public class Main {
  public static final int PORT = 8080;

  public static void main(String[] args) throws IOException {
    runMultiThreadSingleReactor();
  }

  public static void runMultiThreadSingleReactor() throws IOException {
    com.reactor.v2.singlethread.Reactor reactor = new Reactor();
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.initAndRegisterServer(reactor, PORT, MultiThreadNioHandler.class);

    // 启动 Reactor 线程，开始监听 IO 事件
    reactor.startThread();
    reactor.executor.shutdown();
  }
}