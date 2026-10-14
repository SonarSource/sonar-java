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
    return !verbose ? "short" : "long"; // Noncompliant {{Invert this negated condition and swap the two branches of the ternary.}}
//         ^^^^^^^^
  }

  String notEqualComparison() {
    return count != 0 ? "some" : "none"; // Noncompliant
//         ^^^^^^^^^^
  }

  String negatedMethodCall() {
    return !items.isEmpty() ? items.get(0) : "none"; // Noncompliant
//         ^^^^^^^^^^^^^^^^
  }

  String negatedEqualsCall() {
    return !"SHIPPED".equals(state) ? "Pending" : "Shipped"; // Noncompliant
//         ^^^^^^^^^^^^^^^^^^^^^^^^
  }

  String parenthesizedCondition() {
    return (!verbose) ? "short" : "long"; // Noncompliant
//         ^^^^^^^^^^
  }

  String negatedConjunction() {
    return !(verbose && flag) ? "short" : "long"; // Noncompliant
//         ^^^^^^^^^^^^^^^^^^
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
