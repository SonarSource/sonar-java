import java.util.List;

public class MyClass {

  public enum MyEnum1 {
    VALUE;
  }

  public enum MyEnum2 {
    VALUE;
  }

  public interface Interface {
  }

  Object object;
  Object[] arrayOfObjects;
  Integer integer;
  Integer[] arrayOfIntegers;
  String string;
  String[] arrayOfStrings;
  Comparable comparable;
  java.io.File file;
  java.io.Serializable serializable;
  Interface intf;
  MyClass my;
  MyEnum1 myEnum1;
  MyEnum2 myEnum2;

  public void method() {
    // class vs array
    object.equals(arrayOfObjects); // Compliant
    arrayOfObjects.equals(object); // Compliant
    integer.equals(arrayOfObjects); // Noncompliant [[sc=13;ec=19]] {{Remove this call to "equals"; comparisons between a type and an array always return false.}}
    arrayOfObjects.equals(integer); // Noncompliant {{Remove this call to "equals"; comparisons between an array and a type always return false.}}
    arrayOfIntegers.equals(arrayOfStrings); // Noncompliant {{Remove this call to "equals"; comparisons between unrelated arrays always return false.}}
    object.equals(1); // Compliant
    integer.equals(1); // Compliant
    string.equals(1); // Noncompliant {{Remove this call to "equals"; comparisons between unrelated types always return false.}}
    arrayOfObjects.equals(1); // Noncompliant {{Remove this call to "equals"; comparisons between an array and a type always return false.}}

    // arrays vs arrays
    arrayOfObjects = arrayOfIntegers;
    arrayOfObjects.equals(arrayOfIntegers); // Noncompliant {{Use "Arrays.equals(array1, array2)" or the "==" operator instead of using the "Object.equals(Object obj)" method.}}
    arrayOfIntegers.equals(arrayOfObjects); // Noncompliant {{Use "Arrays.equals(array1, array2)" or the "==" operator instead of using the "Object.equals(Object obj)" method.}}
    arrayOfIntegers.equals(arrayOfStrings); // Noncompliant {{Remove this call to "equals"; comparisons between unrelated arrays always return false.}}

    // class vs class
    object.equals(my); // Compliant, related
    my.equals(object); // Compliant, related
    my.equals(string); // Noncompliant {{Remove this call to "equals"; comparisons between unrelated types always return false.}}
    string.equals(my); // Noncompliant {{Remove this call to "equals"; comparisons between unrelated types always return false.}}
    my.equals(file); // Noncompliant {{Remove this call to "equals"; comparisons between unrelated types always return false.}}
    file.equals(my); // Noncompliant {{Remove this call to "equals"; comparisons between unrelated types always return false.}}
    myEnum1.equals(myEnum2); // Noncompliant {{Remove this call to "equals"; comparisons between unrelated types always return false.}}

    // class vs interface
    my.equals(serializable); // Compliant, "MyClass" is not final
    integer.equals(serializable); // Compliant, integer is final and implements serializable
    string.equals(intf); // Noncompliant {{Remove this call to "equals"; comparisons between unrelated types always return false.}}

    // interface vs class
    serializable.equals(my); // Compliant, "MyClass" is not final
    serializable.equals(integer); // Compliant, integer is final and implements serializable
    intf.equals(string); // Noncompliant {{Remove this call to "equals"; comparisons between unrelated types always return false.}}

    // interface vs interface
    comparable.equals(serializable); // Compliant, nothing can be said between two interfaces
    serializable.equals(comparable); // Compliant, nothing can be said between two interfaces

    equals(this); // Compliant
    equals(object); // Compliant
    equals(arrayOfObjects); // False negative, method equals is not overriden

    object.equals(null); // Noncompliant {{Remove this call to "equals"; comparisons against null always return false; consider using '== null' to check for nullity.}}

    Class<?> that = Object.class;
    ((Class<?>) getClass()).equals(that); // Compliant

    Class<Object> that2 = Object.class;
    getClass().equals(that2); // False negative, if it is a Class<Object> then it cannot be a Class<MyClass>

    List<String> listOfStrings;
    List<java.io.File> listOfFiles;
    listOfStrings.equals(listOfFiles); // False negative, since String and File are not related

    List<Object> listOfObjects;
    listOfObjects.equals(listOfStrings); // False negative, compliant if listOfObjects only contains strings, but it is not type-safe
    listOfStrings.equals(listOfObjects); // False negative, compliant if listOfObjects only contains strings, but it is not type-safe

    List<?> listOfObjectsExtended = listOfStrings;
    listOfObjectsExtended.equals(listOfStrings); // Compliant

    // Compliant
    object.equals();
    object.hashCode();
  }

  public <T> void parameterizedMethod1(T o) {
    equals(o); // Compliant
  }

  public <T extends String> void parameterizedMethod2(T o) {
    equals(o); // False negative, String and MyClass are unrelated
  }

  boolean foo(String x) {
    lombok.val y = "Hello World";
    return x.equals(y); // Noncompliant - FP - removed by the lombok filter
  }

}
