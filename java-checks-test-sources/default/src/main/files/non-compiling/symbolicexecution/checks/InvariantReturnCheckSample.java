package symbolicexecution.checks;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;


public class InvariantReturnCheckSample {

  class CompliantExample1 {

    private static void confuse() {
      methodThatDoesNotExist();   // unresolved symbol
    }

    String foo() {
      if (ThreadLocalRandom.current().nextInt() > 1) {
        return null;
      }
      confuse();  // Compliant
      return "something";
    }
  }

  class CompliantExample2 {

    boolean filter(SelectableChannel channel) {
      if (channel == null){
        return false;
      }
      return channel == null ? false :
        getSth(channel) // Compliant
        .isPresent();
    }

    private Optional<InetSocketAddress> getSth(SelectableChannel channel) {
      if (channel instanceof SocketChannel socketChannel) {
        try {
          return Optional.of(((InetSocketAddress) socketChannel.getRemoteAddress()));
        } catch (IOException e) {
          log.error("", e);   // unresolved symbol
        }
      }
      return Optional.empty();
    }

  }

  class CompliantExample3 {

    boolean filter(SelectableChannel channel) {
      if (channel == null){
        return false;
      }
      return getSth(channel) // Compliant
        .isPresent();
    }

    private Optional<String> getSth(SelectableChannel channel) {
      if (channel instanceof SocketChannel) {
        log.info("Test");   // unresolved symbol
        return Optional.of("test");
      }
      return Optional.empty();
    }

  }

  class NoncompliantExample {

    boolean filter(SelectableChannel channel) { // Noncompliant [[sc=13;ec=19]] {{Refactor this method to not always return the same value.}}
      if (channel == null){
        return false;
      }
      return true ? false : getSth(null);
    }

    private Optional<String> getSth(SelectableChannel channel) {
      if (channel instanceof SocketChannel) {
        log.info("Test");   // unresolved symbol
        return Optional.of("test");
      }
      return Optional.empty();
    }

  }

}
