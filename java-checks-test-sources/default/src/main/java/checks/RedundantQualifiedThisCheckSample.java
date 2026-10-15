package checks;

import java.util.function.Supplier;

public class RedundantQualifiedThisCheckSample {

  private final String id = "id";
  private int counter;

  String redundantInMethod() {
    return RedundantQualifiedThisCheckSample.this.id; // Noncompliant {{Remove this redundant qualifier.}}
//         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  void redundantOnMethodCall() {
    RedundantQualifiedThisCheckSample.this.increment(); // Noncompliant
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  RedundantQualifiedThisCheckSample redundantWithoutMemberAccess() {
    return RedundantQualifiedThisCheckSample.this; // Noncompliant
//         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  Supplier<String> redundantInLambda() {
    return () -> RedundantQualifiedThisCheckSample.this.id; // Noncompliant
//               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  Supplier<Supplier<String>> redundantInNestedLambda() {
    return () -> () -> RedundantQualifiedThisCheckSample.this.id; // Noncompliant
//                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  void increment() {
    counter++;
  }

  String compliantExplicitThis() {
    return this.id;
  }

  String compliantImplicitThis() {
    return id;
  }

  class Inner {
    private final String id = "inner";

    String outerQualifierIsRequired() {
      return RedundantQualifiedThisCheckSample.this.id;
    }

    String ownQualifierIsRedundant() {
      return Inner.this.id; // Noncompliant
//           ^^^^^^^^^^
    }

    Supplier<String> outerQualifierIsRequiredInLambda() {
      return () -> RedundantQualifiedThisCheckSample.this.id;
    }

    Supplier<String> ownQualifierIsRedundantInLambda() {
      return () -> Inner.this.id; // Noncompliant
//                 ^^^^^^^^^^
    }

    class Innermost {
      String enclosingQualifiersAreRequired() {
        return RedundantQualifiedThisCheckSample.this.id + Inner.this.id;
      }

      String ownQualifierIsRedundant() {
        return Innermost.this.toString(); // Noncompliant
//             ^^^^^^^^^^^^^^
      }
    }
  }

  Supplier<String> anonymousClassNeedsQualifier() {
    return new Supplier<String>() {
      @Override
      public String get() {
        return RedundantQualifiedThisCheckSample.this.id;
      }
    };
  }

  Supplier<String> lambdaInAnonymousClassNeedsQualifier() {
    return new Supplier<String>() {
      @Override
      public String get() {
        Supplier<String> nested = () -> RedundantQualifiedThisCheckSample.this.id;
        return nested.get();
      }
    };
  }

  String localClassNeedsQualifier() {
    class Local {
      String get() {
        return RedundantQualifiedThisCheckSample.this.id;
      }
    }
    return new Local().get();
  }

  String localClassOwnQualifierIsRedundant() {
    class Local {
      String get() {
        return Local.this.toString(); // Noncompliant
//             ^^^^^^^^^^
      }
    }
    return new Local().get();
  }

  static class StaticNested {
    private final String id = "nested";

    String ownQualifierIsRedundant() {
      return StaticNested.this.id; // Noncompliant
//           ^^^^^^^^^^^^^^^^^
    }

    Supplier<String> ownQualifierIsRedundantInLambda() {
      return () -> StaticNested.this.id; // Noncompliant
//                 ^^^^^^^^^^^^^^^^^
    }
  }

  enum Status {
    OPEN;

    String ownQualifierIsRedundant() {
      return Status.this.name(); // Noncompliant
//           ^^^^^^^^^^^
    }
  }

  record Point(int x, int y) {
    int ownQualifierIsRedundant() {
      return Point.this.x; // Noncompliant
//           ^^^^^^^^^^
    }
  }
}
