import java.util.Arrays;

class A {
  void fun() {

    IntStream.range(1, 5)
        .map((x) -> x * x)
        .map(x -> square(x)) // Noncompliant {{Replace this lambda with a method reference.}}
        .map(x -> { // Noncompliant
          return square(x);
        })
        .map(this::square) //Compliant
        .forEach(System.out::println);
    IntStream.range(1, 5)
        .forEach(x -> System.out.println(x)); // Noncompliant
    IntStream.range(1, 5)
        .forEach(x -> { // Noncompliant
          System.out.println(x);
        });
    IntStream.range(1, 5).forEach(x -> {return;});
  }

  int square(int x) {
    return x * x;
  }
}
