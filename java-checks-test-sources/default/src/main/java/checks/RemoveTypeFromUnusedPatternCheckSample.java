package checks;

import java.util.List;

public class RemoveTypeFromUnusedPatternCheckSample {
  record Guest(String name, String email, String phoneNumber) {
  }

  record Bar(String a, int b){}
  record Foo(String a, Bar b){}

  void nonCompliantExamples(Object o) {
    if (o instanceof Guest(String name, String _, String phoneNumber)) { // Noncompliant
    }

    if(o instanceof Foo(String _, Bar b)){ // Noncompliant {{Remove unused type from unnamed pattern}}
      //                ^^^^^^^^
      //                fix@qf1 {{Remove unused type}}
      //                edit@qf1 [[sc=25;ec=31]] {{}}
      if(b instanceof Bar(String a, int _)){} // Noncompliant {{Remove unused type from unnamed pattern}}
    }

    if(o instanceof Foo(String s, Bar(String _, int b))) { // Noncompliant
    }

    String s1 = switch (o) {
      case Guest(String name, String _, String phoneNumber) -> "Hello " + name + "!"; // Noncompliant
      default -> "Hello!";
    };

    String s2 = switch (o) {
      case Bar(String s, int _) when s.isEmpty() -> ""; // Noncompliant
      //                 ^^^^^
      //                 fix@qf1 {{Remove unused type}}
      //                 edit@qf1 [[sc=26;ec=29]] {{}}
      default -> "Hello!";
    };

    String s3 = switch (o) {
      case Foo(String s, Bar b) when b instanceof Bar(String v, int _) -> ""; // Noncompliant
      default -> "Hello!";
    };



    String s4 = switch (o) {
      case Foo(String s, Bar(String _, int b)) -> ""; // Noncompliant
      default -> "Hello!";
    };
  }

  record Primitives(double a, float b, int c, long d, float e, boolean g){}
  record ClassTypes(Object simple, List<Integer> parametrize, int[] array, List<List<Integer>> nested, List<?> wildcard){}
  record ParametrizeClass<A>(List<A> a){}

  void nonCompliantDifferentTypes(Object o){
    if(o instanceof Primitives(double _, float a, int b, long c, float d, boolean f)){} // Noncompliant
    //                         ^^^^^^^^
    if(o instanceof Primitives(double a, float _, int b, long c, float d, boolean f)){} // Noncompliant
    //                                   ^^^^^^^
    if(o instanceof Primitives(double a, float b, int _, long c, float d, boolean f)){} // Noncompliant
    //                                            ^^^^^
    if(o instanceof Primitives(double a, float b, int c, long _, float d, boolean f)){} // Noncompliant
    //                                                   ^^^^^^
    if(o instanceof Primitives(double a, float b, int c, long d, float _, boolean f)){} // Noncompliant
    //                                                           ^^^^^^^
    if(o instanceof Primitives(double a, float b, int c, long d, float e, boolean _)){} // Noncompliant
    //                                                                    ^^^^^^^^^
    if(o instanceof ClassTypes(Object _, List<Integer> parametrize, int[] array, List<List<Integer>> nested, List<?> wildcard)){} // Noncompliant
    //                         ^^^^^^^^
    if(o instanceof ClassTypes(Object simple, List<Integer> _, int[] array, List<List<Integer>> nested, List<?> wildcard)){} // Noncompliant
    //                                        ^^^^^^^^^^^^^^^
    if(o instanceof ClassTypes(Object simple, List<Integer> parametrize, int[] _, List<List<Integer>> nested, List<?> wildcard)){} // Noncompliant
    //                                                                   ^^^^^^^
    if(o instanceof ClassTypes(Object simple, List<Integer> parametrize, int[] array, List<List<Integer>> _, List<?> wildcard)){} // Noncompliant
    //                                                                                ^^^^^^^^^^^^^^^^^^^^^
    if(o instanceof ClassTypes(Object simple, List<Integer> parametrize, int[] array, List<List<Integer>> nested, List<?> _)){} // Noncompliant
    //                                                                                                            ^^^^^^^^^
    if(o instanceof ParametrizeClass(var _)){} // Noncompliant
    //                               ^^^^^
  }

  void compliantExamples(Object o) {
    if (o instanceof Guest(String name, _, _)) { // compliant
    }

    if(o instanceof Foo(_, Bar b)){ // compliant
      if(b instanceof Bar(String a, _)){} // compliant
    }

    if(o instanceof Foo(String s, Bar(_, int b))) { // compliant
    }


    String s1 = switch (o) {
      case Guest(String name, _, _) -> "Hello " + name + "!"; // compliant
      default -> "Hello!";
    };

    String s2 = switch (o) {
      case Foo(String s, Bar(_, int b)) -> ""; // compliant
      default -> "Hello!";
    };
  }

}
