package checks;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class LambdaA {
  void fun() {
    IntStream.range(1, 5)
        .map((x) -> x * x)
        .map(x -> square(x)) // Noncompliant [[sc=16;ec=18]] {{Replace this lambda with method reference 'this::square'.}}
        .map(x -> { // Noncompliant
          return square(x);
        })
        .map(this::square) //Compliant
        .forEach(System.out::println);
    IntStream.range(1, 5).forEach(x -> System.out.println(x)); // Noncompliant
    IntStream.range(1, 5).forEach(x -> { // Noncompliant
          System.out.println(x);
        });
    IntStream.range(1, 5).forEach(x -> {return;}); // Compliant

    Arrays.asList("bar").stream().filter(string -> string.startsWith("b")); // Compliant
    Arrays.asList(new LambdaA()).stream().filter(a -> a.coolerThan(0, a)); // Compliant

    biConsumer((x, y) -> { ; });
    biConsumer((x, y) -> { ;; });
  }

  void biConsumer(BiConsumer consumer) {
  }

  void runnable(Runnable runnable) {
  }

  void foo(List<String> list, String a) {
    list.stream().map(String::toLowerCase).close();

    list.stream().map(s -> s.toLowerCase()).close(); // Noncompliant
    list.stream().map(s -> { return s.toLowerCase(); }).close(); // Noncompliant

    list.stream().map(s -> new String()).close(); // Compliant
    list.stream().map(s -> a.toLowerCase()).close(); // Compliant
    list.stream().map(s -> s.toLowerCase().toUpperCase()).close(); // Compliant
    list.stream().forEach(s -> fun()); // Compliant
    list.stream().reduce((x, y) -> x.toLowerCase()); // Compliant
  }

  int square(int x) {
    return x * x;
  }
  
  boolean coolerThan(int i, LambdaA a) {
    return true;
  }

  Collection<Number> values = transform(
    input -> getValueProvider().apply(input).getValue() //cannot be replaced by  a method reference.
  );

  Collection<Number> values2 = transform2((input, input2) -> getValueProvider().apply(input)); //cannot be replaced by  a method reference.

  LambdaA getValueProvider() {
    return null;
  }
  LambdaA getValue() {
    return null;
  }
  LambdaA apply(LambdaA a) {
    return a;
  }

  Collection transform(F f) {return null;}
  Collection transform2(F2 f) {return null;}
  interface F2 {
    LambdaA apply(LambdaA a1, LambdaA a2);
  }
  interface F {
    LambdaA apply(LambdaA a1);
  }

  void nullChecks(List<String> strings, String s2) {
    strings.stream().filter(s -> s != null); // Noncompliant {{Replace this lambda with method reference 'Objects::nonNull'.}}
    strings.stream().filter(s -> { return s != null; }); // Noncompliant {{Replace this lambda with method reference 'Objects::nonNull'.}}
    strings.stream().filter(s -> (s) == null); // Noncompliant {{Replace this lambda with method reference 'Objects::isNull'.}}
    strings.stream().filter(s -> null == s); // Noncompliant {{Replace this lambda with method reference 'Objects::isNull'.}}
    strings.stream().filter(s -> (((s == null)))); // Noncompliant {{Replace this lambda with method reference 'Objects::isNull'.}}

    strings.stream().filter(Objects::nonNull); // Compliant
    strings.stream().filter(Objects::isNull); // Compliant

    strings.stream().filter(s -> (((s == s2)))); // Compliant
    strings.stream().filter(s -> (((s2 == s)))); // Compliant
    strings.stream().filter(s -> (((s2 == null)))); // Compliant
    strings.stream().filter(s -> (((null == null)))); // Compliant
  }

  void append(StringBuilder sb, List<Object> list) {
    list.forEach(item -> sb.append("\n").append(item));
  }
}

class LambdaD {

  LambdaD(Object o) { /* ... */ }
  String foo() { return ""; }
  void bar(java.util.function.Supplier<String> supplier) { /* ... */ }

