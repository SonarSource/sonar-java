package checks;

import java.util.Objects;

public class PatternMatchUsingIfCheckSample {


  sealed interface Expr permits Plus, Minus, Const {
  }

  record Plus(Expr lhs, Expr rhs) implements Expr {
  }

  record Minus(Expr lhs, Expr rhs) implements Expr {
  }

  record Const(int value) implements Expr {
  }


  // fix@qf1 {{Replace the chain of if/else with a switch expression.}}
  // edit@qf1 [[sl=+0;el=+9;sc=5;ec=6]] {{switch (expr) {\n      case Plus plus -> {\n        return badCompute1(plus.lhs) + badCompute1(plus.rhs);\n      }\n      case Minus(var l, Expr r) -> {\n        return badCompute1(l) - badCompute1(r);\n      }\n      case Const ignored -> {\n        return ((Const) expr).value;\n      }\n      default -> {\n        throw new AssertionError();\n      }\n    }}}
  static int badCompute1(Expr expr) {
    // Noncompliant@+1 [[sl=+1;el=+1;sc=5;ec=7;quickfixes=qf1]]
    if (expr instanceof Plus plus) {
      return badCompute1(plus.lhs) + badCompute1(plus.rhs);
    } else if (expr instanceof Minus(var l, Expr r)) {
      return badCompute1(l) - badCompute1(r);
    } else if (expr instanceof Const) {
      return ((Const) expr).value;
    } else {
      throw new AssertionError();
    }
  }

  // fix@qf2 {{Replace the chain of if/else with a switch expression.}}
  // edit@qf2 [[sl=+0;el=+11;sc=5;ec=6]] {{switch (expr) {\n      case Plus plus when plus.lhs instanceof Const(var z) && z == 0 -> {\n            return badCompute1(plus.lhs) + badCompute1(plus.rhs);\n      }\n      case Plus plus -> {\n            return badCompute1(plus.lhs) + badCompute1(plus.rhs);\n      }\n      case Minus(var l, Expr r) -> {\n            return badCompute1(l) - badCompute1(r);\n      }\n      case Const ignored -> {\n            return ((Const) expr).value;\n      }\n      default -> {\n            throw new AssertionError();\n      }\n}}}
  static int badCompute2(Expr expr) {
    // Noncompliant@+1 [[sl=+1;el=+1;sc=5;ec=7;quickfixes=qf2]] {{Replace the chain of if/else with a switch expression.}}
    if (expr instanceof Plus plus && plus.lhs instanceof Const(var z) && z == 0){
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

  static abstract class Animal {
    private String name = "?";
  }

  static abstract class WalkingAnimal extends Animal {
    void walk(){}
  }

  class Dog extends WalkingAnimal {
    void bark() {
      System.out.println("Wouf");
    }
  }

  static class Cat extends WalkingAnimal {
  }

  class Snake extends Animal {
  }

  // fix@qf3 {{Replace the chain of if/else with a switch expression.}}
  // edit@qf3 [[sl=+0;el=+9;sc=5;ec=6]] {{switch (animal){\n      case Dog dog -> {\n        dog.bark();\n      }\n      case Cat ignored -> {\n        System.out.println("Meow");\n      }\n      case Snake ignored -> {\n        System.out.println("Ssssssssss");\n      }\n      default -> {\n        System.out.println("Unknown sound");\n      }\n    }}}
  static void badSound1(Animal animal) {
    // Noncompliant@+1 [[sl=+1;el=+1;sc=5;ec=7;quickfixes=qf3]]
    if (animal instanceof Dog dog) {
      dog.bark();
    } else if (animal instanceof Cat) {
      System.out.println("Meow");
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
      System.out.println("Meow");
    } else if (animal instanceof Snake) {
      System.out.println("Ssssssssss");
    } else {
      System.out.println("Unknown sound");
    }
  }

  static void badSound3(Animal animal) {
    if (animal instanceof Dog && !Objects.equals(animal.name, "?") && animal.name.length() < 10) { // Noncompliant [[sc=5;ec=7]] {{Replace the chain of if/else with a switch expression.}}
      System.out.println("Wouf");
    } else if (animal instanceof Cat) {
      System.out.println("Meow");
    } else if (animal instanceof Snake) {
      System.out.println("Ssssssssss");
    } else {
      System.out.println("Unknown sound");
    }
  }

  static void goodSound1(Animal animal) {
    switch (animal) {
      case Dog dog -> dog.bark();
      case Cat ignored -> System.out.println("Meow");
      case Snake ignored -> System.out.println("Ssssssssss");
      default -> System.out.println("Unknown sound");
    }
  }

  static void goodSound2(Animal animal){
    if (animal instanceof Dog dog){
      dog.bark();
    } else {
      System.out.println("Unknown sound");
    }
  }

  static void goodSound3(){ // Compliant because we are not sure that getAnimal always returns the same specie
    if (getAnimal() instanceof Dog dog){
      dog.bark();
    } else if (getAnimal() instanceof Cat){
      System.out.println("Meow");
    } else if (getAnimal() instanceof Snake){
      System.out.println("Ssssssssss");
    } else {
      System.out.println("Unknown sound");
    }
  }

  static void badSoundMin(Animal animal){
    if (animal instanceof Dog dog){ // Noncompliant [[sc=5;ec=7]] {{Replace the chain of if/else with a switch expression.}}
      dog.bark();
    } else if (animal instanceof Cat){
      System.out.println("Meow");
    } else {
      System.out.println("Unknown sound");
    }
  }

  static void goodSound2Args(Animal animal1, Animal animal2) {
    if (animal1 instanceof Dog dog){
      dog.bark();
    } else if (animal2 instanceof Dog dog){
      dog.bark();
    } else if (animal1 instanceof Cat){
      System.out.println("Meow");
    } else {
      System.out.println("Unknown sound");
    }
  }

  static void badSound2Args(Animal animal1, Animal animal2) {
    if (animal1 instanceof Dog dog1 && animal2 instanceof Dog dog2){  // Noncompliant [[sc=5;ec=7]] {{Replace the chain of if/else with a switch expression.}}
      dog1.bark();
      dog2.bark();
    } else if (animal1 instanceof Cat){
      System.out.println("Meow");
    } else {
      System.out.println("Unknown sound");
    }
  }

  static void bar(WalkingAnimal animal, int x, int y){
    if (x < y){
      animal.walk();
    } else if (animal instanceof Dog dog){
      dog.bark();
    } else {
      System.out.println("Hello world");
    }
  }

  static Animal getAnimal(){
    return new Cat();
  }

}
