package checks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class DiamonOperatorCheck_no_version {
  List<Object> myList1 = new ArrayList<>(); // Compliant
  List<Object> myList2 = new ArrayList<Object>(); // Noncompliant [[sc=39;ec=47;quickfixes=qf1]] {{Replace the type specification in this constructor call with the diamond operator ("<>"). (sonar.java.source not set. Assuming 7 or greater.)}}
  // fix@qf1 {{Replace with <>}}
  // edit@qf1 [[sc=39;ec=47]] {{<>}}

  List<Object> myList3 = new ArrayList< // Noncompliant [[sc=39;el=+2;ec=6;quickfixes=qf2]]
    Object
    >();
  // fix@qf2 {{Replace with <>}}
  // edit@qf2 [[sc=39;el=+2;ec=6]] {{<>}}

  void foo() {
    List<Object> myList;
    myList = new ArrayList<>(); // Compliant
    myList = new ArrayList<Object>(); // Noncompliant [[sc=27;ec=35]]

    List<String> strings1 = new ArrayList<>(); // Compliant
    List<String> strings2 = new ArrayList<String>(); // Noncompliant [[sc=42;ec=50]]
    Map<String,List<Integer>> map1 = new HashMap<>(); // Compliant
    Map<String,List<Integer>> map2 = new HashMap<String,List<Integer>>(); // Noncompliant [[sc=49;ec=71]]

    List myOtherList = new ArrayList<Object>(); // Compliant
    new DiamonOperatorCheck_no_version().myList1 = new ArrayList<>(); // Compliant
    new DiamonOperatorCheck_no_version().myList1 = new ArrayList<Object>(); // Noncompliant [[sc=65;ec=73]]

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
}
