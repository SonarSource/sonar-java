package checks;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.Optional;

public class RecordInsteadOfClassCheckSample {
  public final class DefinitelyFinal { // Noncompliant
    private final int i;

    public DefinitelyFinal(final int i) {
      this.i = i;
    }

    public int getI() {
      return i;
    }
  }

  final class SimpleClass implements NotAClass { // Noncompliant {{Refactor this class declaration to use 'record SimpleClass(int sum)'.}}
//            ^^^^^^^^^^^
    private final int sum;
    public static final int VALUE = 42;

    SimpleClass(int sum) { this.sum = sum; }
    int getSum() { return sum; }

    @Override public void foo() { }
  }

  final class SimpleClass2 { // Noncompliant {{Refactor this class declaration to use 'record SimpleClass2(boolean boom, int a)'.}}
//            ^^^^^^^^^^^^
    private final boolean boom;
    private final int a;

    SimpleClass2(boolean boom, int a) { this.boom = boom; this.a = a; }
    boolean boom() { return boom; }
    int getA() { return a; }
    void get() { }
    void is() { }
  }

  final class ComplexClass { // Noncompliant {{Refactor this class declaration to use 'record ComplexClass(int sum, List<...> list, boolean[][] tests, boo...)'.}}
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

  public class NonFinal { // Compliant as it could potentially be inherited from
    private final int i;

    public NonFinal(final int i) {
      this.i = i;
    }

    public int getI() {
      return i;
    }
  }

  public class ASuperClass { // Compliant as it is inherited from
    private final int i;

    public ASuperClass(final int i) {
      this.i = i;
    }

    public int getI() {
      return i;
    }
  }

  public final class B extends ASuperClass { // Compliant as it inherits
    public B(final int j) {
      super(j);
    }
  }


  final class SimpleWithoutGettersForAllFields {
    private final int sum;
    private final int base;

    SimpleWithoutGettersForAllFields(int sum, int base) { this.sum = sum; this.base = base; }
    int getSum() { return sum; }
  }

  final class SimpleWithConstructorNotSettingAllParameters {
    private final int sum;
    private final int base;

    SimpleWithConstructorNotSettingAllParameters(int sum) { this.sum = sum; base = 42; }
    int getSum() { return sum; }
    int getBase() { return base; }
  }

  final class SimpleWithConstructorNotSettingAllParameters2 {
    private final int sum;
    private final int base;

    SimpleWithConstructorNotSettingAllParameters2(int sum, String other) { this.sum = sum; base = 42; }
    int getSum() { return sum; }
    int getBase() { return base; }
  }

  final class ClassWithNoRealSetter { // Noncompliant {{Refactor this class declaration to use 'record ClassWithNoRealSetter(int sum)'.}}
    private final int sum;

    ClassWithNoRealSetter(int sum) { this.sum = sum; }
    int getSum() { return sum; }
    void setValue(int value) { }
    void setSum() { }
    int getValue() { return 0; }
  }

  final class ClassWithNoRealGetter {
    private final int sum;

    ClassWithNoRealGetter(int sum) { this.sum = sum; }
    int getSum(int expected) { return sum; }
  }

  final class DefaultConstructorClass {
    private final int sum = 42;

    int getSum() { return sum; }
  }

  final class ClassExtendingClass extends ASuperClass { ClassExtendingClass() { super(0); } }
  final class ClassWithStaticField { private static int base; }
  final class ClassWithPrivateNonFinalField { private int base; }
  final class ClassWithPublicFinalField { public final int base = 0; }
  final class ClassWithoutFields { }
  final class ClassWithoutFinalFields { private int sum; }
  Object anonymousClass = new Object() {
    private final int sum = 0;
    int getSum() { return sum; }
  };
  abstract class AbstractClass { abstract void foo(); }
  interface NotAClass { void foo(); }

  record SimpleRecord(int sum, int base) {
    SimpleRecord(int sum, int base) { this.sum = sum; this.base = base; }
    int getSum() { return sum; }
    int getBase() { return base; }
  }

  final class GetterWithOtherReturnType { // Compliant, getter does not have the same return type as the default one in record
    private final String bar;

    public GetterWithOtherReturnType(String bar) {
      this.bar = bar;
    }

    public Optional<String> bar() { // Not the same type as the field bar.
      return Optional.of(bar);
    }
  }

  final class SerializableClass implements Serializable { // Compliant, records have different serialization behavior
    private final int sum;

    SerializableClass(int sum) { this.sum = sum; }
    int getSum() { return sum; }
  }

  final class ExternalizableClass implements Externalizable { // Compliant, records have different serialization behavior
    private final int sum;

    ExternalizableClass(int sum) { this.sum = sum; }
    int getSum() { return sum; }
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException { }
    public void writeExternal(ObjectOutput out) throws IOException { }
  }

