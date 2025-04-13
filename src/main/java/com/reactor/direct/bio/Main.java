package com.reactor.direct.bio;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

  public static final int PORT = 8080;

  public static void main(String[] args) throws IOException {
    runBioServer();
  }

  public static void runBioServer() {
    final BioServer bioServer = new BioServer(PORT);

    ExecutorService mainThread = Executors.newSingleThreadExecutor();
    mainThread.submit(bioServer);
    mainThread.shutdown();
  }
}