package checks;

import java.util.Optional;

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

  class GetterWithOtherReturnType { // Compliant, getter does not have the same return type as the default one in record
    private final String bar;

    public GetterWithOtherReturnType(String bar) {
      this.bar = bar;
    }

    public Optional<String> bar() { // Not the same type as the field bar.
      return Optional.of(bar);
    }
  }

  // When the constructor has smaller visibility, it is not possible to create a record with the same behavior.
  // Order: Public > protected > package private > private

  public class ClassWithPublicConstructor { // Noncompliant
    private final int sum;
    public ClassWithPublicConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  public class ClassWithProtectedConstructor { // Compliant, constructor visibility is smaller than public
    private final int sum;
    protected ClassWithProtectedConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  public class ClassWithPackagePrivateConstructor { // Compliant, constructor visibility is smaller than public
    private final int sum;
    ClassWithPackagePrivateConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  public class ClassWithPrivateConstructor { // Compliant, constructor visibility is smaller than public
    private final int sum;
    private ClassWithPrivateConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  protected class ProtectedClassWithPublicConstructor { // Noncompliant
    private final int sum;
    public ProtectedClassWithPublicConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  protected class ProtectedClassWithProtectedConstructor { // Noncompliant
    private final int sum;
    protected ProtectedClassWithProtectedConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  protected class ProtectedClassWithPackagePrivateConstructor { // Compliant
    private final int sum;
    ProtectedClassWithPackagePrivateConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  protected class ProtectedClassWithPrivateConstructor { // Compliant
    private final int sum;
    private ProtectedClassWithPrivateConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  class PackagePrivateClassWithPublicConstructor { // Noncompliant
    private final int sum;
    public PackagePrivateClassWithPublicConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  class PackagePrivateClassWithProtectedConstructor { // Noncompliant
    private final int sum;
    protected PackagePrivateClassWithProtectedConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  class PackagePrivateClassWithPackagePrivateConstructor { // Noncompliant
    private final int sum;
    PackagePrivateClassWithPackagePrivateConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  class PackagePrivateClassWithPrivateConstructor { // Compliant
    private final int sum;
    private PackagePrivateClassWithPrivateConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  private class PrivateClassWithPublicConstructor { // Noncompliant
    private final int sum;
    public PrivateClassWithPublicConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  private class PrivatelassWithPrivateConstructor { // Noncompliant
    private final int sum;
    private PrivatelassWithPrivateConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }
}
