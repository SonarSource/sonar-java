package checks.spring;

import com.google.common.util.concurrent.ListenableFuture;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.springframework.scheduling.annotation.Async;

public class AsyncMethodsReturnTypeCheckSample {
  @Async
  String asyncString() { // Noncompliant [[sc=3;ec=9]] {{Async methods should return 'void' or a 'Future' type.}}
    return null;
  }

  @Async
  int asyncInt() { // Noncompliant
    return 0;
  }

  @Async
  void asyncVoid() { // Compliant
  }

  @Async
  Future<String> asyncFutureString() { // Compliant
    return null;
  }

  @Async
  ListenableFuture<String> asyncListenableFutureString() { // Compliant
    return null;
  }

  @Async
  CompletableFuture<String> asyncCompletableFutureString() { // Compliant
    return null;
  }

  String synchronousMethod() { // Compliant
    return null;
  }

  @Async
  <T> T generic() { // Noncompliant
    return null;
  }

  @Async
  <T extends URL> T genericExtNotFuture() { // Noncompliant
    return null;
  }

  @Async
  <T extends Future<?>> T genericExtFuture() { // Compliant
    return null;
  }

  @Async
  <T extends CompletableFuture<?>> T genericExtCompletableFuture() { // Compliant
    return null;
  }
}

class MyTypeOfList<T> extends ArrayList<T> {
  @Async
  T doSomething(int unused) { // Noncompliant
    return null;
  }
}

class MyTypeOfListExtFuture<T extends Future<?>> extends ArrayList<T> {
  @Async
  T doSomething(int unused) { // Compliant, as T extends Future
    return null;
  }
}
