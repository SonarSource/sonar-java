package checks;

public class PatternMatchUsingIfCheckSample {


  sealed interface Expr permits Plus, Minus, Const {
  }

  record Plus(Expr lhs, Expr rhs) implements Expr {
  }

  record Minus(Expr lhs, Expr rhs) implements Expr {
  }

  record Const(int value) implements Expr {
  }


  static int badCompute1(Expr expr) {
    if (expr instanceof Plus plus) {   // Noncompliant [[sc=5;ec=7]] {{Replace the chain of if/else with a switch expression.}}
      return badCompute1(plus.lhs) + badCompute1(plus.rhs);
    } else if (expr instanceof Minus(var l, Expr r)) {
      return badCompute1(l) - badCompute1(r);
    } else if (expr instanceof Const) {
      return ((Const) expr).value;
    } else {
      throw new AssertionError();
    }
  }

  static int badCompute2(Expr expr) {
    if (expr instanceof Plus plus && plus.lhs instanceof Const(var z) && z == 0){  // Noncompliant [[sc=5;ec=7]] {{Replace the chain of if/else with a switch expression.}}
      return badCompute2(plus.rhs);
    } else if (expr instanceof Plus plus) {
      return badCompute2(plus.lhs) + badCompute2(plus.rhs);
    } else if (expr instanceof Minus(var l, Expr r)) {
      return badCompute2(l) - badCompute2(r);
    } else if (expr instanceof Const) {
      return ((Const) expr).value;
    } else {
      throw new AssertionError();
    }
  }

  static int goodCompute1(Expr expr) {
    return switch (expr) {
      case Plus(var l, var r) -> goodCompute1(l) + goodCompute1(r);
      case Minus(Expr l, var r) -> goodCompute1(l) - goodCompute1(r);
      case Const(int v) -> v;
    };
  }

  // TODO should we accept this?
  static int goodCompute2(Expr expr){
    if (expr.equals(new Const(0))){
      return 0;
    } else if (expr instanceof Plus plus) {
      return badCompute1(plus.lhs) + badCompute1(plus.rhs);
    } else if (expr instanceof Minus(var l, Expr r)) {
      return badCompute1(l) - badCompute1(r);
    } else if (expr instanceof Const) {
      return ((Const) expr).value;
    } else {
      throw new AssertionError();
    }
  }

  // TODO should we accept this?
  static int goodCompute3(Expr expr){
    if (expr instanceof Plus plus) {
      return badCompute1(plus.lhs) + badCompute1(plus.rhs);
    } else if (expr instanceof Minus(var l, Expr r)) {
      return badCompute1(l) - badCompute1(r);
    } else if (expr.equals(new Const(0))){
      return 0;
    } else if (expr instanceof Const) {
      return ((Const) expr).value;
    } else {
      throw new AssertionError();
    }
  }

  abstract class Animal {
  }

  class Dog extends Animal {
    void bark() {
      System.out.println("Wouf");
    }
  }

  class Cat extends Animal {
  }

  class Snake extends Animal {
  }

  static void badSound1(Animal animal) {
    if (animal instanceof Dog dog) { // Noncompliant [[sc=5;ec=7]] {{Replace the chain of if/else with a switch expression.}}
      dog.bark();
    } else if (animal instanceof Cat) {
      System.out.println("Miaou");
    } else if (animal instanceof Snake) {
      System.out.println("Ssssssssss");
    } else {
      System.out.println("Unknown sound");
    }
  }

  static void badSound2(Animal animal) {
    if (animal instanceof Dog) { // Noncompliant [[sc=5;ec=7]] {{Replace the chain of if/else with a switch expression.}}
      System.out.println("Wouf");
    } else if (animal instanceof Cat) {
      System.out.println("Miaou");
    } else if (animal instanceof Snake) {
      System.out.println("Ssssssssss");
    } else {
      System.out.println("Unknown sound");
    }
  }

  static void goodSound(Animal animal) {
    switch (animal) {
      case Dog dog -> dog.bark();
      case Cat ignored -> System.out.println("Miaou");
      case Snake ignored -> System.out.println("Ssssssssss");
      default -> System.out.println("Unknown sound");
    }
  }

}
