package checks;

public class HiddenFieldCheckSample extends MyBaseClass {

  private int foo;
  public int bar;

  {
    int foo = this.foo; // Noncompliant {{Rename "foo" which hides the field declared at line 5.}}
//      ^^^
    int ok = 0; // Compliant
    System.out.println(foo + ok);
  }

  public HiddenFieldCheckSample(int foo) { // Compliant
    this.foo = foo;
  }

  public void setFoo(int foo) { // Compliant
    this.foo = foo;
  }

  public int setNinja(int foo) { // Compliant
    this.foo = foo;
    return 0;
  }

  public int getFoo() {
    return foo;
  }

  public void method1(int foo) { // Compliant - parameter
    int base1 = 0; // Compliant
    int base2 = 0; // Compliant
    int unrelated = 0; // Compliant
    System.out.println(base1 + base2 + unrelated);
  }

  @Override
  public void method2() {
    MyOtherBaseClass instance = new MyOtherBaseClass() {

      @Override
      public void foo() {
        int bar = 0; // Noncompliant {{Rename "bar" which hides the field declared at line 6.}}
        int otherBase1 = 0; // Compliant - limitation
        System.out.println(bar + otherBase1);
      }

    };

    instance.foo();

  }

  public static class MyInnerClass {

    int bar;
    int myInnerClass1;

    public void foo() {
      int bar = 0; // Noncompliant {{Rename "bar" which hides the field declared at line 58.}}
      System.out.println(bar);
    }

    public class MyInnerInnerClass {

      public void foo() {
        int foo = 0;
        int myInnerClass1 = 0; // Noncompliant
        System.out.println(foo + myInnerClass1);
      }

    }

  }

  public @interface AnnotationType {
    String[] value();
  }
}

class MyBaseClass {

  public int base1;
  private int base2;

  public int getBase2() {
    return base2;
  }

  public void method2() {
    int base1 = 0; // Noncompliant
    int base2 = 0; // Noncompliant
    System.out.println(base1 + base2);
  }

}

abstract class MyOtherBaseClass {

  public int otherBase1;

  public abstract void foo();

}

enum MyEnumHiddenField {
  A, B;

  public void foo() {
    int a = 0;
    System.out.println(a);
  }
}

final class DataUtils {

  public int foo;

  public interface Sortable {

    int size();

    void swap(int foo, int j); // Compliant - parameter

    boolean isLess(int i, int j);

  }

}

class FooHiddenField {

  int i;

  {
    for (i = 0; i < 42; i++) {
    }

  }

  static {
    int i = 0; // Compliant
  }

  static void foo() {
    int i = 0; // Compliant
  }
}

class FieldWithLambda {
  Object p;
  java.util.function.Consumer<String> f = p -> { }; // Noncompliant {{Rename "p" which hides the field declared at line 153.}}
}
