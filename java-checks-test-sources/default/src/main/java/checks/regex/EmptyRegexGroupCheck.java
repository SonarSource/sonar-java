package checks.regex;

import java.util.regex.Pattern;

public class EmptyRegexGroupCheck {

  void nonCompliant() {
    Pattern.compile("foo()bar");        // Noncompliant [[sc=25;ec=27]] {{Remove this empty group.}}
    Pattern.compile("foo(?:)bar");      // Noncompliant [[sc=25;ec=29]]
    Pattern.compile("foo(?>)bar");      // Noncompliant
    Pattern.compile("foo(?=)bar");      // Noncompliant
    Pattern.compile("foo(?!)bar");      // Noncompliant
    Pattern.compile("foo(?<=)bar");     // Noncompliant
    Pattern.compile("foo(?<!)bar");     // Noncompliant [[sc=25;ec=30]]
    Pattern.compile("foo(?<name>)bar"); // Noncompliant

    Pattern.compile("(foo()bar)");      // Noncompliant [[sc=26;ec=28]]
    Pattern.compile("(foo(?:)bar)");    // Noncompliant [[sc=26;ec=30]]
    Pattern.compile("(foo(?>)bar)");    // Noncompliant
    Pattern.compile("(foo(?=)bar)");    // Noncompliant
    Pattern.compile("(foo(?!)bar)");    // Noncompliant
    Pattern.compile("(foo(?<=)bar)");   // Noncompliant [[sc=26;ec=31]]
    Pattern.compile("(foo(?<!)bar)");   // Noncompliant
    Pattern.compile("foo(?<name>)bar"); // Noncompliant
  }

  void compliant() {
    Pattern.compile("foo(x)bar");
    Pattern.compile("foo(?:x)bar");
    Pattern.compile("foo(?>x)bar");
    Pattern.compile("foo(?=x)bar");
    Pattern.compile("foo(?!x)bar");
    Pattern.compile("foo(?<=x)bar");
    Pattern.compile("foo(?<!x)bar");
    Pattern.compile("foo(?<name>x)bar");

    Pattern.compile("foo(?-)bar");
    Pattern.compile("foo(?-x)bar");
    Pattern.compile("(foo(?-)bar)");

    Pattern.compile("[foo()bar]");
    Pattern.compile("[foo(?:)bar]");
    Pattern.compile("[foo(?>)bar]");
    Pattern.compile("[foo(?=x)bar]");
    Pattern.compile("[foo(?!x)bar]");
    Pattern.compile("[foo(?<=x)bar]");
    Pattern.compile("[foo(?<!x)bar]");
    Pattern.compile("[foo(?<name>)bar]");

    Pattern.compile("(foo(|)bar)");
    Pattern.compile("(foo(?:|)bar)");
    Pattern.compile("(foo(?>|)bar)");
    Pattern.compile("(foo(?=|)bar)");
    Pattern.compile("(foo(?!|)bar)");
    Pattern.compile("(foo(?<=|)bar)");
    Pattern.compile("(foo(?<!|)bar)");
  }

}
