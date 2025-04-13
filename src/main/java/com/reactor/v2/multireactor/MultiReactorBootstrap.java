package com.reactor.v2.multireactor;

import com.reactor.v2.singlethread.Handler;
import com.reactor.v2.singlethread.NioHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

public class MultiReactorBootstrap {
  /**
   * 主 Reactor 组
   */
  private ReactorGroup mainReactorGroup;

  /**
   * 从 Reactor 组
   */
  private ReactorGroup subReactorGroup;

  private final ServerSocketChannel serverSocket;

  public MultiReactorBootstrap(int port,
                               ReactorGroup mainReactorGroup,
                               ReactorGroup subReactorGroup,
                               Class<? extends Handler> handlerClass) throws IOException {
    this.mainReactorGroup = mainReactorGroup;
    this.subReactorGroup = subReactorGroup;

    // 将服务端 ServerSocketChannel 绑定到端口上
    serverSocket = ServerSocketChannel.open();
    serverSocket.socket().bind(new InetSocketAddress(port));
    serverSocket.configureBlocking(false);
    // 让 Main Reactor 监听 ServerSocketChannel 上的 ACCEPT 事件
    SelectionKey selectionKey = this.mainReactorGroup.register(serverSocket);
    selectionKey.interestOps(SelectionKey.OP_ACCEPT);
    selectionKey.attach(new Acceptor(subReactorGroup, serverSocket, handlerClass));
  }

  public void startMainReactor() {
    mainReactorGroup.next().startThread();
  }

}