  void test(Object param) {
    bar(() -> new LambdaD(param).foo()); // Compliant - this is not equivalent to the next line
    bar(new LambdaD(param)::foo);
  }
}

class TestA {
  
}


class CastCheck {

  void bar(java.util.function.Function<Object, String> function) { /* ... */ }
  void bar2(java.util.function.BiFunction<Object, Object, String> function) { /* ... */ }
  void bar3(java.util.function.Function<Object, List<String>> function) { /* ... */ }
  void bar4(java.util.function.Function<Object, TestA> function) { /* ... */ }
  void bar5(java.util.function.Function<Object, TestA[]> function) { /* ... */ }
  void bar6(java.util.function.Function<Object, TestA[][]> function) { /* ... */ }
  void bar7(java.util.function.Function<Object, List<TestA[][]>> function) { /* ... */ }
  void bar8(java.util.function.Function<Object, List<TestA[][]>[]> function) { /* ... */ }
  void bar9(java.util.function.Function<Object, Character> f) { /* ... */ }
  
  void  barbar(java.util.function.Predicate<Object> function) { /* ... */ }
  void barbar2(java.util.function.BiPredicate<Object, String> function) { /* ... */ }
  void barbar3(java.util.function.Predicate<List<String>> function) { /* ... */ }
  void barbar4(java.util.function.Predicate<Object> function) { /* ... */ }
  void barbar5(java.util.function.Predicate<Object[]> function) { /* ... */ }
  void barbar6(java.util.function.Predicate<Object[][]> function) { /* ... */ }
  void barbar7(java.util.function.Predicate<List<TestA[][]>> function) { /* ... */ }
  void barbar8(java.util.function.Predicate<List<TestA[][]>[]> function) { /* ... */ }
  void barbar9(java.util.function.Predicate<Object> f) { /* ... */ }

  void testInstanceOf(Object param) {
    barbar((o) -> o instanceof String); // Noncompliant {{Replace this lambda with method reference 'String.class::isInstance'.}}
    barbar(String.class::isInstance); // Compliant
  
    barbar4((o) -> o instanceof TestA); // Noncompliant {{Replace this lambda with method reference 'TestA.class::isInstance'.}}
    barbar4(TestA.class::isInstance); //Compliant
  
    barbar3(List.class::isInstance); //Compliant
    barbar3((o) -> o instanceof List); // Noncompliant {{Replace this lambda with method reference 'List.class::isInstance'.}}

    barbar5(TestA[].class::isInstance); //Compliant
    barbar5((o) -> o instanceof TestA[]); // Noncompliant {{Replace this lambda with method reference 'TestA[].class::isInstance'.}}

    barbar6(TestA[][].class::isInstance); //Compliant
    barbar6((o) -> o instanceof TestA[][]); // Noncompliant {{Replace this lambda with method reference 'TestA[][].class::isInstance'.}}

    barbar7(List.class::isInstance); //Compliant
    barbar7((o) -> o instanceof List ); // Noncompliant {{Replace this lambda with method reference 'List.class::isInstance'.}}

    barbar8(List[].class::isInstance); //Compliant
    barbar8((o) -> o instanceof List[] ); // Noncompliant {{Replace this lambda with method reference 'List[].class::isInstance'.}}

    barbar9(char.class::isInstance); //Compliant
    barbar9(Character.class::isInstance); //Compliant
    barbar9((o) -> o instanceof Character); // Noncompliant {{Replace this lambda with method reference 'Character.class::isInstance'.}}
  }

  void testInstanceOf2(Object param) {
    barbar((o) -> { // Noncompliant {{Replace this lambda with method reference 'String.class::isInstance'.}}
      return o instanceof String;
    });
  }

  void testInstanceOf3(Object param) {
    barbar2((a, b) -> { // Compliant
      return a instanceof String;
    });
  }

