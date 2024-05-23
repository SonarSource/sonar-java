package symbolicexecution.checks;

import java.util.Random;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class PrimitiveUnboxing {
  String stringConcatenationIsCompliant(@Nullable String name) {
    return '>' + name; // Compliant because null can be concatenated to other primitives and strings
  }

  void unboxingForComparisonAgainstNullIsCompliant(String key) {
    Boolean nullCondition = null;
    while (nullCondition == null) { // Compliant because compared to null
      nullCondition = Boolean.valueOf("true");
    }
    Float nullNumber = null;
    while (null == nullNumber) { // Compliant because compared to null
      nullNumber = Float.valueOf(3.14f);
    }

    while (null == null) {
      // do something
    }
  }

  void unboxYieldToBoxedTypeIsCompliant(String key) {
    Integer boxed = null;
    // If the switch expression returns to a boxed variable, then there is no need to raise
    Integer boxedAsWell = switch (key) {
      case "nullable":
        yield getANullabbleInteger(); // Compliant
      case "primitive":
        yield 42;
      default:
        yield boxed; // Compliant
    };
  }

  boolean unboxIdentifierOnReturn() {
    Boolean boxed = null;
    return boxed; // Noncompliant
//         ^^^^^
  }

  class VolatileFieldsAreCompliantFalseNegatives {
    private volatile Long boxedVolatile = null;

    public long getBoxedVolatile() {
      return boxedVolatile; // Compliant FN because the engine does look into the symbolic values of volatile fields
    }
  }

  int unboxExpressionOnReturn() {
    return getANullabbleInteger(); // Noncompliant
//         ^^^^^^^^^^^^^^^^^^^^^^
  }

  void unboxOnExplicitYield(String key) {
    Integer boxed = null;
    int primitive = switch (key) {
      case "nullable":
        yield getANullabbleInteger(); // Noncompliant
//            ^^^^^^^^^^^^^^^^^^^^^^
      case "primitive":
        yield 42;
      default:
        yield boxed; // Noncompliant
//            ^^^^^
    };
  }

  void unboxOnImplicitYield(String key) {
    Integer boxed = null;
    int primitive = switch (key) {
      case "nullable" -> getANullabbleInteger(); // Noncompliant
//                       ^^^^^^^^^^^^^^^^^^^^^^
      case "primitive" -> 42;
      default -> boxed; // Noncompliant
//               ^^^^^
    };
  }

  boolean shortComparison() {
    Short boxed = null;
    short primitive = 1;
    return primitive == boxed; // Noncompliant
//                      ^^^^^
  }

  void initializedWithNullableReturnValue() {
    int primitive = getANullabbleInteger(); // Noncompliant
//                  ^^^^^^^^^^^^^^^^^^^^^^
  }

  void declaredThenInitializedWithNullReturnValue() {
    int primitive;
    primitive = getANullabbleInteger(); // Noncompliant
//              ^^^^^^^^^^^^^^^^^^^^^^
  }

  void declareThenInitialize() {
    Integer boxed = null;
    int primitive;
    primitive = boxed; // Noncompliant
//              ^^^^^
  }

  void declareThenOverwrite() {
    Integer boxed = null;
    int primitive = 42;
    boxed = primitive; // Compliant because "boxed" will not unboxed
  }

  void declareThenPlusAssign() {
    Integer boxed = null;
    int primitive = 42;
    boxed += primitive; // Noncompliant
//  ^^^^^
  }

  void loopWithLiterals() {
    Integer i = null;

    while (i < 42) { // Noncompliant
//         ^
      i++; // Noncompliant
//    ^
    }
  }

  void loopWhileLessThanReturnValue() {
    int primitive = 42;
    while (primitive < getANullabbleInteger()) { // Noncompliant
//                     ^^^^^^^^^^^^^^^^^^^^^^
      primitive--;
    }
  }

  void crashOnComparison() {
    Integer i = null;
    int b = 42;
    do {
      b--;
    } while (0 < i); // Noncompliant
  }

  void crashOnDecrement() {
    Integer i = null;
    do {
      --i; // Noncompliant
//      ^
    } while (0 < i);
  }

  @CheckForNull
  static Integer getANullabbleInteger() {
    var random = new Random();
    if (random.nextBoolean()) {
      return Integer.valueOf(42);
    }
    return null;
  }
}
