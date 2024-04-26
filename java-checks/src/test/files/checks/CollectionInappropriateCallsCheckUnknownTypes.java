package checks;

import com.google.common.collect.Lists;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class CollectionInappropriateCallsCheck {
  private void myMethod() {
    List<Unknown> unknownList = new ArrayList<Unknown>();
    Integer myInteger = Integer.valueOf(1);
    List<Set<Integer>> mySetList = new ArrayList<Set<Integer>>();
    Map<String, Unknown> myMap = new HashMap<>();
    unknownList.contains(myInteger);
    mySetList.contains(unknownMethod()); // Compliant
    myUnknownCollection.stream().filter(s -> myASet.contains(s.toString())).collect(Collectors.toSet()); // Compliant
    myMap.containsKey("key");
    myMap.containsKey(1L);
    myMap.containsValue("");
    myMap.containsValue(new Unknown());
    mySetList.removeAll(unknownList);

    Map notParametrizedMap = new HashMap<String, Unknown>();
    notParametrizedMap.containsKey(1L);
    notParametrizedMap.containsValue("");
  }

}

class Test {
  private List<String> badGrades = null;

  public String testSplitArray() {
    badGrades = java.util.Arrays.asList("C", "D", "E");

    List<String> myGrades = java.util.Arrays.asList("A,B,A", "A,C,A", "A,D,E");
    String test = myGrades.stream().
      map(grade -> unknownMethod()).
      filter(grade -> badGrades.contains(grade)). // compliant, type inference is unable to resolve type of grade because of unknownMethod
      findFirst().orElse("Nothing");
  }
}

class LombokVal {
  boolean foo(List<String> words) {
    lombok.val y = "Hello World";
    return  words.contains(y); // Noncompliant
  }
}