  void testInstanceOf4(Object param) {
    barbar(o -> { // Compliant
      return o instanceof String && true;
    });
  }

  void testInstanceOf5(Object param) {
    Object o2 =  new Object();
    barbar(o -> { // Compliant
      return o2 instanceof String;
    });
  }
  
  void testCasts(Object param) {
    bar((o) -> (String)o); // Noncompliant {{Replace this lambda with method reference 'String.class::cast'.}}
    bar(String.class::cast); // Compliant
  
    bar4((o) -> (TestA) o); // Noncompliant {{Replace this lambda with method reference 'TestA.class::cast'.}}
    bar4(TestA.class::cast); //Compliant
  
    bar3(List.class::cast); //Compliant
    bar3((o) -> (List<String>) o); // Compliant, there is no 'List<String>.class::cast'

    bar5(TestA[].class::cast); //Compliant
    bar5((o) -> (TestA[]) o); // Noncompliant {{Replace this lambda with method reference 'TestA[].class::cast'.}}

    bar6(TestA[][].class::cast); //Compliant
    bar6((o) -> (TestA[][]) o); // Noncompliant {{Replace this lambda with method reference 'TestA[][].class::cast'.}}

    bar7(List.class::cast); //Compliant
    bar7((o) -> (List<TestA[][]>) o); // Compliant, there is no 'List<TestA[][]>.class::cast'

    bar8(List[].class::cast); //Compliant
    bar8((o) -> (List<TestA[][]>[]) o); // Compliant, there is no 'List<TestA[][]>[].class::cast'

    bar9(char.class::cast); //Compliant
    bar9(Character.class::cast); //Compliant
    bar9((o) -> (char) o); // Noncompliant {{Replace this lambda with method reference 'char.class::cast'.}}
    bar9((o) -> (Character) o); // Noncompliant {{Replace this lambda with method reference 'Character.class::cast'.}}

    "abc".chars().mapToObj(i -> (char) i); // Compliant, char::cast takes an Object as its argument causing i to be implicitly converted to Integer,
    "abc".chars().mapToObj(char.class::cast); // since casting Integer to char is invalid, this change would turn a valid cast into an invalid one
  }

  void testCasts2(Object param) {
    bar((o) -> { // Noncompliant {{Replace this lambda with method reference 'String.class::cast'.}}
      return (String)o;
    });
  }

  void testCasts3(Object param) {
    bar2((a, b) -> { // Compliant
      return (String)a;
    });
  }

  void testCasts4(Object param) {
    bar(o -> { // Compliant
      return (String)o.getClass().getCanonicalName();
    });
  }

  void testCasts5(Object param) {
    Object o2 =  new Object();
    bar(o -> { // Compliant
      return (String)o2;
    });
  }

