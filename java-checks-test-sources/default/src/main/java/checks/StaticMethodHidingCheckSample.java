package checks;

import java.util.List;

class StaticMethodHidingCheckSample {

  // --- Basic hiding ---

  static class Animal {
    static void describe() {
    }
  }

  static class Dog extends Animal {
    static void describe() { // Noncompliant {{Rename this method; it hides "describe" in "Animal".}}
//              ^^^^^^^^
    }
  }

  // --- Hiding with parameters ---

  static class Formatter {
    static String format(String input) {
      return input;
    }

    static String format(String input, int width) {
      return input;
    }
  }

  static class CustomFormatter extends Formatter {
    static String format(String input) { // Noncompliant {{Rename this method; it hides "format" in "Formatter".}}
      return input.trim();
    }

    static String format(String input, int width) { // Noncompliant {{Rename this method; it hides "format" in "Formatter".}}
      return input.trim();
    }
  }

  // --- Deep hierarchy hiding ---

  static class Base {
    static void process() {
    }
  }

  static class Middle extends Base {
    static void process() { // Noncompliant {{Rename this method; it hides "process" in "Base".}}
    }
  }

  static class Leaf extends Middle {
    static void process() { // Noncompliant {{Rename this method; it hides "process" in "Middle".}}
    }
  }

  // --- Compliant: different parameter types (overloading, not hiding) ---

  static class ParentA {
    static void compute(int x) {
    }
  }

  static class ChildA extends ParentA {
    static void compute(String x) { // Compliant - different param type
    }
  }

  // --- Compliant: different parameter count ---

  static class ParentB {
    static void compute(int x) {
    }
  }

  static class ChildB extends ParentB {
    static void compute(int x, int y) { // Compliant - different param count
    }
  }

  // --- Compliant: instance method overriding ---

  static class ParentC {
    void display() {
    }
  }

  static class ChildC extends ParentC {
    @Override
    void display() { // Compliant - instance method override
    }
  }

  // --- Compliant: no inheritance ---

  static class Unrelated1 {
    static void helper() {
    }
  }

  static class Unrelated2 {
    static void helper() { // Compliant - no inheritance relationship
    }
  }

  // --- Compliant: renamed methods ---

  static class ParentD {
    static void calculate() {
    }
  }

  static class ChildD extends ParentD {
    static void calculateExtended() { // Compliant - different name
    }
  }

  // --- Compliant: private superclass method ---

  static class ParentE {
    private static void internalHelper() {
    }
  }

  static class ChildE extends ParentE {
    static void internalHelper() { // Compliant - parent method is private
    }
  }

  // --- Compliant: interface static methods (not inherited) ---

  interface Printable {
    static void print() {
    }
  }

  static class Printer implements Printable {
    static void print() { // Compliant - interface static methods are not inherited
    }
  }

  // --- Generics: hiding with generic parameter types ---

  static class GenericParent<T> {
    static void transform(List<String> items) {
    }
  }

  static class GenericChild extends GenericParent<Integer> {
    static void transform(List<String> items) { // Noncompliant {{Rename this method; it hides "transform" in "GenericParent".}}
    }
  }

  // --- Compliant: static method in child, instance in parent ---

  static class ParentF {
    void action() {
    }
  }

  static class ChildF extends ParentF {
    static void action() { // Compliant - parent method is not static
    }
  }

  // --- Compliant: instance method in child, static in parent (covered by S2177) ---

  static class ParentG {
    static void action() {
    }
  }

  static class ChildG extends ParentG {
    void action() { // Compliant for this rule - covered by S2177
    }
  }

  // --- Hiding through intermediate class that doesn't define the method ---

  static class GrandParent {
    static void legacy() {
    }
  }

  static class IntermediateParent extends GrandParent {
    // does not override legacy()
  }

  static class GrandChild extends IntermediateParent {
    static void legacy() { // Noncompliant {{Rename this method; it hides "legacy" in "GrandParent".}}
    }
  }

  // --- Multiple methods hiding from the same parent ---

  static class MultiParent {
    static void first() {
    }

    static void second() {
    }
  }

  static class MultiChild extends MultiParent {
    static void first() { // Noncompliant {{Rename this method; it hides "first" in "MultiParent".}}
    }

    static void second() { // Noncompliant {{Rename this method; it hides "second" in "MultiParent".}}
    }
  }
}
