package checks;

import static java.util.Arrays.asList;

import java.util.*;

class UnsupportedOperationUsageCheck {

  private List<String> privateStrings7 = Arrays.asList("test1", "test2");
  private static List<String> privateStaticStrings8 = Arrays.asList("test1", "test2");

  public void add1() {
    List<String> strings1;
    strings1 = Arrays.asList("test1", "test2");
    strings1
      .add("test3"); // Noncompliant
  }

  public void add2() {
    List<String> strings2;
    strings2 = asList("test1", "test2");
    strings2.add("test3"); // Noncompliant
  }

  public void add3() {
    List<String> strings3 = Arrays.asList("test1", "test2");
    strings3.add("test3"); // Noncompliant
  }

  public void add4() {
    List<String> strings4 = Collections.emptyList();
    strings4.add("test"); // Noncompliant
  }

  public void add5() {
    Set<String> strings5 = Collections.emptySet();
    strings5.add("test"); // Noncompliant
  }

  public void add6() {
    Map<String, String> strings6 = Collections.emptyMap();
    strings6.put("test", "test"); // Noncompliant
  }

  public void add7() {
    privateStrings7.add("test"); // Noncompliant
  }

  public void add8() {
    privateStaticStrings8.add("test"); // Noncompliant
  }

  public void add9() {
    List strings9 = org.apache.commons.collections.list.FixedSizeList.decorate(new ArrayList<>(0));
    strings9.add("test"); // Noncompliant
  }

  public void add10() {
    List<String> strings10 = org.apache.commons.collections4.list.FixedSizeList.fixedSizeList(new ArrayList<>(0));
    strings10.add("test"); // Noncompliant
  }

  public void add11() {
    Set<String> strings11 = Set.of();
    strings11.add("test"); // Noncompliant
  }

  public void add12() {
    List<String> strings12 = List.of("test1", "test2", "test4");
    strings12.add("test"); // Noncompliant
  }

  public void add13() {
    List<String> strings13 = List.of("test1", "test2", "test4", "test1", "test2", "test4",
      "test1", "test2", "test4", "test1");
    strings13.add("test"); // Noncompliant
  }

  public void add14() {
    Map<String, String> strings14 = Map.of("test1", "test2");
    strings14.put("test", "test"); // Noncompliant
  }
}
