package com.reactor.v2.singlethread;

import com.reactor.v2.singlereactor.MultiThreadNioHandler;

import java.io.IOException;

public class Main {

  public static final int PORT = 8080;

  public static void main(String[] args) throws IOException {
    runSingleThreadReactor();
  }

  public static void runSingleThreadReactor() throws IOException {
    Reactor reactor = new Reactor();
    //初始化serverSocket, 监听OP_ACCEPT事件。接收到client Socket处理的逻辑在NioHandler
    Bootstrap.initAndRegisterServer(reactor, PORT, NioHandler.class);

    // 启动 Reactor 线程，开始监听 IO 事件
    reactor.startThread();
    reactor.executor.shutdown();
  }
}