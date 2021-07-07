package checks;

public class RecordInsteadOfClassCheck {
  class SimpleClass implements NotAClass { // Noncompliant [[sc=9;ec=20]] {{Refactor this class declaration to use 'record SimpleClass(int sum)'.}}
    private final int sum;
    public static final int VALUE = 42;

    SimpleClass(int sum) { this.sum = sum; }
    int getSum() { return sum; }
    @Override public void foo() { }
  }

  class SimpleClass2 { // Noncompliant [[sc=9;ec=21]] {{Refactor this class declaration to use 'record SimpleClass2(boolean boom, int a)'.}}
    private final boolean boom;
    private final int a;

    SimpleClass2(boolean boom, int a) { this.boom = boom; this.a = a; }
    boolean boom() { return boom; }
    int getA() { return a; }
    void get() { }
    void is() { }
  }

  class ComplexClass { // Noncompliant {{Refactor this class declaration to use 'record ComplexClass(int sum, List<...> list, boolean[][] tests, boo...)'.}}
    private final int sum;
    private final java.util.List<String> list;
    private final boolean[][] tests;
    private final ComplexClass[] classes;
    private final boolean colorBlind;

    ComplexClass(int sum, java.util.List<String> list, boolean[][] tests, boolean colorBlind, ComplexClass ... classes) {
      this.sum = sum;
      this.list = list;
      this.tests = tests;
      this.classes = classes;
      this.colorBlind = colorBlind;
    }

    int getSum() { return sum; }
    java.util.List<String> getList() { return list; }
    boolean[][] getTests() { return tests; }
    ComplexClass[] getClasses() { return classes; }
    boolean isColorBlind() { return colorBlind; }
  }

  class SimpleWithoutGettersForAllFields {
    private final int sum;
    private final int base;

    SimpleWithoutGettersForAllFields(int sum, int base) { this.sum = sum; this.base = base; }
    int getSum() { return sum; }
  }

  class SimpleWithConstructorNotSettingAllParameters {
    private final int sum;
    private final int base;

    SimpleWithConstructorNotSettingAllParameters(int sum) { this.sum = sum; base = 42; }
    int getSum() { return sum; }
    int getBase() { return base; }
  }

  class SimpleWithConstructorNotSettingAllParameters2 {
    private final int sum;
    private final int base;

    SimpleWithConstructorNotSettingAllParameters2(int sum, String other) { this.sum = sum; base = 42; }
    int getSum() { return sum; }
    int getBase() { return base; }
  }

  class ClassWithNoRealSetter { // Noncompliant {{Refactor this class declaration to use 'record ClassWithNoRealSetter(int sum)'.}}
    private final int sum;

    ClassWithNoRealSetter(int sum) { this.sum = sum; }
    int getSum() { return sum; }
    void setValue(int value) { }
    void setSum() { }
    int getValue() { return 0; }
  }

  class ClassWithNoRealGetter {
    private final int sum;

    ClassWithNoRealGetter(int sum) { this.sum = sum; }
    int getSum(int expected) { return sum; }
  }

  class DefaultConstructorClass {
    private final int sum = 42;

    int getSum() { return sum; }
  }

  class ClassExtendingClass extends SimpleClass { ClassExtendingClass() { super(0); } }
  class ClassWithStaticField { private static int base; }
  class ClassWithPrivateNonFinalField { private int base; }
  class ClassWithPublicFinalField { public final int base = 0; }
  class ClassWithoutFields { }
  class ClassWithoutFinalFields { private int sum; }
  abstract class AbstractClass { abstract void foo(); }
  interface NotAClass { void foo(); }

  record SimpleRecord(int sum, int base) {
    SimpleRecord(int sum, int base) { this.sum = sum; this.base = base; }
    int getSum() { return sum; }
    int getBase() { return base; }
  }
}
