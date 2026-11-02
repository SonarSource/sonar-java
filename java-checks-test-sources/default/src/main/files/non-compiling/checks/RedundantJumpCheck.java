import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

abstract class A {

  public void reproducer() {
    for (;;) {
      continue; // Noncompliant
    }
    for (;;) {
      if(condition){
        continue;
      }
      foo();
    }
  }

  public void loops() {

    while (condition1) {
      foo();
      continue; // Noncompliant
    }

    while (condition1) {
      foo();
      break;
    }

    while (condition1) {
      if (condition2) {
        continue; // Noncompliant
      } else {
        foo();
      }
    }
  }

  public void for_loops() {
    for (int i = 0; i < 2; i++) {
      if (i == 1) {
        continue; // Noncompliant
      } else {
        foo();
      }
    }

    for (int i = 0; i < 2; i++) {
      if (i == 1) {
        break;
      }
    }

    for (int i = 0; i < 2; i++) {
      if (i == 1) {
        continue; // Noncompliant
      }
      continue; // Noncompliant
    }

    for (int i = 0; i < 2; i++) {
      foo();
      continue; // Noncompliant
    }

    for (int i = 0; i < 3; i = j < 5 ? i + 1 : i) {
      continue; // Noncompliant
    }

    for (int i = 0; i < 2; i++) {
      foo();
      if (i == 1) {
        continue; // Noncompliant
      }
    }

    for(int i =0;i<10;i++) {
      if(i == 5) {
        continue;
      }
      foo();
    }
  }

  public int return_in_non_void_method() {
    foo();
    return 42;
  }

  public void useless_return() {
    foo();
    return; // Noncompliant
  }

  public void void_method_with_useful_return_without_expression() {
    if (condition) {
      return;
    }
    foo();
  }

  public void return_in_try_finally_followed_by_empty_statement() {
    try {
      if (condition2) {
        return; // Noncompliant
      }
    } finally {
      bar();
    }
    ;
  }

  public void continue_in_try_finally_followed_by_empty_statement() {
    while (condition1) {
      try {
        if (condition2) {
          continue; // Noncompliant
        }
      } finally {
        bar();
      }
      ;
    }
  }

  public void early_return_in_loop_try_finally() {
    while (condition1) {
      try {
        if (condition2) {
          return;
        }
        foo();
      } finally {
        bar();
      }
      foo();
    }
  }

  public void return_at_end_of_loop_try_finally() {
    while (condition1) {
      try {
        if (condition2) {
          return;
        }
      } finally {
        bar();
      }
      foo();
    }
  }

  public void early_return_in_for_loop_try_finally() {
    for (int i = 0; i < 10; i++) {
      try {
        if (condition2) {
          return;
        }
        foo();
      } finally {
        bar();
      }
      foo();
    }
  }

  public void return_at_end_of_for_loop_try_finally() {
    for (int i = 0; i < 10; i++) {
      try {
        if (condition2) {
          return;
        }
      } finally {
        bar();
      }
      foo();
    }
  }

  public void early_return_in_dowhile_loop_try_finally() {
    do {
      try {
        if (condition2) {
          return;
        }
        foo();
      } finally {
        bar();
      }
      foo();
    } while (condition1);
  }

  public void continue_in_loop_try_finally() {
    while (condition1) {
      try {
        if (condition2) {
          continue;
        }
        foo();
      } finally {
        bar();
      }
      foo();
    }
  }

  public void continue_at_end_of_loop_try_finally() {
    while (condition1) {
      try {
        if (condition2) {
          continue;
        }
      } finally {
        bar();
      }
      foo();
    }
  }

  public void redundant_continue_in_loop_try_finally() {
    while (condition1) {
      try {
        foo();
        continue; // Noncompliant
      } finally {
        bar();
      }
    }
  }

  public void return_in_try_finally_at_end_of_method() {
    try {
      foo();
      return; // Noncompliant
    } finally {
      bar();
    }
  }

  public void return_in_dowhile_false_try_finally() {
    do {
      try {
        foo();
        return; // Noncompliant
      } finally {
        bar();
      }
    } while (false);
  }

