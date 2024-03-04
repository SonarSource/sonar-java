package checks;

public class StringIndexofRangesCheckSample {

  int foo(){

    String hello = "Hello, world!";
    int index = 0;

    index += hello.indexOf('o', 11, 7); // Noncompliant
    index += hello.indexOf('o', 7, 11); // Compliant
    index += hello.indexOf('o', -1, 11); // Noncompliant
    index += hello.indexOf('o', 0, 11); // Compliant

    index += hello.indexOf("o", 11, 7); // Noncompliant
    index += hello.indexOf("llo", 7, 11); // Compliant
    index += hello.indexOf("world", -1, 11); // Noncompliant
    index += hello.indexOf("!", 0, 11); // Compliant

    index += hello.indexOf("!", 0, hello.length()); // Compliant

    index += "Hi".indexOf('i', 2, // Noncompliant
      3);  // Noncompliant [[secondary=22]]
    index += "Hi".indexOf('i', 0, 3);  // Noncompliant

    var i = 2;
    index += "Hi".indexOf('i', i, 2);  // Compliant
    index += "Hi".indexOf('i', 0, i);  // Compliant

    index += hello.indexOf('o', 0, hello.length() + 1); // Noncompliant
    index += hello.indexOf('o', 0, 1 + hello.length()); // Noncompliant
    index += hello.indexOf('o', 0, hello.length() + 1 - 5 + 2 + 3); // Compliant
    index += hello.indexOf('o', 0, 1 + hello.length() + 1 - 2 + 5*4); // Compliant

    index += hello.indexOf('o', 0, hello.length() - 1); // Compliant
    index += hello.indexOf('o', 0, hello.length() + 1 - 2*2); // Compliant
    index += hello.indexOf('o', 0, -hello.length() + 215); // Compliant
    index += hello.indexOf('o', 0, 215 - hello.length()); // Compliant
    index += hello.indexOf('o', 215 - hello.length(), 0); // Compliant
    index += hello.indexOf('o', 0, hello.length() * hello.length()); // Compliant

    index += hello.indexOf('o', hello.length() - 2, hello.length() - 5); // Noncompliant
    index += hello.indexOf('o', hello.length() - 5, hello.length() - 2); // Compliant

    index += hello.indexOf('o', hello.length(), // Noncompliant
      hello.length() - 2); // Noncompliant

    index += hello.indexOf('o', hello.length() - 2, hello.hashCode() - 5); // Compliant
    index += hello.indexOf('o', 0, hello.hashCode() + 1); // Compliant

    index += makeAString().indexOf('o', 0, makeAString().length() + 1); // Compliant

    var hi = "Hi!";
    index += hello.indexOf('o', 1, hi.length() + 2);  // Compliant
    index += hello.indexOf('o', 0, 1 + hi.length()); // Compliant

    index += hello.indexOf('o', 1, "Abc".length() + 2);  // Compliant

    return index;
  }

  static String makeAString(){
    return "This is" + " a string";
  }

}
