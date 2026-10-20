package checks;

import java.util.function.Supplier;

public class RedundantQualifiedThisCheckSample {

  private final String id = "id";
  private int counter;

  String redundantInMethod() {
    return RedundantQualifiedThisCheckSample.this.id; // Noncompliant {{Remove this redundant qualifier.}} [[quickfixes=qf1]]
//         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf1 {{Remove the redundant qualifier}}
    // edit@qf1 [[sc=12;ec=46]] {{}}
  }

  void redundantOnMethodCall() {
    RedundantQualifiedThisCheckSample.this.increment(); // Noncompliant [[quickfixes=qf2]]
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf2 {{Remove the redundant qualifier}}
    // edit@qf2 [[sc=5;ec=39]] {{}}
  }

  RedundantQualifiedThisCheckSample redundantWithoutMemberAccess() {
    return RedundantQualifiedThisCheckSample.this; // Noncompliant [[quickfixes=qf3]]
//         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf3 {{Remove the redundant qualifier}}
    // edit@qf3 [[sc=12;ec=46]] {{}}
  }

  Supplier<String> redundantInLambda() {
    return () -> RedundantQualifiedThisCheckSample.this.id; // Noncompliant [[quickfixes=qf4]]
//               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf4 {{Remove the redundant qualifier}}
    // edit@qf4 [[sc=18;ec=52]] {{}}
  }

  Supplier<Supplier<String>> redundantInNestedLambda() {
    return () -> () -> RedundantQualifiedThisCheckSample.this.id; // Noncompliant [[quickfixes=qf5]]
//                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf5 {{Remove the redundant qualifier}}
    // edit@qf5 [[sc=24;ec=58]] {{}}
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
      return Inner.this.id; // Noncompliant [[quickfixes=qf6]]
//           ^^^^^^^^^^
      // fix@qf6 {{Remove the redundant qualifier}}
      // edit@qf6 [[sc=14;ec=20]] {{}}
    }

    Supplier<String> outerQualifierIsRequiredInLambda() {
      return () -> RedundantQualifiedThisCheckSample.this.id;
    }

    Supplier<String> ownQualifierIsRedundantInLambda() {
      return () -> Inner.this.id; // Noncompliant [[quickfixes=qf7]]
//                 ^^^^^^^^^^
      // fix@qf7 {{Remove the redundant qualifier}}
      // edit@qf7 [[sc=20;ec=26]] {{}}
    }

    class Innermost {
      String enclosingQualifiersAreRequired() {
        return RedundantQualifiedThisCheckSample.this.id + Inner.this.id;
      }

      String ownQualifierIsRedundant() {
        return Innermost.this.toString(); // Noncompliant [[quickfixes=qf8]]
//             ^^^^^^^^^^^^^^
        // fix@qf8 {{Remove the redundant qualifier}}
        // edit@qf8 [[sc=16;ec=26]] {{}}
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
        return Local.this.toString(); // Noncompliant [[quickfixes=qf9]]
//             ^^^^^^^^^^
        // fix@qf9 {{Remove the redundant qualifier}}
        // edit@qf9 [[sc=16;ec=22]] {{}}
      }
    }
    return new Local().get();
  }

  static class StaticNested {
    private final String id = "nested";

    String ownQualifierIsRedundant() {
      return StaticNested.this.id; // Noncompliant [[quickfixes=qf10]]
//           ^^^^^^^^^^^^^^^^^
      // fix@qf10 {{Remove the redundant qualifier}}
      // edit@qf10 [[sc=14;ec=27]] {{}}
    }

    Supplier<String> ownQualifierIsRedundantInLambda() {
      return () -> StaticNested.this.id; // Noncompliant [[quickfixes=qf11]]
//                 ^^^^^^^^^^^^^^^^^
      // fix@qf11 {{Remove the redundant qualifier}}
      // edit@qf11 [[sc=20;ec=33]] {{}}
    }
  }

  enum Status {
    OPEN;

    String ownQualifierIsRedundant() {
      return Status.this.name(); // Noncompliant [[quickfixes=qf12]]
//           ^^^^^^^^^^^
      // fix@qf12 {{Remove the redundant qualifier}}
      // edit@qf12 [[sc=14;ec=21]] {{}}
    }
  }

  record Point(int x, int y) {
    int ownQualifierIsRedundant() {
      return Point.this.x; // Noncompliant [[quickfixes=qf13]]
//           ^^^^^^^^^^
      // fix@qf13 {{Remove the redundant qualifier}}
      // edit@qf13 [[sc=14;ec=20]] {{}}
    }
  }
}

