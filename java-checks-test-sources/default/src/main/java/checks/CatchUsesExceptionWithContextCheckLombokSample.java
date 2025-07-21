package checks;

import lombok.Data;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

// Lombok annotation here is what confuses the parser.
@Data
class ApprovalInfo {
  private String id;
}

class ApprovalControllerAllInOne {
  public void decoy() {
    // Verify that we find a problem when it is there.
    try {
    } catch (Exception ex1) { // Noncompliant
      throw new RuntimeException("message");
    }
  }

  static <T> CompletableFuture<T> worker(Callable<T> callable) {
    return null;
  }

  public void example(ApprovalInfo approvalInfo) {
    worker(() -> approvalInfo.getId())
      .thenAccept(approval -> {
        try {
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      });
  }
}
