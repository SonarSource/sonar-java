package checks.regex;

import java.util.regex.Pattern;

public class EmptyRegexGroupCheckSample {

  void nonCompliant() {
    Pattern.compile("foo()bar"); // Noncompliant {{Remove this empty group.}}
//                      ^^
    Pattern.compile("foo(?:)bar"); // Noncompliant
//                      ^^^^
    Pattern.compile("foo(?>)bar"); // Noncompliant
    Pattern.compile("foo(?=)bar"); // Noncompliant
    Pattern.compile("foo(?!)bar"); // Noncompliant
    Pattern.compile("foo(?<=)bar"); // Noncompliant
    Pattern.compile("foo(?<!)bar"); // Noncompliant
//                      ^^^^^
    Pattern.compile("foo(?<name>)bar"); // Noncompliant

    Pattern.compile("(foo()bar)"); // Noncompliant
//                       ^^
    Pattern.compile("(foo(?:)bar)"); // Noncompliant
//                       ^^^^
    Pattern.compile("(foo(?>)bar)"); // Noncompliant
    Pattern.compile("(foo(?=)bar)"); // Noncompliant
    Pattern.compile("(foo(?!)bar)"); // Noncompliant
    Pattern.compile("(foo(?<=)bar)"); // Noncompliant
//                       ^^^^^
    Pattern.compile("(foo(?<!)bar)"); // Noncompliant
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
