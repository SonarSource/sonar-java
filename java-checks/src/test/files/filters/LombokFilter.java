import lombok.val;
import lombok.var; // NoIssue
import java.util.List; // WithIssue
import static lombok.AccessLevel.PRIVATE;

class Fields {
  @lombok.Getter
  class Getter { // WithIssue
    private int foo; // NoIssue
  }

  class Getter2 { // WithIssue
    private int foo; // WithIssue
  }

  @lombok.Setter
  class Setter { // WithIssue
    private int foo; // NoIssue
  }

  class Setter2 { // WithIssue
    private int foo; // WithIssue
  }

  @lombok.Data
  class Data { // WithIssue
    private int foo; // NoIssue
  }

  class Data2 { // WithIssue
    private int foo; // WithIssue
  }

  @lombok.Value
  class Value { // WithIssue
    private int foo; // NoIssue
  }

  class Value2 { // WithIssue
    private int foo; // WithIssue
  }

  @lombok.Builder
  class Builder { // WithIssue
    private int foo; // NoIssue
  }

  class Builder2 { // WithIssue
    private int foo; // WithIssue
  }

  @lombok.ToString
  class ToString { // WithIssue
    private int foo; // NoIssue
  }

  class ToString2 { // WithIssue
    private int foo; // WithIssue
  }

  @lombok.RequiredArgsConstructor
  class RequiredArgsConstructor { // NoIssue
    private int foo; // NoIssue
  }

  class RequiredArgsConstructor2 { // WithIssue
    private int foo; // WithIssue
  }

  @lombok.AllArgsConstructor
  class AllArgsConstructor { // NoIssue
    private int foo; // NoIssue
  }

  class AllArgsConstructor2 { // WithIssue
    private int foo; // WithIssue
  }

  @lombok.NoArgsConstructor
  class NoArgsConstructor { // NoIssue
    private int foo; // NoIssue
  }

  class NoArgsConstructor2 { // WithIssue
    private int foo; // WithIssue
  }

  @lombok.EqualsAndHashCode
  class EqualsAndHashCode { // WithIssue
    private int foo; // NoIssue
  }

  class EqualsAndHashCode2 { // WithIssue
    private int foo; // WithIssue
  }
}

class EqualsNotOverriddenInSubclass {
  class A {
    String s1;
  }

  class B extends A { // NoIssue
    String s2;
  }

  @lombok.EqualsAndHashCode
  class B1 extends A { // NoIssue
    String s2;
  }

  @lombok.Data
  class B2 extends A { // NoIssue
    String s2;
  }

  @lombok.Value
  class B3 extends A { // NoIssue
    String s2;
  }
}

public class EqualsNotOverridenWithCompareToCheck implements Comparable {

  class A implements Comparable {
    public int compareTo(Object o) { // WithIssue
      return 0;
    }
  }

  @lombok.EqualsAndHashCode
  class A1 implements Comparable<A> {
    @Override
    public int compareTo(A o) { // NoIssue
      return 0;
    }
  }

  class A2 implements Comparable<A> {
    @Override
    public int compareTo(A o) { // WithIssue
      return 0;
    }
  }

  @lombok.Data
  class B1 implements Comparable<A> {
    @Override
    public int compareTo(A o) { // NoIssue
      return 0;
    }
  }

  class B2 implements Comparable<A> {
    @Override
    public int compareTo(A o) { // WithIssue
      return 0;
    }
  }

  @lombok.Value
  class C1 implements Comparable<A> {
    @Override
    public int compareTo(A o) { // NoIssue
      return 0;
    }
  }

  class C2 implements Comparable<A> {
    @Override
    public int compareTo(A o) {  // WithIssue
      return 0;
    }
  }
}

static class UtilityClass {
  private UtilityClass() {}

  static class A { // WithIssue
    public static void foo() {
    }

    public final String FINAL_NON_STATIC = "x"; // WithIssue
    public final String finalNonStatic = "x"; // WithIssue
  }

  @lombok.experimental.UtilityClass
  static class B { // NoIssue
    public static void foo() {
    }

    public final String STATIC_FINAL_ALLCAPS = "x"; // NoIssue
    public final String staticFinalCamelCase = "x"; // NoIssue
  }
}

static class UtilityClassWithPublicConstructorCheck {
  private UtilityClassWithPublicConstructorCheck() {
  }

  static class A { // WithIssue
  }

  @lombok.NoArgsConstructor
  public static class B { // WithIssue
  }

  @lombok.RequiredArgsConstructor
  public static class C { // WithIssue
  }

  @lombok.AllArgsConstructor
  public static class D { // WithIssue
  }

  @lombok.NoArgsConstructor(staticName = "yolo", access = lombok.AccessLevel.PRIVATE)
  public static class E { // NoIssue
  }

  @lombok.RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
  public static class F { // NoIssue
  }

  @lombok.AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
  public static class G { // NoIssue
  }

  @lombok.NoArgsConstructor(access = PRIVATE)
  public static class H { // NoIssue
  }

  @lombok.NoArgsConstructor(access = lombok.AccessLevel.NONE)
  public static class I { // WithIssue
  }

  @lombok.NoArgsConstructor()
  public static class J { // WithIssue
  }

  @lombok.NoArgsConstructor(access = getValue()) // does not compile - for coverage only
  public static class K { // NoIssue
  }

  public static lombok.AccessLevel getValue() {
    return lombok.AccessLevel.MODULE;
  }
}

class LombokVal {
  boolean s2159_val_with_fully_qualified_name(String x) {
    lombok.val y = "Hello World";
    return x
      .equals( // NoIssue
        y);
  }

  boolean s2159_val_with_import(String x) {
    val y = "Hello World";
    return x.equals(y); // NoIssue
  }

  boolean s2159_valid(String x) {
    Object y = "Hello World";
    return x.equals(y); // Withissue
  }

  boolean s2175(java.util.List<String> words) {
    lombok.val y = "Hello World";
    return words.contains(y); // NoIssue
  }

  boolean s2175_valid(java.util.List<String> words) {
    Integer y = 42;
    return words.contains(y); // WithIssue
  }
}

class PrivateFieldOnlyUsedLocally {
  private PrivateFieldOnlyUsedLocally() {
  }

  class A { // WithIssue
    private int foo; // WithIssue
    public void bar(int y){
      foo = y + 5;
      if (foo == 0) {
        // ...
      }
    }
  }

  @lombok.Getter
  class B { // WithIssue
    private int foo; // NoIssue
    public void bar(int y){
      foo = y + 5;
      if (foo == 0) {
        // ...
      }
    }
  }

  @lombok.AllArgsConstructor
  class C { // NoIssue
    private int foo; // NoIssue
    public void bar(int y){
      if (foo == 0) {
        // ...
      }
    }
  }

  @lombok.Data
  class D { // WithIssue
    private int foo; // NoIssue
    public void bar(int y){
      foo = y + 5;
      if (foo == 0) {
        // ...
      }
    }
  }

  class E { // WithIssue
    @Getter private int foo; // NoIssue
    public void bar(int y){
      foo = y + 5;
      if (foo == 0) {
        // ...
      }
    }
  }

  class UsingVarAndVal {
    public void test() {
      var foo1 = "hello, ";
      val foo2 = 5;
      System.out.println(foo1 + foo2);
    }
  }
}
