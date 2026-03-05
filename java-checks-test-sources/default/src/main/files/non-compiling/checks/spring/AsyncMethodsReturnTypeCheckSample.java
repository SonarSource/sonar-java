package checks.spring;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.springframework.scheduling.annotation.Async;

public class AsyncMethodsReturnTypeCheckSample {

  @Async
  public CompletableFuture<Data> unknownTypeArg() { // Compliant
    return CompletableFuture.completedFuture(new Data());
  }

  @Async
  public void voidType() { // Compliant
    return;
  }

  @Async
  public CompletableFuture<Integer> futureSubtype() { // Compliant
    return CompletableFuture.completedFuture(42);
  }

  @Async
  public Data unknownType() { // Compliant, no issue is raised when the return type is unknown (it might be a subtype of Future)
    return new Data();
  }

  @Async
  public Integer builtinType() { // Noncompliant {{Async methods should return 'void' or a 'Future' type.}}
//       ^^^^^^^
    return 42;
  }

}
