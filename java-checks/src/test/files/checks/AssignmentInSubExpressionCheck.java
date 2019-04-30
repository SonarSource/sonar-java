import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

abstract class Foo {

  int b, i, index, node;
  Integer plop, something;
  int[] foo, _stack;
  Foo obj;

  @interface MyAnnotation {
    String value();
  }

  Foo foo() {
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

    if ((a = b = 0) != 1) { // Noncompliant
    }

    while ((foo = bar()) == null) { // Compliant
    }

    while ((foo = bar())[0] <= 0) { // Compliant
    }

    while ((foo = bar())[0] < 0) { // Compliant
    }

    while ((foo = bar())[0] >= 0) { // Compliant
    }

    while ((foo = bar())[0] > 0) { // Compliant
    }

    while ((obj = foo()).index != 0) { // Compliant
    }

    while ((a += 0) > 42) { // Compliant
    }

    i = a + 0;
    i = (a = bar()[0]) + 5; // Noncompliant

    while (null != (foo = bar())) { // Compliant
    }

    if ((a += b) > 0) { // Noncompliant
    }

    return null;
  }

  boolean field;
  EventBus eventBus;

  @MyAnnotation(value = "toto") // Compliant
  int[] bar() {
    eventBus.register(event -> field = !field);
    eventBus.register(event -> { field = !field; });
    eventBus.register(event -> { if(field = !field) return; }); // Noncompliant
    return null;
  }

  interface EventBus {
    void register(Consumer<Object> test);
  }

  void sonarJava1516() {
    Set<Integer> ids;
    while ((ids = getNextIds()).size() > 0) { // Compliant
      defaultValue();
    }
  }

  abstract Set<Integer> getNextIds();

  void sonarJava1516_bis(List<Integer> ids) {
    Integer a;
    while (!ids.isEmpty()) {
      int x = (a = ids.remove(0)) + 5; // Noncompliant
    }
  }

  int j, c, len;
  byte[] bresult;
  char[] lineBuffer;

  void sonarJava2193() {
    int i = j = 0; // Compliant
    int l = i;
    int k = (l += 1); // Compliant
    double a = b = c = defaultValue();
    byte[] result;
    result = (bresult = new byte[len]);
    char[] buf = lineBuffer = new char[128];
  }

  abstract int defaultValue();

  class SonarJava2821 {
    private Integer field = 0;

    void fun(List<Integer> list) {
      list.forEach(e -> field += e); // Compliant : ignore assignment expression in lambda
      list.forEach(e -> SonarJava2821.this.field &= e); // Compliant : ignore assignment expression in lambda
      list.forEach(e -> field = field + e); // Compliant : ignore assignment expression in lambda
    }
  }
}
