package checks;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockingOperationsInVirtualThreadsCheckSample {
  HttpURLConnection con = (java.net.HttpURLConnection) new java.net.URL("http://localhost:64738").openConnection();

  public BlockingOperationsInVirtualThreadsCheckSample() throws IOException {
  }

  void newThread() throws IOException {

    new Thread(() -> {
//  ^^^<
      try {
        con.getResponseMessage(); // Noncompliant {{Use virtual threads for heavy blocking operations.}}
//          ^^^^^^^^^^^^^^^^^^
        con.getResponseCode(); // Noncompliant
        con.disconnect(); // Compliant
        con.getInputStream(); // Compliant
        con.getRequestMethod(); // Compliant
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    Thread.ofPlatform().start(() -> {
      try {
        con.getResponseMessage(); // Noncompliant
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    Thread.ofPlatform().unstarted(() -> {
      try {
        con.getResponseMessage(); // Noncompliant
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    try {
      con.getResponseMessage(); // Compliant - not in a thread
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          con.getResponseMessage(); // Noncompliant
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });

    new Thread() {
      @Override
      public void run() {
        try {
          con.getResponseMessage(); // Noncompliant
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  void executor() {
    ExecutorService executor = Executors.newFixedThreadPool(100);
    executor.execute(() -> {
      try {
        con.getResponseMessage(); // Compliant FN - we don't support ExecutorService, as it is hard to determine if it uses virtual threads without data flow analysis
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private class NoncompliantThread extends Thread {
    @Override
    public void run() {
      try {
        con.getResponseMessage(); // Noncompliant
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private class CompliantRunnable implements Runnable {
    @Override
    public void run() {
      try {
        con.getResponseMessage(); // Compliant - not a thread
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  void otherBlockingOperations() {
    new Thread(() -> {
      try {
        Thread.sleep(1000); // Noncompliant
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
  }

  void compliantVirtualThreads() {

    Thread.ofVirtual().start(() -> {
      try {
        con.getResponseMessage(); // Compliant
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    Thread.startVirtualThread(() -> {
      try {
        con.getResponseMessage(); // Compliant
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    try (ExecutorService myExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
      myExecutor.execute(() -> {
        try {
          con.getResponseMessage(); // Compliant
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }
}
