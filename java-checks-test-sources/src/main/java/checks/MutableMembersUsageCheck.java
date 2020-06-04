package checks;

import java.util.*;
import com.google.common.collect.ImmutableCollection;

class MutableMembersUsageCheck {
  private String[] strings;
  public String[] properties;
  private List<String> list = new LinkedList<>();
  // coverage
  private static List<String> staticList = null;
  private List<String> otherList = staticList;
  private ImmutableCollection ic;

  public MutableMembersUsageCheck () {
    strings = new String[]{"first", "second"};
    properties = new String[]{"a"};
  }

  public String [] getStrings() {
    return strings; // Noncompliant [[sc=12;ec=19]] {{Return a copy of "strings".}}
  }

  public void other(String[] given) {
    String[] doSomething = given; // Noncompliant [[sc=28;ec=33]] {{Store a copy of "given".}}
    return;
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

class Fields {
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

  private static List<String> unmodifiable_not_final = Collections.unmodifiableList(Arrays.asList("A", "B", "C"));

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
    return unmodifiable_not_final; // Noncompliant
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