  void quickFixes(List<String> strings) {
    IntStream.range(1, 5).forEach(x -> staticMethod(x)); // Noncompliant [[sc=37;ec=39;quickfixes=qf1]] {{Replace this lambda with method reference 'CastCheck::staticMethod'.}}
    // fix@qf1 {{Replace with "CastCheck::staticMethod"}}
    // edit@qf1 [[sc=35;ec=55]] {{CastCheck::staticMethod}}
    IntStream.range(1, 5).forEach(x -> notStatic(x)); // Noncompliant [[sc=37;ec=39;quickfixes=qf2]] {{Replace this lambda with method reference 'this::notStatic'.}}
    // fix@qf2 {{Replace with "this::notStatic"}}
    // edit@qf2 [[sc=35;ec=52]] {{this::notStatic}}
    Nested n = new Nested(1);
    IntStream.range(1, 5).forEach(x -> n.takeInt(x)); // Noncompliant [[sc=37;ec=39;quickfixes=qf3]] {{Replace this lambda with method reference 'n::takeInt'.}}
    // fix@qf3 {{Replace with "n::takeInt"}}
    // edit@qf3 [[sc=35;ec=52]] {{n::takeInt}}
    IntStream.range(1, 5).forEach(x -> NestedStatic.takeIntStatic(x)); // FN, do not report an issue because NestedStatic is not final

    IntStream.range(1, 5).forEach(x -> new Nested(x)); // Noncompliant [[sc=37;ec=39;quickfixes=qf_new_class]] {{Replace this lambda with method reference 'Nested::new'.}}
    // fix@qf_new_class {{Replace with "Nested::new"}}
    // edit@qf_new_class [[sc=35;ec=53]] {{Nested::new}}
    IntStream.range(1, 5).forEach(x -> new CastCheck.Nested(x)); // Noncompliant [[sc=37;ec=39;quickfixes=qf_new_class2]] {{Replace this lambda with method reference 'CastCheck.Nested::new'.}}
    // fix@qf_new_class2 {{Replace with "CastCheck.Nested::new"}}
    // edit@qf_new_class2 [[sc=35;ec=63]] {{CastCheck.Nested::new}}

    IntStream.range(1, 5).forEach(x -> new NestedStatic.NestedInNested(x)); // Noncompliant [[sc=37;ec=39;quickfixes=qf_new_class3]] {{Replace this lambda with method reference 'NestedStatic.NestedInNested::new'.}}
    // fix@qf_new_class3 {{Replace with "NestedStatic.NestedInNested::new"}}
    // edit@qf_new_class3 [[sc=35;ec=74]] {{NestedStatic.NestedInNested::new}}

    strings.stream()
      .map(s -> s.toLowerCase()) // Noncompliant [[sc=14;ec=16;quickfixes=qf4]] {{Replace this lambda with method reference 'String::toLowerCase'.}}
      // fix@qf4 {{Replace with "String::toLowerCase"}}
      // edit@qf4 [[sc=12;ec=32]] {{String::toLowerCase}}
      .map(s -> { // Noncompliant [[sc=14;ec=16;quickfixes=qf5]]
        return s.toLowerCase();
      })
      // fix@qf5 {{Replace with "String::toLowerCase"}}
      // edit@qf5 [[sc=12;el=+2;ec=8]] {{String::toLowerCase}}
      .forEach(x -> System.out.println(x)); // Noncompliant [[sc=18;ec=20;quickfixes=qf6]] {{Replace this lambda with method reference 'System.out::println'.}}
    // fix@qf6 {{Replace with "System.out::println"}}
    // edit@qf6 [[sc=16;ec=42]] {{System.out::println}}

    strings.stream().filter(s -> s != null); // Noncompliant  [[sc=31;ec=33;quickfixes=qf_null1]]
    // fix@qf_null1 {{Replace with "Objects::nonNull"}}
    // edit@qf_null1 [[sc=29;ec=43]] {{Objects::nonNull}}
    strings.stream().filter(s -> (s) == null);// Noncompliant  [[sc=31;ec=33;quickfixes=qf_null2]]
    // fix@qf_null2 {{Replace with "Objects::isNull"}}
    // edit@qf_null2 [[sc=29;ec=45]] {{Objects::isNull}}
    barbar((o) -> o instanceof String); // Noncompliant  [[sc=16;ec=18;quickfixes=qf_instance_of]]
    // fix@qf_instance_of {{Replace with "String.class::isInstance"}}
    // edit@qf_instance_of [[sc=12;ec=38]] {{String.class::isInstance}}
    bar((o) -> (String) o); // Noncompliant [[sc=13;ec=15;quickfixes=qf_cast1]]
    // fix@qf_cast1 {{Replace with "String.class::cast"}}
    // edit@qf_cast1 [[sc=9;ec=26]] {{String.class::cast}}
    bar6((o) -> (TestA[][]) o); // Noncompliant [[sc=14;ec=16;quickfixes=qf_cast2]]
    // fix@qf_cast2 {{Replace with "TestA[][].class::cast"}}
    // edit@qf_cast2 [[sc=10;ec=30]] {{TestA[][].class::cast}}
  }

