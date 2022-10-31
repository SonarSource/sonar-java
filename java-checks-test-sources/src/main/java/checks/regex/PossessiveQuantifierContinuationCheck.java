package checks.regex;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class PossessiveQuantifierContinuationCheck {

  public void f(Pattern pattern) {
    f(compile("a+abc"));
    f(compile("a+?abc"));
    f(compile("a++abc")); // Noncompliant [[sc=19;ec=20;secondary=12]] {{Change this impossible to match sub-pattern that conflicts with the previous possessive quantifier.}}
    f(compile("\\d*+[02468]")); // Noncompliant [[sc=21;ec=28]]
    f(compile("(\\d)*+([02468])")); // Noncompliant [[sc=23;ec=32]]
    f(compile("\\d++(?:[eE][+-]?\\d++)?[fFdD]?"));
    f(compile("a*+\\s"));
    f(compile("[+-]?(?:NaN|Infinity|(?:\\d++(?:\\.\\d*+)?|\\.\\d++)(?:[eE][+-]?\\d++)?[fFdD]?|0[xX](?:\\p{XDigit}++(?:\\.\\p{XDigit}*+)?|\\.\\p{XDigit}++)[pP][+-]?\\d++[fFdD]?)"));
    f(compile("aa++bc"));
    f(compile("\\d*+(?<=[02468])"));
    f(compile("(xx++)+x")); // Noncompliant [[sc=23;ec=24]]
    f(compile("(bx++)+x")); // false-negative, limitation of the algorithm when there's infinite loop
    f(compile("(?:xx++)+x")); // Noncompliant [[sc=25;ec=26]]
    f(compile("(xx++)x")); // Noncompliant [[sc=22;ec=23]]
    f(compile(".*+\\w")); // Noncompliant [[sc=19;ec=22]]
    f(compile(".*+\\w+")); // Noncompliant [[sc=19;ec=23]]
    f(compile("(a|b|c)*+(a|b)")); // Noncompliant [[sc=25;ec=30]]
    f(compile("(:[0-9])?+(:[0-9])?+"));
  }

}
