package checks.regex;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class PossessiveQuantifierContinuationCheckSample {

  public void f(Pattern pattern) {
    f(compile("a+abc"));
    f(compile("a+?abc"));
    f(compile("a++abc")); // Noncompliant {{Change this impossible to match sub-pattern that conflicts with the previous possessive quantifier.}}
//                ^
//  ^^^<
    f(compile("\\d*+[02468]")); // Noncompliant
//                  ^^^^^^^
    f(compile("(\\d)*+([02468])")); // Noncompliant
//                    ^^^^^^^^^
    f(compile("\\d++(?:[eE][+-]?\\d++)?[fFdD]?"));
    f(compile("a*+\\s"));
    f(compile("[+-]?(?:NaN|Infinity|(?:\\d++(?:\\.\\d*+)?|\\.\\d++)(?:[eE][+-]?\\d++)?[fFdD]?|0[xX](?:\\p{XDigit}++(?:\\.\\p{XDigit}*+)?|\\.\\p{XDigit}++)[pP][+-]?\\d++[fFdD]?)"));
    f(compile("aa++bc"));
    f(compile("\\d*+(?<=[02468])"));
    f(compile("(xx++)+x")); // Noncompliant
//                    ^
    f(compile("(bx++)+x")); // Noncompliant
//                    ^
    f(compile("(?:xx++)+x")); // Noncompliant
//                      ^
    f(compile("(xx++)x")); // Noncompliant
//                   ^
    f(compile(".*+\\w")); // Noncompliant
//                ^^^
    f(compile(".*+\\w+")); // Noncompliant
//                ^^^^
    f(compile("(a|b|c)*+(a|b)")); // Noncompliant
//                      ^^^^^
    f(compile("(:[0-9])?+(:[0-9])?+"));
  }

}
