import java.util.List;
import java.util.Set;

class Foo {
  
  void foo() {
    int a = 0;                   // Compliant
    a = 0;                       // Compliant
    System.out.println(a);       // Compliant
    System.out.println(a = 0);   // Noncompliant [[sc=26;ec=27]] {{Extract the assignment out of this expression.}}
    System.out.println(a += 0);  // Noncompliant [[sc=26;ec=28]] {{Extract the assignment out of this expression.}}
    System.out.println(a == 0);  // Compliant

    a = b = 0;                   // Compliant
    a += foo[i];                 // Compliant

    _stack[
           index = 0             // Noncompliant
           ] = node;

    while ((foo = bar()) != null) { // Compliant
    }

    if ((plop = something) != null) { // Compliant
    }

    if ((a = b = 0) != null) { // Noncompliant
    }

    while ((foo = bar()) == null) { // Compliant
    }

    while ((foo = bar()) <= 0) { // Compliant
    }

    while ((foo = bar()) < 0) { // Compliant
    }

    while ((foo = bar()) >= 0) { // Compliant
    }

    while ((foo = bar()) > 0) { // Compliant
    }

    while ((a = foo()).foo != 0) { // Compliant
    }

    while ((a += 0) > 42) { // Compliant
    }

    a + 0;
    (a = foo()) + 5; // Noncompliant

    while (null != (foo = bar())) { // Compliant
    }
  }

  boolean field;

  @MyAnnotation(name="toto", type=Type.SubType) // Compliant
  void bar(){
    eventBus.register((NextPlayer) event -> field = !field);
    eventBus.register((NextPlayer) event -> {field = !field;});
    eventBus.register((NextPlayer) event -> {if(field = !field) return false;}); // Noncompliant
  }

  void sonarJava1516() {
    Set<Integer> ids;
    while ((ids = getNextIds()).size() > 0) { // Compliant
      log.info("Result: {}", ids);
    }
  }

  void sonarJava1516_bis(List<Integer> ids) {
    Integer a;
    while (!ids.isEmpty()) {
      int x = (a = ids.remove(0)) + 5; // Noncompliant
    }
  }

  void sonarJava2193() {
    int i = j = 0; // Compliant
    int l = i;
    int k = (l += 1); // Compliant
    double a = b = c = defaultValue();
    Object[] result;
    result = (bresult = new byte[len]);
    char[] buf = lineBuffer = new char[128];
  }

}
