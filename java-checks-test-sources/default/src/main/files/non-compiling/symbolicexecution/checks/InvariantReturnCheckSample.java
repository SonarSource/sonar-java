package symbolicexecution.checks;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class InvariantReturnCheckSample {

  class Example {
    private static void confuse() {
      org.sonar.check.Priority.valueOf("INFO");
      org.slf4j.LoggerFactory.getLogger("Foo").info("a message");
      com.amazonaws.services.ecs.model.ScaleUnit.valueOf("unit");
      com.google.gson.stream.JsonToken.valueOf("token");
    }

    String foo() {
      if (ThreadLocalRandom.current().nextInt() > 1) {
        return null;
      }

      confuse();

      return "something";
    }
  }

  @Log4j2
  class TestS3516 {
    boolean filter(SelectionKey selectionKey, String hostAddress) {
      if (hostAddress == null) {
        return true;
      }
      return getSth(selectionKey).map(address -> true).orElse(false);
    }

    private Optional<String> getSth(SelectionKey selectionKey) {
      SelectableChannel channel = selectionKey.channel();
      if (channel instanceof SocketChannel) {
        log.info("Test");
        return Optional.of("test");
      }
      return Optional.empty();
    }

  }

  @Log4j2
  class TestS3516_2 {
    boolean filter(SelectionKey selectionKey, String hostAddress) {
      if (hostAddress == null) {
        return true;
      }
      return getInetSocketAddress(selectionKey)
        .map(address -> address.getAddress().getHostAddress().equals(hostAddress)).orElse(false);
    }

    private Optional<InetSocketAddress> getInetSocketAddress(SelectionKey selectionKey) {
      SelectableChannel channel = selectionKey.channel();
      if (channel instanceof SocketChannel socketChannel) {
        try {
          return Optional.of(((InetSocketAddress) socketChannel.getRemoteAddress()));
        } catch (IOException e) {
          log.error("", e);
        }
      }
      return Optional.empty();
    }

  }

  public boolean someMethod() {
    // No issue should be raised because someMethod may be overriden
    try {
      someExceptionalMethod();
    } catch (IllegalArgumentException e) {
      return false;
    }
    return true;
  }

  int someExceptionalMethod() {
    throw new IllegalArgumentException();
  }

}