  public void return_in_while_false_try_finally() {
    while (false) {
      try {
        foo();
        return; // Noncompliant
      } finally {
        bar();
      }
    }
  }

  public void return_in_for_false_try_finally() {
    for (; false;) {
      try {
        foo();
        return; // Noncompliant
      } finally {
        bar();
      }
    }
  }

  public void return_in_dowhile_boolean_false_try_finally() {
    do {
      try {
        foo();
        return; // Noncompliant
      } finally {
        bar();
      }
    } while (Boolean.FALSE);
  }

  public void return_in_dowhile_not_true_try_finally() {
    do {
      try {
        foo();
        return; // Noncompliant
      } finally {
        bar();
      }
    } while (!true);
  }

  public void switch_statements(int param) {
    switch (param) {
      case 0:
        foo();
        break;
      default:
    }
    foo();
    switch (param) {
      case 0:
        foo();
        return;
      case 1:
        bar();
        return;
    }
  }

  public void throwing_exception() {
    throw new UnsupportedOperationException();
  }

  public abstract void abstract_method();

  void undefinedLabel(int values[]) {
    for (int v: values) {
      if (v == 2) {
        continue label; // this should be a compiler error, but we want to test that the check does not crash
      }
    }
  }

  void continueInTryFinallyOutsideLoop() {
    try {
      continue; // this should be a compiler error, but we want to test that the check does not crash
    } finally {
      foo();
    }
  }

  Runnable emptyLambda = () -> {};

  Runnable expressionLambda = () -> System.out.println("expression");

  Supplier<Runnable> expressionReturningLambda = () -> () -> {
    return; // Noncompliant {{Remove this redundant jump.}}
//  ^^^^^^^
  };

  Runnable fieldInitializer = () -> {
    System.out.println("field");
    return; // Noncompliant {{Remove this redundant jump.}}
//  ^^^^^^^
  };

  void immediatelyInvokedLambda(int x) {
    Runnable r = () -> {
      System.out.println(x);
      return; // Noncompliant {{Remove this redundant jump.}}
//    ^^^^^^^
    };
    r.run();
    System.out.println("after lambda");
  }

  Runnable conditionalReturns(int x) {
    return () -> {
      if (x < 0) {
        return;
      }
      System.out.println(x);
      if (x == 0) {
        return; // Noncompliant {{Remove this redundant jump.}}
//      ^^^^^^^
      }
    };
  }

  IntSupplier valueReturningLambda(int x) {
    return () -> {
      System.out.println(x);
      return x;
    };
  }

  Runnable nestedLambdas(int x) {
    return () -> {
      Runnable inner = () -> {
        System.out.println(x);
        return; // Noncompliant {{Remove this redundant jump.}}
//      ^^^^^^^
      };
      inner.run();
      System.out.println("after inner lambda");
    };
  }

  Runnable loopJumps(int x) {
    return () -> {
      for (int i = 0; i < x; i++) {
        if (i == 1) {
          continue;
        }
        if (i == 2) {
          return;
        }
        System.out.println(i);
        continue; // Noncompliant {{Remove this redundant jump.}}
//      ^^^^^^^^^
      }
    };
  }

  Runnable returnThroughFinally(int x) {
    return () -> {
      try {
        System.out.println(x);
        return; // Noncompliant {{Remove this redundant jump.}}
//      ^^^^^^^
      } finally {
        System.out.println("cleanup");
      }
    };
  }

  Runnable earlyReturnThroughFinally(int x) {
    return () -> {
      try {
        if (x < 0) {
          return;
        }
      } finally {
        System.out.println("cleanup");
      }
      System.out.println(x);
    };
  }

  IntUnaryOperator continueInSwitchExpressionLambda() {
    return x -> switch (x) {
      default -> {
        for (int i = 0; i < x; i++) {
          System.out.println(i);
          continue; // Noncompliant {{Remove this redundant jump.}}
//        ^^^^^^^^^
        }
        yield x;
      }
    };
  }
}
