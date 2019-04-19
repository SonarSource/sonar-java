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
    if("".equals(Fruit.GRAPE)) { } // Noncompliant [[sc=11;ec=17]]
    if(Fruit.GRAPE.equals("")) { } // Noncompliant [[sc=20;ec=26]]
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
