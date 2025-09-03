package org.sonar.java.checks.helpers.logic;

import org.sonar.plugins.java.api.semantic.Symbol;

import static org.sonar.java.checks.helpers.logic.Ternary.UNKNOWN;

/**
 * Demo for {@link Ternary}
 */
public class HelperPredicates {
  private HelperPredicates() {}

  public static Ternary isUsed(Symbol symbol) {
    if(symbol.isUnknown()) {
      return UNKNOWN;
    }
    return Ternary.of(!symbol.usages().isEmpty());
  }
}
