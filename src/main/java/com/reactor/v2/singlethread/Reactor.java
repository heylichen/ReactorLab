package com.reactor.v2.singlethread;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Reactor implements Runnable {
  /**
   * 选择器，NIO 组件，通知 Channel 就绪的事件
   */
  final Selector selector;

  /**
   * Reactor 的执行线程
   */
  public final ExecutorService executor;

  //for multi reactor
  public Reactor() throws IOException {
    executor = Executors.newSingleThreadExecutor();
    selector = Selector.open();
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
      throw new RuntimeException(e);
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