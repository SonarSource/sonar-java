package checks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class InappropriateCastCheckSample {

  interface Animal {}
  interface Vehicle {}
  interface Drawable {}

  static class Dog implements Animal {}
  static final class FinalDog implements Animal {}
  static class Car implements Vehicle {}
  static class Circle extends Shape implements Drawable {}
  static class Shape {}
  static abstract class AbstractShape {}

  enum Color { RED, GREEN, BLUE }
  enum Size { SMALL, MEDIUM, LARGE }

  // Noncompliant: unrelated concrete classes
  void unrelatedConcreteClasses(Dog dog, Car car, String str) {
    Car c = (Car) dog; // Noncompliant {{"Dog" cannot be cast to "Car" without a risk of "ClassCastException".}}
    Dog d = (Dog) car; // Noncompliant {{"Car" cannot be cast to "Dog" without a risk of "ClassCastException".}}
    Dog d2 = (Dog) str; // Noncompliant {{"String" cannot be cast to "Dog" without a risk of "ClassCastException".}}
  }

  // Noncompliant: final class to unrelated interface
  void finalClassToUnrelatedInterface(FinalDog finalDog, String str) {
    Vehicle v = (Vehicle) finalDog; // Noncompliant {{"FinalDog" cannot be cast to "Vehicle" without a risk of "ClassCastException".}}
    Drawable d = (Drawable) str; // Noncompliant {{"String" cannot be cast to "Drawable" without a risk of "ClassCastException".}}
  }

  // Noncompliant: interface to unrelated final class
  void interfaceToUnrelatedFinalClass(Vehicle vehicle, Drawable drawable) {
    FinalDog fd = (FinalDog) vehicle; // Noncompliant {{"Vehicle" cannot be cast to "FinalDog" without a risk of "ClassCastException".}}
    String s = (String) drawable; // Noncompliant {{"Drawable" cannot be cast to "String" without a risk of "ClassCastException".}}
  }

  // Noncompliant: enums are implicitly final
  void enumCasts(Color color, Size size) {
    Size s = (Size) color; // Noncompliant {{"Color" cannot be cast to "Size" without a risk of "ClassCastException".}}
    Drawable d = (Drawable) color; // Noncompliant {{"Color" cannot be cast to "Drawable" without a risk of "ClassCastException".}}
  }

  // Compliant: upcast (subtype to supertype)
  void upcast(Circle circle, Dog dog) {
    Shape s = (Shape) circle; // Compliant
    Object o = (Object) dog; // Compliant
    Animal a = (Animal) dog; // Compliant
  }

  // Compliant: downcast along hierarchy
  void downcast(Shape shape, Animal animal) {
    Circle c = (Circle) shape; // Compliant
    Dog d = (Dog) animal; // Compliant
  }

  // Compliant: cast to/from Object
  void objectCasts(Object obj, Dog dog) {
    Dog d = (Dog) obj; // Compliant
    Object o = (Object) dog; // Compliant
  }

  // Compliant: cast between interfaces
  void interfaceCasts(Animal animal, Vehicle vehicle) {
    Vehicle v = (Vehicle) animal; // Compliant
    Animal a = (Animal) vehicle; // Compliant
  }

  // Compliant: non-final class to unrelated interface
  void nonFinalClassToInterface(Dog dog, Shape shape) {
    Vehicle v = (Vehicle) dog; // Compliant
    Serializable s = (Serializable) shape; // Compliant
  }

  // Compliant: cast involving generics/wildcards
  void genericCasts(List<?> wildcardList, List<Integer> intList) {
    List<String> strList = (List<String>) wildcardList; // Compliant
    ArrayList<Integer> al = (ArrayList<Integer>) intList; // Compliant
  }

  // Compliant: instanceof guard (still a valid downcast along hierarchy)
  void instanceofGuard(Object obj) {
    if (obj instanceof String) {
      String s = (String) obj; // Compliant
    }
  }

  // Compliant: cast to related interface (class implements interface)
  void relatedInterfaceCast(Circle circle) {
    Drawable d = (Drawable) circle; // Compliant
  }

  // Compliant: abstract class to interface
  void abstractClassToInterface(AbstractShape abstractShape) {
    Drawable d = (Drawable) abstractShape; // Compliant
  }

  // Compliant: cast involving type variables
  <T> void typeVariableCast(T obj) {
    String s = (String) obj; // Compliant
  }

  // Compliant: primitive casts
  void primitiveCast(int i) {
    long l = (long) i; // Compliant
    double d = (double) i; // Compliant
  }

}
