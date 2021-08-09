package checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectionConstructorReferenceCheck {
  void foo() {
    Map<Integer, List<Object>> col1 = Stream.of(1, 2, 54000)
      .collect(Collectors.toMap(
        Function.identity(),
        ArrayList::new));// Noncompliant [[sc=9;ec=23]] {{Replace this method reference by a lambda to explicitly show the usage of ArrayList(int initialCapacity) or ArrayList().}}

    Map<Integer, List<Object>> col2 = Stream.of(1, 2, 54000)
      .collect(Collectors.toMap(
        Function.identity(),
        id -> new ArrayList<>())); // Compliant, usage of lambdas are explicit

    List<Integer> col3 = Stream.of(1, 2, 54000)
      .collect(Collectors.toCollection(ArrayList::new));// Compliant, Supplier argument

    List<Integer> arrayList = new ArrayList();
    Map<Integer, Integer> col4 = Stream.of(1, 2, 54000)
      .collect(Collectors.toMap(
        Function.identity(),
        arrayList::indexOf)); // Compliant, not a constructor

    List<Function<Integer, Object>> constructors = Arrays.asList(
      ArrayList::new, // Noncompliant
      BitSet::new, // Compliant, not a collection
      HashMap::new, // Noncompliant
      HashSet::new, // Noncompliant
      Hashtable::new, // Noncompliant [[sc=7;ec=21]] {{Replace this method reference by a lambda to explicitly show the usage of Hashtable(int initialCapacity) or Hashtable().}}
      IdentityHashMap::new, // Noncompliant [[sc=7;ec=27]] {{Replace this method reference by a lambda to explicitly show the usage of IdentityHashMap(int expectedMaxSize) or IdentityHashMap().}}
      LinkedHashMap::new, // Noncompliant
      LinkedHashSet::new, // Noncompliant
      PriorityQueue::new, // Noncompliant
      Vector::new, // Noncompliant
      WeakHashMap::new // Noncompliant
    );

    Function<Collection<String>, List<String>> list1 = ArrayList::new;  // Compliant, refer to "ArrayList(Collection<? extends E>)"
    IntFunction<List<String>> list2 = ArrayList::new;  // Compliant, not a Function but an explicit IntFunction
  }

}
