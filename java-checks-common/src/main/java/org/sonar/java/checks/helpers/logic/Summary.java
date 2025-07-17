package org.sonar.java.checks.helpers.logic;

import static org.sonar.java.checks.helpers.logic.Ternary.FALSE;
import static org.sonar.java.checks.helpers.logic.Ternary.TRUE;
import static org.sonar.java.checks.helpers.logic.Ternary.UNKNOWN;

/**
 * Utility to for implementing logical "and" and  "or" in {@link Ternary}, both
 * directly and as a {@link java.util.stream.Collector}.
 */
class Summary {
  private boolean allTrue = true;
  private boolean anyTrue = false;
  private boolean allFalse = true;
  private boolean anyFalse = false;

  void add(Ternary arg) {
    allTrue &= arg.is(true);
    anyTrue |= arg.is(true);
    allFalse &= arg.is(false);
    anyFalse |= arg.is(false);
  }

  Summary addAll(Ternary... args) {
    for (Ternary arg : args) {
      add(arg);
    }
    return this;
  }

  Summary combine(Summary other) {
    allTrue &= other.allTrue;
    anyTrue |= other.anyTrue;
    allFalse &= other.allFalse;
    anyFalse |= other.anyFalse;
    return this;
  }

  Ternary logicalAnd() {
    if (allTrue) {
      return TRUE;
    } else if (anyFalse) {
      return FALSE;
    } else {
      return UNKNOWN;
    }
  }

  Ternary logicalOr() {
    if (allFalse) {
      return FALSE;
    } else if (anyTrue) {
      return TRUE;
    } else {
      return UNKNOWN;
    }
  }
}
