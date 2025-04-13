package com.reactor.v2.singlethread;

import java.lang.reflect.Constructor;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * 处理客户端连接事件, serverSocket端，ACCEPT 事件的handler
 */
public class Acceptor implements Handler {
  /**
   * 选择器，NIO 组件，通知 Channel 就绪的事件
   */
  private Selector selector;

  /**
   * TCP 服务端 Socket，监听某个端口进来的客户端连接和请求
   */
  private ServerSocketChannel serverSocket;

  /**
   * Handler 的类型， 处理接入与Client通信的Socket的handler
   */
  private final Class<? extends Handler> handlerClass;

  public Acceptor(Class<? extends Handler> handlerClass) {
    this.handlerClass = handlerClass;
  }

  @Override
  public void init(SelectionKey key) {
    this.selector = key.selector();
    this.serverSocket = (ServerSocketChannel) key.channel();
  }

  @Override
  public void run() {
    try {
      // 接收客户端连接，返回客户端 SocketChannel。非阻塞模式下，没有客户端连接则直接返回 null
      final SocketChannel socket = serverSocket.accept();
      if (socket == null) {
        return;
      }

      // 根据 Handler 类型，实例化 Handler, 在 Handler 线程中处理客户端 IO 事件
      final Constructor<? extends Handler> constructor = handlerClass.getConstructor();
      Handler handler = constructor.newInstance();

      Bootstrap.initAndRegister(selector, socket, handler);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}