  int notStatic(int x) {
    return x * x;
  }

  static int staticMethod(int x) {
    return x * x * x;
  }

  class Nested {
    {
      IntStream.range(1, 5).forEach(x -> takeInt(x)); // Noncompliant [[sc=39;ec=41;quickfixes=qf_init]] {{Replace this lambda with method reference 'Nested.this::takeInt'.}}
      // Could be simpler: "this::takeInt", but add the class name since we can not get the enclosing method
      // fix@qf_init {{Replace with "Nested.this::takeInt"}}
      // edit@qf_init [[sc=37;ec=52]] {{Nested.this::takeInt}}

      IntStream.range(1, 5).forEach(x -> notStatic(x)); // Noncompliant [[sc=39;ec=41;quickfixes=qf_init2]] {{Replace this lambda with method reference 'CastCheck.this::notStatic'.}}
      // fix@qf_init2 {{Replace with "CastCheck.this::notStatic"}}
      // edit@qf_init2 [[sc=37;ec=54]] {{CastCheck.this::notStatic}}
    }

    Nested(int x) {

    }

    void quickFixInNestedClass() {
      IntStream.range(1, 5).forEach(x -> notStatic(x)); // Noncompliant [[sc=39;ec=41;quickfixes=qf_this]] {{Replace this lambda with method reference 'CastCheck.this::notStatic'.}}
      // fix@qf_this {{Replace with "CastCheck.this::notStatic"}}
      // edit@qf_this [[sc=37;ec=54]] {{CastCheck.this::notStatic}}

      IntStream.range(1, 5).forEach(x -> staticMethod(x)); // Noncompliant [[sc=39;ec=41;quickfixes=qf_this2]] {{Replace this lambda with method reference 'CastCheck::staticMethod'.}}
      // fix@qf_this2 {{Replace with "CastCheck::staticMethod"}}
      // edit@qf_this2 [[sc=37;ec=57]] {{CastCheck::staticMethod}}
    }

    class NestedDeeper {
      void quickFixInNestedClass() {
        IntStream.range(1, 5).forEach(x -> notStatic(x)); // Noncompliant [[sc=41;ec=43;quickfixes=qf_this3]] {{Replace this lambda with method reference 'CastCheck.this::notStatic'.}}
        // fix@qf_this3 {{Replace with "CastCheck.this::notStatic"}}
        // edit@qf_this3 [[sc=39;ec=56]] {{CastCheck.this::notStatic}}
      }
    }

    public int takeInt(int x) {
      return 1 + x;
    }

    public int doSomething() {
      return 1;
    }
  }

  void quickFixInNestedClass() {
    Stream.of(new NestedExtend()).forEach(x -> x.doSomething()); // Noncompliant [[sc=45;ec=47;quickfixes=qf_method_override]]
    // fix@qf_method_override {{Replace with "Nested::doSomething"}}
    // edit@qf_method_override [[sc=43;ec=63]] {{Nested::doSomething}}

    Stream.of(new NestedExtendOverrideTakeInt()).forEach(x -> x.doSomething()); // Noncompliant [[sc=60;ec=62;quickfixes=qf_method_override2]]
    // fix@qf_method_override2 {{Replace with "NestedExtendOverrideTakeInt::doSomething"}}
    // edit@qf_method_override2 [[sc=58;ec=78]] {{NestedExtendOverrideTakeInt::doSomething}}
  }

  class NestedExtend extends Nested {
    NestedExtend() {
      super(1);
    }
  }

  class NestedExtendOverrideTakeInt extends Nested {
    NestedExtendOverrideTakeInt() {
      super(1);
    }

    public int doSomething() {
      return 1;
    }
  }

  static class NestedStatic {
    public static int takeIntStatic(int x) {
      return 1 + x;
    }

    static class NestedInNested {
      NestedInNested(int x) {

      }
    }
  }
}

