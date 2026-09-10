package checks;

import java.util.stream.IntStream;

public class LambdaSingleExpressionCheckNoVersionSample {
  public void method() {
    IntStream.range(1, 5).map(x -> x * x - 1).forEach(x -> System.out.println(x));
    IntStream.range(1, 5).map(x -> {return x * x - 1;}) // Noncompliant {{Remove useless curly braces around statement and then remove useless return keyword (sonar.java.source not set. Assuming 8 or greater.)}}
//                                 ^
        .forEach(x -> { // Compliant - lambda body spans multiple lines, block form is kept for readability
          System.out.println(x + 11);
        });
    IntStream.range(1, 5).map(x -> { // Compliant - non-expression statement
      if (x % 2 == 0) return 0;
      else return 1;
    });
    IntStream.range(1, 5).forEach(x -> {
      try {
      x = x/0;
      } catch (Exception e) {
        System.out.println(x);
      }
    });
    IntStream.range(1, 5).forEach(x -> {
      while(true) {
      }
    });
  }

}
