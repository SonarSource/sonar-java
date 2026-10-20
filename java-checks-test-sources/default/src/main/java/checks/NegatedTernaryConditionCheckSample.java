package checks;

import java.util.List;

public class NegatedTernaryConditionCheckSample {

  private boolean verbose;
  private boolean flag;
  private int count;
  private String state;
  private Object value;
  private List<String> items;

  String negatedBooleanVariable() {
    return !verbose ? "short" : "long"; // Noncompliant {{Invert this negated condition and swap the two branches of the ternary.}} [[quickfixes=qf1]]
//         ^^^^^^^^
    // fix@qf1 {{Invert the condition and swap the branches}}
    // edit@qf1 [[sc=12;ec=20]] {{verbose}}
    // edit@qf1 [[sc=23;ec=30]] {{"long"}}
    // edit@qf1 [[sc=33;ec=39]] {{"short"}}
  }

  String notEqualComparison() {
    return count != 0 ? "some" : "none"; // Noncompliant [[quickfixes=qf2]]
//         ^^^^^^^^^^
    // fix@qf2 {{Invert the condition and swap the branches}}
    // edit@qf2 [[sc=18;ec=20]] {{==}}
    // edit@qf2 [[sc=25;ec=31]] {{"none"}}
    // edit@qf2 [[sc=34;ec=40]] {{"some"}}
  }

  String negatedMethodCall() {
    return !items.isEmpty() ? items.get(0) : "none"; // Noncompliant [[quickfixes=qf3]]
//         ^^^^^^^^^^^^^^^^
    // fix@qf3 {{Invert the condition and swap the branches}}
    // edit@qf3 [[sc=12;ec=28]] {{items.isEmpty()}}
    // edit@qf3 [[sc=31;ec=43]] {{"none"}}
    // edit@qf3 [[sc=46;ec=52]] {{items.get(0)}}
  }

  String negatedEqualsCall() {
    return !"SHIPPED".equals(state) ? "Pending" : "Shipped"; // Noncompliant
//         ^^^^^^^^^^^^^^^^^^^^^^^^
  }

  String parenthesizedCondition() {
    return (!verbose) ? "short" : "long"; // Noncompliant [[quickfixes=qf4]]
//         ^^^^^^^^^^
    // fix@qf4 {{Invert the condition and swap the branches}}
    // edit@qf4 [[sc=12;ec=22]] {{verbose}}
    // edit@qf4 [[sc=25;ec=32]] {{"long"}}
    // edit@qf4 [[sc=35;ec=41]] {{"short"}}
  }

  String negatedConjunction() {
    return !(verbose && flag) ? "short" : "long"; // Noncompliant [[quickfixes=qf5]]
//         ^^^^^^^^^^^^^^^^^^
    // fix@qf5 {{Invert the condition and swap the branches}}
    // edit@qf5 [[sc=12;ec=30]] {{(verbose && flag)}}
    // edit@qf5 [[sc=33;ec=40]] {{"long"}}
    // edit@qf5 [[sc=43;ec=49]] {{"short"}}
  }

  String inLocalVariable() {
    String label = !flag ? "off" : "on"; // Noncompliant
//                 ^^^^^
    return label;
  }

  String asArgument() {
    return String.valueOf(!flag ? "off" : "on"); // Noncompliant
//                        ^^^^^
  }

  String nestedTernaryInBranch() {
    return verbose ? (!flag ? "off" : "on") : "none"; // Noncompliant
//                    ^^^^^
  }

  String bothTernariesNegated() {
    return !verbose // Noncompliant
//         ^^^^^^^^
      ? "short"
      : (count != 0 ? "some" : "none"); // Noncompliant
//       ^^^^^^^^^^
  }

  int assignmentMovedToFalseBranch(int x, int y) {
    return !flag ? x = 1 : y; // Noncompliant [[quickfixes=qf6]]
//         ^^^^^
    // fix@qf6 {{Invert the condition and swap the branches}}
    // edit@qf6 [[sc=12;ec=17]] {{flag}}
    // edit@qf6 [[sc=20;ec=25]] {{y}}
    // edit@qf6 [[sc=28;ec=29]] {{(x = 1)}}
  }

  int compoundAssignmentMovedToFalseBranch(int x, int y) {
    return !flag ? x += 1 : y; // Noncompliant [[quickfixes=qf7]]
//         ^^^^^
    // fix@qf7 {{Invert the condition and swap the branches}}
    // edit@qf7 [[sc=12;ec=17]] {{flag}}
    // edit@qf7 [[sc=20;ec=26]] {{y}}
    // edit@qf7 [[sc=29;ec=30]] {{(x += 1)}}
  }

  int parenthesizedAssignmentStaysAsIs(int x, int y) {
    return !flag ? (x = 1) : y; // Noncompliant [[quickfixes=qf8]]
//         ^^^^^
    // fix@qf8 {{Invert the condition and swap the branches}}
    // edit@qf8 [[sc=12;ec=17]] {{flag}}
    // edit@qf8 [[sc=20;ec=27]] {{y}}
    // edit@qf8 [[sc=30;ec=31]] {{(x = 1)}}
  }

  // Compliant cases

  String positiveCondition() {
    return verbose ? "long" : "short";
  }

  String equalComparison() {
    return count == 0 ? "none" : "some";
  }

  String positiveMethodCall() {
    return items.isEmpty() ? "none" : items.get(0);
  }

  String nullCheckIsExempt() {
    return value != null ? value.toString() : "unknown";
  }

  String reversedNullCheckIsExempt() {
    return null != value ? value.toString() : "unknown";
  }

  String negatedNullEqualityIsExempt() {
    return !(value == null) ? value.toString() : "unknown";
  }

  // S1940 already reports a negation wrapped around a comparison, so this rule stays silent.
  String negatedRelationalIsCoveredByS1940() {
    return !(count > 0) ? "nonpositive" : "positive";
  }

  String negatedEqualityIsCoveredByS1940() {
    return !(count == 0) ? "some" : "none";
  }

  String negatedInequalityIsCoveredByS1940() {
    return !(count != 0) ? "none" : "some";
  }

  String parenthesizedNegatedComparisonIsCoveredByS1940() {
    return (!(count <= 0)) ? "positive" : "nonpositive";
  }

  String relationalComparison() {
    return count > 0 ? "some" : "none";
  }

  String conjunctionIsNotNegatedAtTopLevel() {
    return verbose && flag ? "both" : "other";
  }

  String nestedNotEqualIsNotNegatedAtTopLevel() {
    return value != null && count != 0 ? "both" : "other";
  }

  String negatedConditionInIfStatementIsOutOfScope() {
    if (!verbose) {
      return "short";
    } else {
      return "long";
    }
  }

  boolean negationOutsideTernary() {
    return !verbose;
  }

  boolean notEqualOutsideTernary() {
    return count != 0;
  }
}
