package com.reactor.v2.singlethread;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Bootstrap {

  public static void initAndRegisterServer(Reactor reactor, int port, Class<? extends Handler> handlerClass) throws IOException {
    //初始化ServerSocket
    ServerSocketChannel serverSocket = ServerSocketChannel.open();
    serverSocket.socket().bind(new InetSocketAddress(port));
    serverSocket.configureBlocking(false);

    // 注册并关注一个 IO 事件，这里是 ACCEPT（接收客户端连接）
    SelectionKey selectionKey = reactor.register(serverSocket);
    selectionKey.interestOps(SelectionKey.OP_ACCEPT);

    // 将 Acceptor 作为附件关联到 SelectionKey 上，用于在客户端连接事件发生时取出，让 Acceptor 去分发连接给 Handler
    Acceptor acceptor = new Acceptor(handlerClass);
    selectionKey.attach(acceptor);

    acceptor.init(selectionKey);
  }

  public static void initAndRegister(Selector selector, SocketChannel socket, Handler handler) throws IOException {
    // 设置非阻塞（NIO）。这样，socket 上的操作如果无法立即完成，不会阻塞，而是会立即返回。
    socket.configureBlocking(false);
    // Optionally try first read now
    // 注册客户端 socket 到 Selector。
    // 这里先不设置感兴趣的事件，分离 register 和 interestOps 这两个操作，避免多线程下的竞争条件和同步问题。
    SelectionKey selectionKey = socket.register(selector, 0);
    // 把 Handler 自身放到 selectionKey 的附加属性中，用于在 IO 事件就绪时从 selectedKey 中获取 Handler，然后处理 IO 事件。
    selectionKey.attach(handler);
    // 监听客户端连接上的 IO READ 事件
    selectionKey.interestOps(SelectionKey.OP_READ);

    handler.init(selectionKey);

    // 由于 Selector 的注册信息发生变化，立即唤醒 Selector，让它能够处理最新订阅的 IO 事件
    selector.wakeup();
  }
}
