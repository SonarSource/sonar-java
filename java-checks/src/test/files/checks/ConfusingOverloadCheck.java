public class Parent {
  public static int fieldMethod;

  public void doSomething(Computer.Pear p) {
  }
  public static void staticDifference(int i) {
  }

  private void privateMethod(){}

}

public class Child extends Parent {

  public void doSomething(Fruit.Pear p) {  // Noncompliant [[sc=15;ec=26]] {{Rename this method or correct the type of the argument(s) to override the parent class method.}}
  }
  public void doSomething(Fruit.Apple a) {}
  public void doSomething(Fruit.Pear p, int i) {
  }
  public void fieldMethod() {}

  public void staticDifference(int i) {  // Noncompliant {{Rename this method or make it "static".}}
  }
  public void staticDifference() {

  }
  private void privateMethod(){} // Noncompliant {{Rename this method; there is a "private" method in the parent class with the same name.}}
}
class ChildBis extends Parent {}
class ChildBisBis extends ChildBis {
  public static void staticDifference(int i) {
  }
  private void privateMethod(){} // Compliant, we only check the first parent
}

class ChildTer extends ChildBisBis {
  public void staticDifference(int i) {  // Noncompliant {{Rename this method or make it "static".}}
  }

}

public class Parent2 {

  public void doSomething(Computer.Pear p) {
  }

  public static void staticDifference() {
  }

  private void writeObject() {}
}

public class Child2 extends Parent2 {

  public void doSomething(Computer.Pear p) {  // true override
  }

  public static void staticDifference() {
  }
  private void writeObject() {}
}
public class Child3 extends ParentUnkown {
  public void doSomething(Computer.Pear p) {}
}
public @interface ConstructorProperties {
  int value();
}
class Computer{
  static class Pear{}
}
class Fruit{
  static class Pear{}
  static class Apple{}
}


class UnknownParam {
  private static int method(UNKNOWN arg) {

  }
}
class UnknownParamChild extends UnknownParam {
  protected int method(UNKNOWN arg) {

  }
}
