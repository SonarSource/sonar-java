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
  }

}
