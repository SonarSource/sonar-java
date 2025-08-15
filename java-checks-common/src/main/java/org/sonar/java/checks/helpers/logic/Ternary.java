package org.sonar.java.checks.helpers.logic;

import java.util.stream.Collector;

/**
 * Implementation of tree-valued logic in which a proposition can be true, false, or unknown.
 *
 * <p>This class allows us to be precise in certain predicates, for instance
 * {@code type.isSubtypeOf("com.example.MySuper")}, where the exact value is unknown
 * due to missing semantics.
 */
public enum Ternary {
  TRUE, FALSE, UNKNOWN;

  /**
   * Adapter for {@code boolean}.
   */
  public static Ternary of(boolean value) {
    return value ? TRUE : FALSE;
  }

  /**
   * Adapter for {@code boolean} where {@code null} means {@code UNKNOWN}.
   */
  public static Ternary ofNullable(Boolean value) {
    return value == null ? UNKNOWN : of(value);
  }

  /**
   * Checks whether the object is exactly {@code TRUE} or {@code FALSE}.
   */
  public boolean is(boolean value) {
    if (value) {
      return this == TRUE;
    } else {
      return this == FALSE;
    }
  }

  /**
   * The value is known and true.
   */
  public boolean isTrue() {
    return this == TRUE;
  }

  /**
   * The value is known and false.
   */
  public boolean isFalse() {
    return this == FALSE;
  }

  public boolean maybeTrue() {
    return this == TRUE || this == UNKNOWN;
  }

  public boolean maybeFalse() {
    return this == FALSE || this == UNKNOWN;
  }

  /**
   * Negation. Unknown stays unknown, otherwise the usual boolean logic.
   */
  public Ternary not() {
    return switch (this) {
      case TRUE -> FALSE;
      case FALSE -> TRUE;
      case UNKNOWN -> UNKNOWN;
    };
  }

  /**
   * Alternative:
   * <ul>
   *   <li> {@code TRUE} if any argument is true,
   *   <li> {@code FALSE} if all arguments are false,
   *   <li> {@code UNKNOWN} otherwise.
   * </ul>
   */
  public static Ternary or(Ternary... args) {
    return new Summary().addAll(args).logicalOr();
  }

  /**
   * Conjunction:
   * <ul>
   *   <li> {@code TRUE} if all arguments are true,
   *   <li> {@code FALSE} if any argument is false,
   *   <li> {@code UNKNOWN} otherwise.
   * </ul>
   */
  public static Ternary and(Ternary... args) {
    return new Summary().addAll(args).logicalAnd();
  }

  /**
   * Returns a collector creating a new ternary value following the logic of {@link #and(Ternary...)}.
   */
  public static Collector<Ternary, Summary, Ternary> and() {
    return Collector.of(Summary::new, Summary::add, Summary::combine, Summary::logicalAnd);
  }

  /**
   * Returns a collector creating a new ternary value following the logic of {@link #or(Ternary...)}.
   */
  public static Collector<Ternary, Summary, Ternary> or() {
    return Collector.of(Summary::new, Summary::add, Summary::combine, Summary::logicalOr);
  }
}
