package checks;

import java.util.Arrays;

class ArrayHashCodeAndToStringCheck {

  Foo foo;

  void method(String[] args, String string) {
    String argStr = args.toString(); // Noncompliant [[sc=26;ec=34]] {{Use "Arrays.toString(array)" instead.}}
    int argHash = args.hashCode(); // Noncompliant {{Use "Arrays.hashCode(array)" instead.}}
    Class class1 = args.getClass();
    String str = string.toString();
    int hash = string.hashCode();
  }

  void testQuickfixes(String[] args) {
    String argStr = args.toString(); // Noncompliant [[sc=26;ec=34;quickfixes=qfToString]] {{Use "Arrays.toString(array)" instead.}}
    // fix@qfToString {{Use "Arrays.toString(array)" instead}}
    // edit@qfToString [[sc=21;ec=36]] {{Arrays.toString(args)}}

    int argHash = args.hashCode(); // Noncompliant [[sc=24;ec=32;quickfixes=qfHashCode]] {{Use "Arrays.hashCode(array)" instead.}}
    // fix@qfHashCode {{Use "Arrays.hashCode(array)" instead}}
    // edit@qfHashCode [[sc=19;ec=34]] {{Arrays.hashCode(args)}}

    argStr = Arrays.toString(args); // Compliant
    argHash = Arrays.hashCode(args); // Compliant

    int[][] array = new int[42][42];
    array[0].toString(); // Noncompliant [[sc=14;ec=22;quickfixes=qfToString1]] {{Use "Arrays.toString(array)" instead.}}
    // fix@qfToString1 {{Use "Arrays.toString(array)" instead}}
    // edit@qfToString1 [[sc=5;ec=24]] {{Arrays.toString(array[0])}}

    this.foo.bar.myArray.toString(); // Noncompliant [[sc=26;ec=34;quickfixes=qfToString2]] {{Use "Arrays.toString(array)" instead.}}
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
