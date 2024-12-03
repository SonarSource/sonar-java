class Reproducer {
  public void test(boolean cond) {
    // When performing type inference for the lambda below, in particular its return type, Eclipse JDT will deduce a cyclic type for
    // java.lang.Record.
    //
    // To give some more details:
    // It will deduce that Record implements the interface `SuperType` defined below.
    // Probably because of this erroneous deduction, it will further report that Record is a sub-type of itself.
    var response = forceTypeInferenceForLambda(() -> {
      if (cond) {
        return new SubTypeA();
      } else {
        return new SubTypeB();
      }
    });

    // This will crash DBD due to an infinite recursion on the type hierarchy for the commits before DBD-1268.
    response.toString();
  }

  <T> T forceTypeInferenceForLambda(Supplier<T> supplier) {
    return null;
  }

  private interface SuperType {
  }

  // Using records seems to be necessary to trigger the problem
  private record SubTypeA() implements SuperType {
  }

  private record SubTypeB() implements SuperType {
  }
}


class A {
  public enum Fruit {
    APPLE, BANANA, GRAPE
  }

  public enum Cake {
    LEMON_TART, CHEESE_CAKE;

    boolean foo(Object o) {
      return equals(o); // compliant when called inside an enum (no member select).
    }
  }

  public boolean isFruitGrape(Fruit candidateFruit) {
    if("".equals(Fruit.GRAPE)) { } // Noncompliant
//        ^^^^^^
    if(Fruit.GRAPE.equals("")) { } // Noncompliant
//                 ^^^^^^
    if(equals(new A())) { }
    return candidateFruit.equals(Fruit.GRAPE); // Noncompliant {{Use "==" to perform this enum comparison instead of using "equals"}}
  }

  public boolean isFruitGrape(Cake candidateFruit) {
    return candidateFruit.equals(Fruit.GRAPE); // Noncompliant {{Use "==" to perform this enum comparison instead of using "equals"}}
  }
  public boolean isFruitGrape(Fruit candidateFruit) {
    return candidateFruit == Fruit.GRAPE; // Compliant; there is only one instance of Fruit.GRAPE - if candidateFruit is a GRAPE it will have the same reference as Fruit.GRAPE
  }

  public boolean isFruitGrape(Cake candidateFruit) {
    return candidateFruit == Fruit.GRAPE; // Compliant: compilation time failure
  }
  public boolean objectIsObject(Object object) {
    return object.equals(object);
  }
}
