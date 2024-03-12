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

  static void badSound1(Animal animal) {
    if (animal instanceof Dog dog) { // Noncompliant [[sc=5;ec=7]] {{Replace the chain of if/else with a switch expression.}}
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
