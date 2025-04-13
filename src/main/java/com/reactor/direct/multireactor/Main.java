package com.reactor.direct.multireactor;

import com.reactor.direct.singlethread.NioHandler;

import java.io.IOException;

public class Main {

  public static final int PORT = 8080;

  public static void main(String[] args) throws IOException {
    runMultiReactor();
  }

  public static void runMultiReactor() throws IOException {
    // 创建单线程的主 Reactor 组
    ReactorGroup mainReactorGroup = new ReactorGroup(1);
    // 创建 4 个线程的从 Reactor 组
    ReactorGroup subReactorGroup = new ReactorGroup(4);

    MultiReactorBootstrap bootstrap = new MultiReactorBootstrap(PORT, mainReactorGroup, subReactorGroup, NioHandler.class);
    bootstrap.startMainReactor();
  }
}