package com.reactor.v2.multireactor;

import com.reactor.direct.singlethread.Reactor;
import com.reactor.v2.singlethread.Bootstrap;
import com.reactor.v2.singlethread.Handler;

import java.lang.reflect.Constructor;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * 处理客户端连接事件, serverSocket端，ACCEPT 事件的handler
 */
public class Acceptor implements Runnable {
  /**
   * Handler 的类型， 处理接入与Client通信的Socket的handler
   */
  private final Class<? extends Handler> handlerClass;

  /**
   * TCP 服务端 Socket，监听某个端口进来的客户端连接和请求
   */
  private final ServerSocketChannel serverSocket;

  /**
   * 从 Reactor 组
   */
  private final ReactorGroup subReactorGroup;

  public Acceptor(ReactorGroup subReactorGroup, ServerSocketChannel serverSocket, Class<? extends Handler> handlerClass) {
    this.subReactorGroup = subReactorGroup;
    this.serverSocket = serverSocket;
    this.handlerClass = handlerClass;
  }

  @Override
  public void run() {
    try {
      // 接收客户端连接，返回客户端 SocketChannel。非阻塞模式下，没有客户端连接则直接返回 null
      final SocketChannel socket = serverSocket.accept();
      if (socket == null) {
        return;
      }
      // 从 Sub Reactor 组中轮询选择一个 Reactor，用于处理新的客户端连接
      final Reactor subReactor = subReactorGroup.next();

      // 根据 Handler 类型，实例化 Handler, 在 Handler 线程中处理客户端 IO 事件
      final Constructor<? extends Handler> constructor = handlerClass.getConstructor();
      Handler handler = constructor.newInstance();

      Bootstrap.initAndRegister(subReactor.getSelector(), socket, handler);

      // 启动 Sub Reactor 线程，开始监听客户端 SocketChannel 上的 IO 事件
      subReactor.startThread();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}