package checks;

import lombok.Data;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

// Lombok annotation here is what confuses the parser:
// if this class had a getter, then `ex` in `example()` below would not be unknown.
@Data
class Info {
  private String id;
}

class CatchUsesExceptionWithContextCheckLombokSample {
  public void decoy() {
    // Verify that we find a problem when it is there.
    try {
    } catch (Exception ex) { // Noncompliant
      throw new RuntimeException("message");
    }
  }

  static <T> CompletableFuture<T> worker(Callable<T> callable) {
    return null;
  }

  public void example(Info info) {
    worker(() -> info.getId())
      .thenAccept(id -> {
        try {
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      });
  }
}
