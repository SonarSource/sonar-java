package checks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

class LambdaTooBigCheckCustom {

  private void f() {
    Callable<Integer> c0 = () -> {
      return 1;
    };

    Callable<Integer> c1 = () -> { // Noncompliant {{Reduce this lambda expression number of lines from 8 to at most 6.}}
//                         ^^^^^
      System.out.println();
      System.out.println();
      System.out.println();
      System.out.println();
      System.out.println();
      return 1;
    };
//  ^<


    Callable<Integer> c2 = () -> 1 + 2;
    Callable<Integer> c3 = () -> 1 + 2 + // Noncompliant {{Reduce this lambda expression number of lines from 7 to at most 6.}}
//                         ^^^^^
      2 +
      3 * 4 +
      5 +
      3 +
      1 +
      1;
//    ^<

    Runnable r2 = () -> System.out.println("Hello world two!");

    Predicate<Person> allDraftees =
      p ->
        p.getAge() >= 18
          && p.getAge() <= 25
          && p.isMale() == true;

    List<Person> pl = Person.createShortList();
    pl.forEach(p -> {
      System.out.println();
      System.out.println("Name: " + p.getName() +
        "Age: " + p.getAge() + "is male: " + p.isMale());
    });
  }

  private static class Person {

    String name;
    int age;
    boolean isMale;

    public static List<Person> createShortList() {
      return new ArrayList<>();
    }

    public int getAge() {
      return age;
    }

    public boolean isMale() {
      return isMale;
    }

    public String getName() {
      return name;
    }
  }
}
