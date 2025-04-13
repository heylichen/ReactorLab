package com.reactor.direct.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NioEchoClient {
  private static final String HOST = "localhost";
  private static final int PORT = 8080;
  private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
  private SocketChannel socketChannel;
  private Selector selector;

  public static void main(String[] args) throws IOException {
    NioEchoClient client = new NioEchoClient();
    client.start();
  }

  public void start() throws IOException {
    // 初始化Selector和SocketChannel
    selector = Selector.open();
    socketChannel = SocketChannel.open();
    socketChannel.configureBlocking(false); // 非阻塞模式

    // 发起非阻塞连接
    boolean connected = socketChannel.connect(new InetSocketAddress(HOST, PORT));
    if (!connected) {
      // 连接未立即完成，注册OP_CONNECT事件
      socketChannel.register(selector, SelectionKey.OP_CONNECT);
    } else {
      // 已连接，注册OP_READ事件并启动输入线程
      handleConnected();
    }

    // 启动用户输入线程
    new Thread(this::handleUserInput).start();

    // 事件循环
    while (!Thread.interrupted()) {
      selector.select(); // 阻塞直到有事件就绪
      Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

      while (keys.hasNext()) {
        SelectionKey key = keys.next();
        keys.remove();

        if (!key.isValid()) {
          continue;
        }

        if (key.isConnectable()) {
          handleConnect(key);
        } else if (key.isReadable()) {
          handleRead(key);
        } else if (key.isWritable()) {
          handleWrite(key);
        }
      }
    }
  }

  private void handleConnect(SelectionKey key) throws IOException {
    SocketChannel channel = (SocketChannel) key.channel();
    if (channel.finishConnect()) { // 完成连接
      System.out.println("Connected to server");
      handleConnected();
    } else {
      System.err.println("Connection failed");
      System.exit(1);
    }
  }

  private void handleConnected() throws IOException {
    // 注册OP_READ事件，准备接收服务器响应
    socketChannel.register(selector, SelectionKey.OP_READ);
  }

  private void handleRead(SelectionKey key) throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    int bytesRead = socketChannel.read(buffer);

    if (bytesRead == -1) {
      System.out.println("Server closed the connection");
      key.cancel();
      socketChannel.close();
      return;
    }

    buffer.flip();
    String response = StandardCharsets.UTF_8.decode(buffer).toString();
    System.out.println("Received from server: " + response);
  }

  private void handleWrite(SelectionKey key) throws IOException {
    String message = messageQueue.poll();
    if (message != null) {
      //server 端约定消息结束符号
      message = message + "\r\n";
      ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
      socketChannel.write(buffer);
      System.out.print("Sent to server: " + message);
    }

    // 消息发送完毕后，取消关注OP_WRITE事件，避免不必要的CPU占用
    key.interestOps(SelectionKey.OP_READ);
  }

  private void handleUserInput() {
    Scanner scanner = new Scanner(System.in);
    while (true) {
      System.out.print("Enter message (type 'exit' to quit): ");
      String input = scanner.nextLine().trim();

      if ("exit".equalsIgnoreCase(input)) {
        System.out.println("Closing client...");
        try {
          socketChannel.close();
          selector.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
        System.exit(0);
      }

      if (!input.isEmpty()) {
        messageQueue.offer(input);
        // 注册OP_WRITE事件以触发发送
        socketChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
        selector.wakeup(); // 唤醒Selector处理新事件
      }

      sleep(1000);
    }
  }

  private void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}