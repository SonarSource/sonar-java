package checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

class DiamondOperatorCheck_java_8 {
  List<Object> myList1 = new ArrayList<>(); // Compliant
  List<Object> myList2 = new ArrayList<Object>(); // Noncompliant [[sc=39;ec=47]] {{Replace the type specification in this constructor call with the diamond operator ("<>").}}

  void foo() {
    List<Object> myList;
    myList = new ArrayList<>(); // Compliant
    myList = new ArrayList<Object>(); // Noncompliant [[sc=27;ec=35]]

    List<String> strings1 = new ArrayList<>(); // Compliant
    List<String> strings2 = new ArrayList<String>(); // Noncompliant [[sc=42;ec=50]]
    Map<String,List<Integer>> map1 = new HashMap<>(); // Compliant
    Map<String,List<Integer>> map2 = new HashMap<String,List<Integer>>(); // Noncompliant [[sc=49;ec=71]]

    List myOtherList = new ArrayList<Object>(); // Compliant
    new DiamondOperatorCheck_java_8().myList1 = new ArrayList<>(); // Compliant
    new DiamondOperatorCheck_java_8().myList1 = new ArrayList<Object>(); // Noncompliant [[sc=62;ec=70]]

    List<Object>[] myArrayOfList = new List[10];
    myArrayOfList[0] = new ArrayList<>(); // Compliant
    myArrayOfList[1] = new ArrayList<Object>(); // Noncompliant [[sc=37;ec=45]]

    new ArrayList<Object>().add(new Object()); // Compliant

    ((List<Object>) new ArrayList<Object>()).isEmpty(); // Noncompliant [[sc=34;ec=42]]
    ((List<Object>) new ArrayList<>()).isEmpty(); // Compliant

    Iterator<Object> iterator = new Iterator<Object>() { // Compliant - anonymous classes requires to be typed
      @Override public Object next() { return null; }
      @Override public boolean hasNext() { return false; }
    };

    Object data = new List[10];
    ((List[])data)[2] = new ArrayList<String>();
  }

  List<Object> qix(boolean test) {
    List<Object> myList = test ?
      new ArrayList<>() : // Compliant
        new ArrayList<Object>(); // Noncompliant [[sc=22;ec=30]]

    myList = new ArrayList<>(test ? new ArrayList<>() : new ArrayList<Object>(5)); // Compliant
    if (test) {
      return new ArrayList<>(); // Compliant
    }
    return new ArrayList<Object>(); // Noncompliant [[sc=25;ec=33]]
  }

  public void assignmentOnMethodInvocation() {
    this.bar()[0] = new GenericClass<String, Integer, Integer>();
  }

  public MyInterface[] bar() {
    return new MyInterface[1];
  }

  interface MyInterface { }
  static class GenericClass<X, Y, Z> implements MyInterface { }

  static class A<X> {

    public A(X x) { }

    public void tst() {
      bar(0, new ArrayList<String>()); // Noncompliant [[sc=27;ec=35]]
      foo(0, new ArrayList<String>()); // Compliant
      qix(new ArrayList<String>()); // Compliant
      gul(new ArrayList<String>()); // Compliant

      A<List<String>> als =
        new A<List<String>>( // Noncompliant [[sc=14;ec=28]]
          new ArrayList<String>()); // Noncompliant [[sc=24;ec=32]]

      List<String> values = Arrays.asList("Stop", "pointing", "fingers", "steve");
      new ArrayList<String>(values); // FN - based on type inference
      new ArrayList<>(values);
    }

    static void bar(int i, List<String> strings) {}
    static void foo(Object... objects) {}
    static void qix(Object o) {}
    static <T> void gul(T t) {}
  }
}

enum SonarProblemCase {
  MY_BUCKET(rp -> { return new ArrayList<Integer>(); }),
  MY_BUCKET2(rp -> new ArrayList<Integer>());

  SonarProblemCase(Function<Object, Object> bucketer) {}
}
