package checks;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import static checks.MutableMembersUsageCheck.CustomImmutableList.staticallyImportedMethod;

class MutableMembersUsageCheck {

  private String[] strings;
  public String[] properties;
  private List<String> list = new LinkedList<>();
  // coverage
  private static List<String> staticList = null;
  private List<String> otherList = staticList;
  private ImmutableCollection ic;

  private List<String> mutableList = new ArrayList<>();
  private List<String> immutableList = Collections.unmodifiableList(mutableList);
  private List<String> singletonList = Collections.singletonList("A");
  private List<String> emptyList = Collections.emptyList();
  private Set<String> emptySet = Collections.emptySet();
  private Set<String> checkedSet = Collections.checkedSet(new HashSet<>(), String.class);
  private List<String> customImmutableList1 = customImmutableList(mutableList);
  private List<String> customImmutableList2 = CustomImmutableList.create(mutableList);
  private List<String> customImmutableList3 = staticallyImportedMethod(mutableList);
  private List<String> customUnmodifiableList1 = customUnmodifiableList(mutableList);
  private List<String> customUnmodifiableList2 = CustomUnmodifiableList.create(mutableList);

  private Map<String, String> hashMap = new HashMap<>();

  public Map<String, String> getHashMap() {
    return hashMap; // Noncompliant
  }

  private ImmutableMap<String, String> immutableMap;

  public Map<String, String> getImmutableMap() {
    return immutableMap; // Compliant
  }

  private Map<String, String> immutableMap2 = ImmutableMap.of("abc", "abc");

  public Map<String, String> getImmutableMap2() {
    return immutableMap2; // Compliant
  }

  private static final Set<String> unmodifiableFromStream =
    Stream.of(
        "first",
        "second")
      .collect(Collectors.toUnmodifiableSet());


  private static final List<String> unmodifiableFromStream2 =
    Arrays.asList(
        "first",
        "second")
      .stream()
      .collect(Collectors.toUnmodifiableList());

  private static final Map<Integer, Set<String>> unmodifiableFromStream4 =
    Stream.of("first", "second", "second", "thirteen", "thirteen", "thirteen")
      .collect(
        Collectors.collectingAndThen(
          Collectors.groupingBy(
            String::length, Collectors.<String>toUnmodifiableSet()
          ),
          Collections::unmodifiableMap
        )
      );

  private static final Map<Integer, String> unmodifiableFromStream3 =
    Stream.of("first", "second", "thirteen")
      .collect(Collectors.toUnmodifiableMap(String::length, e -> e));

  public Set<String> getUnmodifiableFromStream() {
    return unmodifiableFromStream;
  }

  public List<String> getUnmodifiableFromStream2() {
    return unmodifiableFromStream2;
  }

  public Map<Integer, String> getUnmodifiableFromStream3() {
    return unmodifiableFromStream3;
  }

  // A known false-positive as it's hard to identify that the result of the stream.collect() is actually unmodifiable
  public Map<Integer, Set<String>> getUnmodifiableFromStream4() {
    return unmodifiableFromStream4; // Noncompliant
  }

  // The following collections are immutable and therefore compliant.
  private static final Set<String> UNION = Sets.union(Collections.emptySet(), Set.of("Java"));
  private static final Set<String> DIFF = Sets.difference(Collections.emptySet(), Set.of("Java"));
  private static final Set<String> INTER = Sets.intersection(Collections.emptySet(), Set.of("Java"));
  private static final Set<String> SYM_DIFF = Sets.symmetricDifference(Collections.emptySet(), Set.of("Java"));
  private static final List<String> AS_LIST = Lists.asList("ABC", new String[] {"", ""});

  public Set<String> getUnion() {
    return UNION;
  }
  public Set<String> getDiff() {
    return DIFF;
  }
  public Set<String> getInter() {
    return INTER;
  }
  public Set<String> getSymDiff() {
    return SYM_DIFF;
  }
  public List<String> getAsList() {
    return AS_LIST;
  }

  public MutableMembersUsageCheck () {
    strings = new String[]{"first", "second"};
    properties = new String[]{"a"};
  }

  public String [] getStrings() {
    return strings; // Noncompliant [[sc=12;ec=19]] {{Return a copy of "strings".}}
  }

  public void setStringsFromGiven(String [] given) {
    strings = given; // Noncompliant [[sc=15;ec=20]] {{Store a copy of "given".}}
  }

  public List<String> getMutableList() {
    return mutableList; // Noncompliant {{Return a copy of "mutableList".}}
  }

  public List<String> getImmutableList() {
    return immutableList;
  }

  public List<String> getSingletonList() {
    return singletonList;
  }

  public List<String> getEmptyList() {
    return emptyList;
  }

  public Set<String> getEmptySet() {
    return emptySet;
  }

  public Set<String> getCheckedSet() {
    return checkedSet; // Noncompliant
  }

  public List<String> getCustomImmutableList1() {
    return customImmutableList1;
  }

  public List<String> getCustomImmutableList2() {
    return customImmutableList2;
  }

  public List<String> getCustomImmutableList3() {
    return customImmutableList3;
  }

