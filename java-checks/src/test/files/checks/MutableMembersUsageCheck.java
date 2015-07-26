import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.LinkedList;
import com.google.common.collect.ImmutableCollection;

class A {
  private String[] strings;
  public String[] properties;
  private List<String> list = new LinkedList<>();
  // coverage
  private static List<String> staticList = null;
  private List<String> otherList = staticList;
  private ImmutableCollection ic;

  public A () {
    strings = new String[]{"first", "second"};
    properties = new String[]{"a"};
  }

  public String [] getStrings() {
    return strings; // Noncompliant {{Return a copy of "strings".}}
  }

  public void other(String[] given) {
    String[] doSomething = given; // Noncompliant {{Store a copy of "given".}}
    return;
  }

  public void setStrings(String [] strings) {
    this.strings = strings; // Noncompliant {{Store a copy of "strings".}}
    String[] local;
    this.strings = local;
  }

  public void setImmutableCollection(ImmutableCollection ic) {
    this.ic = ic;
  }
}

class C {
  private String [] strings;

  public C () {
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
    return date; // Noncompliant {{Return a copy of "date".}}
  }

  public Date[] getDateOK() {
    Date[] dates = new Date[date.length];
    for (int i = 0; i < date.length; i++) {
      dates[i] = (Date) date[i].clone();
    }
    return dates;
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
