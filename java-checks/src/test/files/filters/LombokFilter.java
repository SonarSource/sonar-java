/* unused */ import java.util.List; // WithIssue
import java.util.regex.Pattern;

import lombok.val; // NoIssue
import lombok.var; // NoIssue
/* unused */ import lombok.SneakyThrows; // WithIssue

import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.XMLConstants;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;
import static lombok.AccessLevel.PUBLIC;

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
  class Data { // NoIssue
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

  // SONARJAVA-3579 No FP for non-static final fields with Builder.Default
  @lombok.Builder
  class Builder3 { // NoIssue
    @lombok.Builder.Default
    public final int bar = 1; // NoIssue
  }

  // Final fields with Builder.Default that aren't in a Builder class should still complain
  class Builder4 { // WithIssue
    @lombok.Builder.Default
    public final int bar = 1; // WithIssue
  }

  // Final fields without Builder.Default should still complain
  @lombok.Builder
  class Builder5 { // WithIssue
    public final int foo = 1; // WithIssue
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

  @lombok.Data
  class RequiredArgsConstructor15 { // NoIssue
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
    public String s1;
  }

  class B extends A { // NoIssue
    String s2; // WithIssue
  }

  @lombok.EqualsAndHashCode
  class B1 extends A { // NoIssue
    public String s2;
  }

  @lombok.Data
  class B2 extends A { // NoIssue
    public String s2;
  }

  @lombok.Value
  class B3 extends A { // NoIssue
    String s2; // NoIssue
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

    private final Pattern PATTERN = Pattern.compile(".*"); // WithIssue

    private static final String STRCONST = ".*";

    private void match() {
      Pattern.compile(STRCONST); // WithIssue
      "abc".matches(STRCONST); // WithIssue
      PATTERN.matcher("a");
    }

    private String doNotAccessInstanceField() { // WithIssue
      return STRCONST;
    }
  }

  // UtilityClass generates static keyword for fields
  @lombok.experimental.UtilityClass
  static class B { // NoIssue
    public static void foo() {
    }

    public final String STATIC_FINAL_ALLCAPS = "x"; // NoIssue
    public final String staticFinalCamelCase = "x"; // NoIssue

    private final Pattern PATTERN = Pattern.compile(".*"); // NoIssue

    private static final String STRCONST = ".*";

    private void match() {
      Pattern.compile(STRCONST); // WithIssue
      "abc".matches(STRCONST); // WithIssue
      PATTERN.matcher("a");
    }

    private String doNotAccessInstanceField() { // NoIssue
      return STRCONST;
    }
  }
}

static class UtilityClassWithPublicConstructorCheck {
  private UtilityClassWithPublicConstructorCheck() {
  }

  static class A { // WithIssue
    public static int i;
  }

  @lombok.NoArgsConstructor
  public static class B { // WithIssue
    public static int i;
  }

  @lombok.RequiredArgsConstructor
  public static class C { // WithIssue
    public static int i;
  }

  @lombok.AllArgsConstructor
  public static class D { // WithIssue
    public static int i;
  }

  @lombok.NoArgsConstructor(staticName = "yolo", access = lombok.AccessLevel.PRIVATE)
  public static class E { // NoIssue
    public static int i;
  }

  @lombok.RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
  public static class F { // NoIssue
    public static int i;
  }

  @lombok.AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
  public static class G { // NoIssue
    public static int i;
  }

  @lombok.NoArgsConstructor(access = PRIVATE)
  public static class H1 { // NoIssue
    public static int i;
  }

  @lombok.NoArgsConstructor(access = PROTECTED)
  public static class H2 { // NoIssue
    public static int i;
  }

  @lombok.NoArgsConstructor(access = PUBLIC)
  public static class H3 { // WithIssue
    public static int i;
  }

  @lombok.NoArgsConstructor(access = lombok.AccessLevel.NONE)
  public static class I { // NoIssue
    public static int i;
  }

  @lombok.NoArgsConstructor()
  public static class J { // WithIssue
    public static int i;
  }

  @lombok.NoArgsConstructor(access = getValue()) // does not compile - for coverage only
  public static class K { // NoIssue
    public static int i;
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

  void S5845(String s) {
    lombok.val y = "Hello World";
    org.junit.Assert.assertEquals(y, s); // NoIssue
  }

  boolean S5845_valid(String s) {
    Integer y = 42;
    org.junit.Assert.assertEquals(y, s); // WithIssue
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
  class D { // NoIssue
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

@lombok.Value
class IgnoreModifier {
  String id; // NoIssue
  String name; // NoIssue
  @lombok.experimental.PackagePrivate String email; // WithIssue
}

@lombok.Value
@lombok.experimental.NonFinal
@lombok.AllArgsConstructor
class NonFinalClassAnnotationException extends RuntimeException { // NoIssue
  final int id; // NoIssue
  private String name; // WithIssue

  public String getName() {
    return name;
  }
}

@lombok.Value
@lombok.AllArgsConstructor
class NonFinalVariableAnnotationException extends RuntimeException { // NoIssue
  int id; // NoIssue
  @lombok.experimental.NonFinal private String name; // WithIssue
}

class FieldDefaults {
  @lombok.experimental.FieldDefaults
  class A {
    int id; // WithIssue
  }
  @lombok.experimental.FieldDefaults(level = lombok.AccessLevel.PRIVATE)
  class B {
    int id; //NoIssue
    @lombok.experimental.PackagePrivate String name; // WithIssue
  }
  @lombok.experimental.FieldDefaults(level = lombok.AccessLevel.NONE)
  class C {
    int id; // WithIssue
  }
  @lombok.experimental.FieldDefaults(makeFinal=getValue()) // does not compile - for coverage only
  class MakeFinalFalseAnnotationException extends RuntimeException {  // WithIssue
    private String name; // WithIssue
  }

  @lombok.experimental.FieldDefaults(makeFinal=true, level = lombok.AccessLevel.PRIVATE)
  @lombok.AllArgsConstructor
  class NonFinalVariableAnnotationException extends RuntimeException { // NoIssue
    int id; // NoIssue
    String name; // NoIssue
    @lombok.experimental.NonFinal private String email;  // WithIssue
  }

  @lombok.experimental.FieldDefaults(makeFinal=false)
  class MakeFinalFalseAnnotationException extends RuntimeException {
    private String name; // WithIssue

    public MakeFinalFalseAnnotationException(String name) {
      this.name = name;
    }
  }

  @lombok.experimental.FieldDefaults
  @lombok.AllArgsConstructor
  class FieldDefaultsException extends RuntimeException { // NoIssue
    private String name; // WithIssue

    public String getName() {
      return name;
    }
  }
}

class SpringComponentsInjected {

  @Controller
  @lombok.AllArgsConstructor
  public class SampleService1 { // NoIssue
    private final String someField; // NoIssue
  }

  @Service
  @lombok.RequiredArgsConstructor
  public class SampleService2 { // NoIssue
    private final String someField; // NoIssue
  }

  @Repository
  @lombok.NoArgsConstructor(force = true)
  public class SampleService3 { // NoIssue
    private final String someField; // NoIssue
  }

  @Controller
  @lombok.Data
  public class SampleService4 { // NoIssue
    private final String someField; // NoIssue
  }
}

class XxeProcessing {
  public DocumentBuilderFactory test1() {
    val factory = DocumentBuilderFactory.newInstance(); // NoIssue
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory;
  }

  public DocumentBuilderFactory test2() {
    val factory;
    factory = DocumentBuilderFactory.newInstance(); // NoIssue
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory;
  }
}
