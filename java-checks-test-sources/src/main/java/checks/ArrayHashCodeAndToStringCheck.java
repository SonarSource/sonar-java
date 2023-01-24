package checks;

import java.util.Arrays;

class ArrayHashCodeAndToStringCheck {

  void method(String[] args, String string) {
    String argStr = args.toString(); // Noncompliant [[sc=26;ec=34]] {{Use "Arrays.toString(array)" instead.}}
    int argHash = args.hashCode(); // Noncompliant {{Use "Arrays.hashCode(array)" instead.}}
    Class class1 = args.getClass();
    String str = string.toString();
    int hash = string.hashCode();
  }

  void testQuickfixes(String[] args) {
    String argStr = args.toString(); // Noncompliant [[sc=26;ec=34;quickfixes=qfToString]] {{Use "Arrays.toString(array)" instead.}}
    // fix@qfToString {{Use "Arrays.toString(array)" instead.}}
    // edit@qfToString [[sc=21;ec=36]] {{Arrays.toString(args)}}

    int argHash = args.hashCode(); // Noncompliant [[sc=24;ec=32;quickfixes=qfHashCode]] {{Use "Arrays.hashCode(array)" instead.}}
    // fix@qfHashCode {{Use "Arrays.hashCode(array)" instead.}}
    // edit@qfHashCode [[sc=19;ec=34]] {{Arrays.hashCode(args)}}

    argStr = Arrays.toString(args); // Compliant
    argHash = Arrays.hashCode(args); // Compliant
  }
}
