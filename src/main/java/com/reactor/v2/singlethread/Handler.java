package com.reactor.v2.singlethread;

import java.nio.channels.SelectionKey;

public interface Handler extends Runnable {

  void init(SelectionKey key);
}
