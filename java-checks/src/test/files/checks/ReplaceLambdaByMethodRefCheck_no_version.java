import java.util.Arrays;

class A {
  void fun() {

    IntStream.range(1, 5)
        .map((x) -> x * x)
        .map(x -> square(x)) // Noncompliant {{Replace this lambda with a method reference. (sonar.java.source not set. Assuming 8 or greater.)}}
        .map(x -> { // Noncompliant
          return square(x);
        })
        .map(this::square) //Compliant
        .forEach(System.out::println);
    IntStream.range(1, 5).forEach(x -> System.out.println(x)); // Noncompliant
    IntStream.range(1, 5).forEach(x -> { // Noncompliant
          System.out.println(x);
        });
    IntStream.range(1, 5).forEach(x -> {return;}); // Compliant
    
    Arrays.asList("bar").stream().filter(string -> string.startsWith("b")); // Compliant
    Arrays.asList(new A()).stream().filter(a -> a.coolerThan(0, a)); // Compliant
  }

  int square(int x) {
    return x * x;
  }
  
  boolean coolerThan(int i, A a) {
    return true;
  }
}