  public List<String> getCustomUnmodifiableList1() {
    return customUnmodifiableList1;
  }

  public List<String> getCustomUnmodifiableList2() {
    return customUnmodifiableList2;
  }

  private static List<String> customImmutableList(List<String> given) {
    return Collections.unmodifiableList(given);
  }

  private static List<String> customUnmodifiableList(List<String> given) {
    return Collections.unmodifiableList(given);
  }

  public static class CustomImmutableList {
    public static List<String> create(List<String> given) {
      return Collections.unmodifiableList(given);
    }
    public static List<String> staticallyImportedMethod(List<String> given) {
      return Collections.unmodifiableList(given);
    }
  }

  public static class CustomUnmodifiableList {
    public static List<String> create(List<String> given) {
      return Collections.unmodifiableList(given);
    }
  }

  public void other(String[] given) {
    String[] doSomething = given; // Compliant, not stored in a member
    return;
  }

  public int[] passThrough(int[] arr) {
    return arr;
  }

  public int[] useButDoNotStoreInMember(int[] arr1, int[] arr2) {
    int[] tmp;
    if (arr1.length >= arr2.length) {
      tmp = arr1; // Compliant
    } else {
      tmp = arr2; // Compliant
    }
    return tmp;
  }

  public int[] useButDoNotStoreInMember2(int[] arr1, int[] arr2) {
    int[] tmp = arr1; // Compliant
    if (arr1.length < arr2.length) {
      tmp = arr2; // Compliant
    }
    return tmp;
  }

  public void setStrings(String [] strings) {
    this.strings = strings; // Noncompliant {{Store a copy of "strings".}}
    String[] local = new String[0];
    this.strings = local;
  }

  public void setImmutableCollection(ImmutableCollection ic) {
    this.ic = ic;
  }

  public List<String> foo() {
    return list; // Noncompliant
  }
  public List<String> foo2() {
    List<String> plop = Collections.unmodifiableList(list);
    return plop;
  }
}

class MutableMembersUsageCheck2 {
  private String [] strings;

  public MutableMembersUsageCheck2 () {
    strings = new String[]{"first", "second"};
  }

  public String [] getStrings() {
    return strings.clone();
  }

  public void setStrings(String [] strings) {
    this.strings = strings.clone(); // Compliant
  }
}

// Examples coming from CERT

class MutableClass {
  private Date d;

  public MutableClass() {
    d = new Date();
  }

  public Date getDate() {
    return d; // Noncompliant {{Return a copy of "d".}}
  }

  public Date getDateOK() {
    return (Date)d.clone();
  }
}

class MutableClass2 {
  private Date[] date;

  public MutableClass2() {
    date = new Date[20];
    for (int i = 0; i < date.length; i++) {
      date[i] = new Date();
    }
  }

  public Date[] getDate() {
    return this.date; // Noncompliant {{Return a copy of "date".}}
  }

  public Date[] getDateOK() {
    Date[] dates = new Date[date.length];
    for (int i = 0; i < date.length; i++) {
      dates[i] = (Date) date[i].clone();
    }
    return dates;
  }

  public Date[] getDateOk() {
    MutableClass2 mc2 = new MutableClass2();
    return mc2.date;
  }

  public Date[] getDateOk2() {
    return new MutableClass2().date;
  }

}

class ReturnRef {
  // Internal state, may contain sensitive data
  private Hashtable<Integer,String> ht = new Hashtable<Integer,String>();

  private ReturnRef() {
    ht.put(1, "123-45-6666");
  }

  public Hashtable<Integer,String> getValues(){
    return ht; // Noncompliant {{Return a copy of "ht".}}
  }

  private Hashtable<Integer,String> getValuesOK(){
    return (Hashtable<Integer, String>) ht.clone(); // shallow copy
  }
}

class MutableMembersUsageCheckFields {
  private static final List<String> UNMODIFIABLE = Collections.unmodifiableList(Arrays.asList("A", "B", "C"));
  private static final List<String> UNMODIFIABLE2;
  private static final Object UNMODIFIABLE_OBJECT;

  static {
    UNMODIFIABLE2 = Collections.unmodifiableList(Arrays.asList("A", "B", "C"));
    UNMODIFIABLE_OBJECT = UNMODIFIABLE2;
  }
  private static final ImmutableCollection UNMODIFIABLE3 = getImmutableCollection();

  private static final List<String> MODIFIABLE = new ArrayList<>();
  private static final List<String> MODIFIABLE2;
  static {
    MODIFIABLE2 = new ArrayList<>();
  }

  private static List<String> unmodifiable_static_not_final = Collections.unmodifiableList(Arrays.asList("A", "B", "C"));

  public List<String> foo1() {
    return UNMODIFIABLE; // Compliant
  }

  public List<String> foo2() {
    return UNMODIFIABLE2; // Compliant
  }

  public Collection<String> foo3() {
    return UNMODIFIABLE3; // Compliant
  }

  public List<String> bar1() {
    return unmodifiable_static_not_final; // Compliant, private variable only assigned to immutable collections
  }

  public List<String> bar2() {
    return MODIFIABLE; // Noncompliant
  }

