package checks;

import com.google.common.collect.ImmutableCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PublicStaticMutableMembersCheck {
  public static final List LIST = Arrays.asList("a"); // Noncompliant
  public static final List UNKNOWN_LIST = unknownMethod("a"); // Compliant
  public static final List noInitializer;
  // we don't know the type of foo
  public static final List unknown = foo();

  // Java 9
  public static final Map<String, String> IMMUTABLE_MAP = Map.of("a", "A");
  public static final Map<String, String> IMMUTABLE_MAP_COPY = Map.copyOf(new HashMap<>());
  public static final Map<String, String> IMMUTABLE_MAP_OF_ENTRIES = Map.ofEntries(Map.entry("1", "2"));
  public static final List<String> IMMUTABLE_LIST = List.of("hello");
  public static final List<String> IMMUTABLE_LIST_COPY = List.copyOf(new ArrayList<>());
  public static final Set<String> IMMUTABLE_SET = Set.of("hello");
  public static final Set<String> IMMUTABLE_SET_COPY = Set.copyOf(new HashSet<>());

  // guava (Forbidden API)
  public static final List<String> immutableList = ImmutableList.of("a");
  public static final Set<String> immutableSet = ImmutableSet.of("a");
  public static final Map<String, String> immutableMap = ImmutableMap.of("a", "a");

  public static final Set<String> otherImmutableSet = immutableSet;
  public static final Map<String, String> otherImmutableMap = immutableMap;
}

interface I {
  public static MyImmutableCollection<String> immutableList2; //Compliant : immutable collection

  // guava (Forbidden API)
  public static final List<String> immutableList3 = ImmutableList.of("a");
  public static final Set<String> immutableSet2 = ImmutableSet.of("a");
  public static final Map<String, String> immutableMap2 = ImmutableMap.of("a", "a");
  public static final Set otherImmutableSet2 = immutableSet2;
}

class MyImmutableCollection<E> extends ImmutableCollection<E> { }