  final class ClassWithWriteObject {
    private final int sum;

    ClassWithWriteObject(int sum) { this.sum = sum; }
    int getSum() { return sum; }
    private void writeObject(ObjectOutputStream out) throws IOException { }
  }

  final class ClassWithReadObject {
    private final int sum;

    ClassWithReadObject(int sum) { this.sum = sum; }
    int getSum() { return sum; }
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException { }
  }

  final class ClassWithReadObjectNoData {
    private final int sum;

    ClassWithReadObjectNoData(int sum) { this.sum = sum; }
    int getSum() { return sum; }
    private void readObjectNoData() throws ObjectStreamException { }
  }

  final class ClassWithWriteReplace {
    private final int sum;

    ClassWithWriteReplace(int sum) { this.sum = sum; }
    int getSum() { return sum; }
    private Object writeReplace() throws ObjectStreamException { return this; }
  }

  final class ClassWithReadResolve {
    private final int sum;

    ClassWithReadResolve(int sum) { this.sum = sum; }
    int getSum() { return sum; }
    private Object readResolve() throws ObjectStreamException { return this; }
  }

  final class ClassWithSerialPersistentFields {
    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[0];
    private final int sum;

    ClassWithSerialPersistentFields(int sum) { this.sum = sum; }
    int getSum() { return sum; }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  final class ClassWithJsonAnnotation { // Compliant, framework metadata owns the class shape
    private final int sum;

    ClassWithJsonAnnotation(int sum) { this.sum = sum; }
    int getSum() { return sum; }
  }

  final class ClassWithJsonCreatorConstructor {
    private final int sum;

    @JsonCreator
    ClassWithJsonCreatorConstructor(int sum) { this.sum = sum; }
    int getSum() { return sum; }
  }

  final class ClassWithJsonAnnotatedConstructorParameter {
    private final int sum;

    ClassWithJsonAnnotatedConstructorParameter(@JsonProperty("total") int sum) { this.sum = sum; }
    int getSum() { return sum; }
  }

  final class ClassWithJsonAnnotatedField {
    @JsonProperty("total")
    private final int sum;

    ClassWithJsonAnnotatedField(int sum) { this.sum = sum; }
    int getSum() { return sum; }
  }

  final class ClassWithJsonAnnotatedGetter {
    private final int sum;

    ClassWithJsonAnnotatedGetter(int sum) { this.sum = sum; }

    @JsonProperty("total")
    int getSum() { return sum; }
  }

  // When the constructor has smaller visibility, it is not possible to create a record with the same behavior.
  // Order: Public > protected > package private > private

  public final class ClassWithPublicConstructor { // Noncompliant
    private final int sum;
    public ClassWithPublicConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  public final class ClassWithProtectedConstructor { // Compliant, constructor visibility is smaller than public
    private final int sum;
    protected ClassWithProtectedConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  public final class ClassWithPackagePrivateConstructor { // Compliant, constructor visibility is smaller than public
    private final int sum;
    ClassWithPackagePrivateConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  public final class ClassWithPrivateConstructor { // Compliant, constructor visibility is smaller than public
    private final int sum;
    private ClassWithPrivateConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  protected final class ProtectedClassWithPublicConstructor { // Noncompliant
    private final int sum;
    public ProtectedClassWithPublicConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  protected final class ProtectedClassWithProtectedConstructor { // Noncompliant
    private final int sum;
    protected ProtectedClassWithProtectedConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  protected final class ProtectedClassWithPackagePrivateConstructor { // Compliant
    private final int sum;
    ProtectedClassWithPackagePrivateConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  protected final class ProtectedClassWithPrivateConstructor { // Compliant
    private final int sum;
    private ProtectedClassWithPrivateConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  final class PackagePrivateClassWithPublicConstructor { // Noncompliant
    private final int sum;
    public PackagePrivateClassWithPublicConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  final class PackagePrivateClassWithProtectedConstructor { // Noncompliant
    private final int sum;
    protected PackagePrivateClassWithProtectedConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  final class PackagePrivateClassWithPackagePrivateConstructor { // Noncompliant
    private final int sum;
    PackagePrivateClassWithPackagePrivateConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  final class PackagePrivateClassWithPrivateConstructor { // Compliant
    private final int sum;
    private PackagePrivateClassWithPrivateConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  private final class PrivateClassWithPublicConstructor { // Noncompliant
    private final int sum;
    public PrivateClassWithPublicConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }

  private final class PrivatelassWithPrivateConstructor { // Noncompliant
    private final int sum;
    private PrivatelassWithPrivateConstructor(int sum) {
      this.sum = sum;
    }
    int getSum() { return sum; }
  }
}
