class Foo {
  void foo() {
    int a = 0;                   // Compliant
    a = 0;                       // Compliant
    System.out.println(a);       // Compliant
    System.out.println(a = 0);   // Noncompliant {{Extract the assignment out of this expression.}}
    System.out.println(a += 0);  // Noncompliant {{Extract the assignment out of this expression.}}
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

    while ((a = foo()).foo != 0) { // Noncompliant
    }

    while ((a += 0) > 42) { // Noncompliant
    }

    a + 0;
    (a = foo()) + 5; // Noncompliant

    while (null != (foo = bar())) { // Compliant
    }
  }
  @MyAnnotation(name="toto", type=Type.SubType) // Compliant
  void bar(){
    eventBus.register((NextPlayer) event -> isPlaying = !isPlaying);
    eventBus.register((NextPlayer) event -> {isPlaying = !isPlaying;});
    eventBus.register((NextPlayer) event -> {if(isPlaying = !isPlaying) return false;}); // Noncompliant
  }

}
