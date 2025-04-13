package com.reactor.direct.singlethread;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Reactor implements Runnable {
  /**
   * 选择器，NIO 组件，通知 Channel 就绪的事件
   */
  final Selector selector;

  /**
   * TCP 服务端 Socket，监听某个端口进来的客户端连接和请求
   */
  ServerSocketChannel serverSocket;

  /**
   * Reactor 的执行线程
   */
  public final ExecutorService executor;

  /**
   * Handler 的类型， 处理接入与Client通信的Socket的handler
   */
  final Class<?> handlerClass;

  /**
   * 直接创建 Reactor 使用
   */
  public Reactor(int port, Class<?> handlerClass) throws IOException {
    this.handlerClass = handlerClass;
    executor = Executors.newSingleThreadExecutor();
    selector = Selector.open();
    //初始化ServerSocket
    serverSocket = ServerSocketChannel.open();
    serverSocket.socket().bind(new InetSocketAddress(port));
    serverSocket.configureBlocking(false);
    // 注册并关注一个 IO 事件，这里是 ACCEPT（接收客户端连接）
    final SelectionKey selectionKey = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
    // 将 Acceptor 作为附件关联到 SelectionKey 上，用于在客户端连接事件发生时取出，让 Acceptor 去分发连接给 Handler
    selectionKey.attach(new Acceptor());
  }

  //for multi reactor
  public Reactor() throws IOException {
    executor = Executors.newSingleThreadExecutor();
    selector = Selector.open();
    this.handlerClass = null;
  }

  /**
   * 启动 Reactor 线程，执行 run 方法
   */
  public void startThread() {
    executor.execute(this);
  }

  @Override
  public void run() { // normally in a new Thread
    try {
      // 死循环，直到线程停止
      while (!Thread.interrupted()) {
        // 阻塞，直到至少有一个通道的 IO 事件就绪
        selector.select();
        // 拿到就绪通道的选择键 SelectionKey 集合
        final Set<SelectionKey> selectedKeys = selector.selectedKeys();
        // 遍历就绪通道的 SelectionKey
        for (SelectionKey selectedKey : selectedKeys) {
          // 分发
          dispatch(selectedKey);
        }
        // 清空就绪通道的 SelectionKey 集合
        selectedKeys.clear();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * 分发事件，将就绪通道的注册键关联的处理器取出并执行
   * <p>
   * 在 MainReactor 中，就绪的是客户端连接事件，处理器是 Acceptor
   * <p>
   * 在 SubReactor 中，就绪的是客户端 IO 事件，处理器是 Handler
   */
  private void dispatch(SelectionKey selectionKey) {
    //约定attachment都是Runnable
    final Runnable runnable = (Runnable) selectionKey.attachment();
    if (runnable != null) {
      // 执行处理
      runnable.run();
    }
  }

  /**
   * 处理客户端连接事件, serverSocket端，ACCEPT 事件的handler
   */
  class Acceptor implements Runnable {
    @Override
    public void run() {
      try {
        // 接收客户端连接，返回客户端 SocketChannel。非阻塞模式下，没有客户端连接则直接返回 null
        final SocketChannel socket = serverSocket.accept();
        if (socket != null) {
          // 将提示发送给客户端
          socket.write(ByteBuffer.wrap("reactor> ".getBytes()));
          // 根据 Handler 类型，实例化 Handler
          final Constructor<?> constructor = handlerClass.getConstructor(Selector.class, SocketChannel.class);
          // 在 Handler 线程中处理客户端 IO 事件
          constructor.newInstance(selector, socket);
        }
      } catch (Exception e) {
      }
    }
  }

  //-------------for multi reactor
  public Selector getSelector() {
    return selector;
  }

  public SelectionKey register(ServerSocketChannel serverSocket) {
    try {
      return serverSocket.register(selector, 0);
    } catch (ClosedChannelException e) {
      throw new RuntimeException(e);
    }
  }
}