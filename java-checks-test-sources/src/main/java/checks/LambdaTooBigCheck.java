package checks;

import java.util.concurrent.Callable;

class LambdaTooBigCheckA {

  Callable<Integer> c1 = () -> { // Noncompliant [[sc=26;ec=31;secondary=17]] {{Reduce this lambda expression number of lines from 11 to at most 10.}}
    System.out.println();
    System.out.println();
    System.out.println();
    System.out.println();
    System.out.println();
    System.out.println();
    System.out.println();
    System.out.println();
    return 1;
  };

  Callable<Integer> c2 = () -> {
    System.out.println();
    return 42;
  };

  Callable<Integer> c3 = () -> 1 + 2 +
    2 +
    3 * 4 +
    5 +
    3 +
    1 +
    1;
}