  public List<String> bar3() {
    return MODIFIABLE2; // Noncompliant
  }

  private static ImmutableCollection getImmutableCollection() {
    return null;
  }

  class ImmutableInConstructor {
    private final List<String> list;

    ImmutableInConstructor(List<String> list) {
      this.list = Collections.unmodifiableList(list);
    }

    public List<String> getList() {
      return list; // Compliant, final and only assigned in constructor
    }
  }

  class ImmutableOnlyInOneConstructor {
    private final List<String> list;

    ImmutableOnlyInOneConstructor(List<String> list) {
      this.list = Collections.unmodifiableList(list);
    }

    ImmutableOnlyInOneConstructor() {
      this.list = new ArrayList();
    }

    public List<String> getList() {
      return list; // Noncompliant
    }
  }

  class FieldsNotStaticFinal {
    private List<String> UNMODIFIABLE = Collections.unmodifiableList(Arrays.asList("A", "B", "C"));
    private List<String> UNMODIFIABLE2;
    private List<String> UNMODIFIABLE3 = null;

    FieldsNotStaticFinal() {
      UNMODIFIABLE2 = Collections.unmodifiableList(Arrays.asList("A", "B", "C"));
    }

    void set(List<String> list) {
      UNMODIFIABLE2 = Collections.unmodifiableList(list);
      UNMODIFIABLE3 = Collections.unmodifiableList(list);
    }

    public List<String> foo1() {
      return UNMODIFIABLE; // Compliant, only assigned unmodifiable collections
    }

    public List<String> foo2() {
      return UNMODIFIABLE2; // Compliant, only assigned unmodifiable collections
    }

    public List<String> foo3() {
      return UNMODIFIABLE3; // Compliant, only assigned unmodifiable collections
    }
  }
}

class MutableMembersUsageCheckEmptyArrayExample {

  private static final int[] EMPTY = new int[0];
  private static final int[][][] EMPTY_SEVERAL_DIMS = new int[0][0][0];
  private static final int[] NOT_EMPTY = new int[foo()];
  private static final int[][][] NOT_EMPTY_SEVERAL_DIMS = new int[0][foo()][1];
  private static final int[] NOT_EMPTY_INIT = new int[]{ 1, 2};
  private static final char[] PLUS_SIGN = { '+' };

  public int[] getValues() {
    return EMPTY;
  }
  public int[][][] getValues2() {
    return EMPTY_SEVERAL_DIMS;
  }
  public int[] getValues3() {
    return NOT_EMPTY;// Noncompliant
  }
  public int[][][] getValues4() {
    return NOT_EMPTY_SEVERAL_DIMS; // Noncompliant
  }
  public int[] getValues5() {
    return NOT_EMPTY_INIT; // Noncompliant
  }
  public char[] getValues6() {
    return PLUS_SIGN; // Noncompliant
  }

  private static int foo() {
    return 42;
  }
}

class MutableMembersUsageCheckCollectionSingleton {
  private final static Set<String> singletonSet = Collections.singleton("Test");

  public Set<String> getSet() {
    return singletonSet;
  }
}

class MutableMembersUsageCheckImmutableInsideConstructors {
  private final List<String> list;
  private final Collection<String> collection;

  MutableMembersUsageCheckImmutableInsideConstructors(List<String> list) {
    this.list = Collections.unmodifiableList(list);
    this.collection = getImmutableCollection();
  }

  MutableMembersUsageCheckImmutableInsideConstructors(String element) {
    this(Arrays.asList(element));
  }

  public List<String> getList() {
    return list; // Compliant - field is immutable
  }

  public Collection<String> getCollection() {
    return collection; // Compliant - field is immutable
  }

  private static ImmutableCollection<String> getImmutableCollection() {
    return null;
  }
}

enum MyEnumWithAMutableMember {
  ELEMENT(new int[]{0, 1, 2});

  private final int[] arg;

  MyEnumWithAMutableMember(int[] arg) {
    this.arg = arg; // Compliant
  }
}

class MyClassWithAMutableMember {
  private final int[] arg;

  MyClassWithAMutableMember(int[] arg) {
    this.arg = arg; // Noncompliant
  }
}

class Java9Methods {
  private static final List<String> MODIFIABLE = new ArrayList<>();
  private static final List<Integer> IMMUTABLE_LIST = List.of(1, 2, 3);
  private static final List<Integer> IMMUTABLE_COPY_LIST = List.copyOf(new ArrayList<>());
  private static final Set<String> IMMUTABLE_SET = Set.of("a");
  private static final Set<String> IMMUTABLE_COPY_SET = Set.copyOf(new HashSet<>());

  public List<Integer> immutableList() {
    return IMMUTABLE_LIST; // Compliant
  }

  public List<Integer> immutableListCopy() {
    return IMMUTABLE_COPY_LIST; // Compliant
  }

  public Set<String> immutableSet() {
    return IMMUTABLE_SET; // Compliant
  }

  public Set<String> immutableSetCopy() {
    return IMMUTABLE_COPY_SET; // Compliant
  }

  public List<String> modifiable() {
    return MODIFIABLE; // Noncompliant
  }

}
