package checks;

import java.util.Arrays;

class ArrayHashCodeAndToStringCheckSample {

  Foo foo;

  void method(String[] args, String string) {
    String argStr = args.toString(); // Noncompliant {{Use "Arrays.toString(array)" instead.}}
//                       ^^^^^^^^
    int argHash = args.hashCode(); // Noncompliant {{Use "Arrays.hashCode(array)" instead.}}
    Class class1 = args.getClass();
    String str = string.toString();
    int hash = string.hashCode();
  }

  void testQuickfixes(String[] args) {
    String argStr = args.toString(); // Noncompliant {{Use "Arrays.toString(array)" instead.}} [[quickfixes=qfToString]]
//                       ^^^^^^^^
    // fix@qfToString {{Use "Arrays.toString(array)" instead}}
    // edit@qfToString [[sc=21;ec=36]] {{Arrays.toString(args)}}

    int argHash = args.hashCode(); // Noncompliant {{Use "Arrays.hashCode(array)" instead.}} [[quickfixes=qfHashCode]]
//                     ^^^^^^^^
    // fix@qfHashCode {{Use "Arrays.hashCode(array)" instead}}
    // edit@qfHashCode [[sc=19;ec=34]] {{Arrays.hashCode(args)}}

    argStr = Arrays.toString(args); // Compliant
    argHash = Arrays.hashCode(args); // Compliant

    int[][] array = new int[42][42];
    array[0].toString(); // Noncompliant {{Use "Arrays.toString(array)" instead.}} [[quickfixes=qfToString1]]
//           ^^^^^^^^
    // fix@qfToString1 {{Use "Arrays.toString(array)" instead}}
    // edit@qfToString1 [[sc=5;ec=24]] {{Arrays.toString(array[0])}}

    this.foo.bar.myArray.toString(); // Noncompliant {{Use "Arrays.toString(array)" instead.}} [[quickfixes=qfToString2]]
//                       ^^^^^^^^
    // fix@qfToString2 {{Use "Arrays.toString(array)" instead}}
    // edit@qfToString2 [[sc=5;ec=36]] {{Arrays.toString(this.foo.bar.myArray)}}
  }

  class Foo {
    Bar bar;
  }

  class Bar {
    int[] myArray = new int[42];
  }
}
