package checks;

import java.util.function.Consumer;
import java.util.function.Function;

public class LambdaSingleExpressionCheckSample {

  public static class Ambiguous {
    public void ambiguous(Consumer<String> consumer) {}
    public void ambiguous(Function<String, String> function) {}

    void sample() {
      // Here, curly braces are required to avoid ambiguity.
      ambiguous(s -> { System.out.println(); }); // Compliant

      // We can remove the braces and return keyword.
      ambiguous(s -> { return "Hello " + s; }); // Noncompliant

      ambiguous(s -> "Hello " + s); // Compliant
    }
  }

  public static class Unambiguous {
    public void unambiguous(Consumer<String> consumer) {}

    void sample() {
      unambiguous(s -> { System.out.println(); }); // Noncompliant
    }
  }

  public static class OverloadedUnambiguous1 {
    public void unambiguous(Consumer<String> consumer, int i) {}
    public void unambiguous(Consumer<String> consumer, String s) {}

    void samples() {
      unambiguous(s -> { System.out.println(); }, 5); // Noncompliant
    }
  }

  public static class OverloadedUnambiguous2 {
    public void unambiguous(Consumer<String> consumer, int i) {}
    public void unambiguous(Function<String, String> function, String s) {}

    void sample() {
      // False negative due to single-argument heuristic.
      // The second argument disambiguates the invocation.
      unambiguous(s -> { System.out.println(); }, 5); // Compliant
    }
  }

  public static class OnExpression {
    public OnExpression somethingElse() {
      return this;
    }

    public OnExpression ambiguous(Consumer<String> consumer) {
      return this;
    }

    public OnExpression ambiguous(Function<String, String> function) {
      return this;
    }

    public OnExpression unambiguous(Consumer<String> consumer, int i) {
      return this;
    }

    public OnExpression unambiguous(Consumer<String> consumer, String s) {
      return this;
    }

    static void sample() {
      new OnExpression()
        .ambiguous(s -> { System.out.println(); }) // Compliant
        .unambiguous(s -> { System.out.println(); }, 5); // Noncompliant

      OnExpression onExpression = new OnExpression();
      onExpression
        .unambiguous(s -> { System.out.println(); }, 5) // Noncompliant
        .ambiguous(s -> { System.out.println(); }); // Compliant
    }
  }

  void samples() {
    // Ensure we can handle lambdas which are not passed as arguments (assignment here).
    Consumer<String> consumer = s -> { System.out.println(s); }; // Noncompliant
    consumer.accept("Hello");

    // Test for potential error when accessing the n-th parameter's type due to varargs.
    lambdaVararg(
      s -> { System.out.println("one"); }, // Noncompliant
      s -> { System.out.println("two"); }, // Noncompliant
      s -> { System.out.println("three"); } // Noncompliant
    );
  }

  void lambdaVararg(Consumer<String> ...consumers) {}
}
