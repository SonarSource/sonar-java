import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

class A {
  static final String CONSTANT = "";
  static final String[] ARRAY_CONSTANT = new String[] {""}; // Although mutable, as a static final field we consider it as a constant for testing purposes
  static final Iterable<Double> ITERABLE_CONSTANT = new ArrayList<>(); // Although mutable, as a static final field we consider it as a constant for testing purposes
  static final List<Integer> LIST_CONSTANT = new ArrayList<>(); // Although mutable, as a static final field we consider it as a constant for testing purposes

  static String NOT_A_CONSTANT_1 = "";
  final String NOT_A_CONSTANT_2 = "";

  void fun() {
    assertEquals(0, new A().actual());
    assertEquals(new A().actual(), 0); // Noncompliant [[sc=36;ec=37;secondary=23]] {{Swap these 2 arguments so they are in the correct order: expected value, actual value.}}
    assertEquals(new A().actual(), 0, "message"); // Noncompliant {{Swap these 2 arguments so they are in the correct order: expected value, actual value.}}
    assertEquals(new A().actual(), 0, () -> "messageSupplier"); // Noncompliant
    assertEquals("constantString", actualObject(), "message");
    assertEquals(actualObject(), "constantString", "message"); // Noncompliant
    assertEquals(0d, actualDouble(), 1d);
    assertEquals(0d, actualDouble(), 1d, () -> "messageSupplier");
    assertEquals(actualDouble(), 0.0d, 1d); // Noncompliant
    assertEquals(actualDouble(), 0.0d, 1d, "message"); // Noncompliant
    assertEquals(A.CONSTANT, actualObject());
    assertEquals(actualObject(), CONSTANT); // Noncompliant
    assertEquals(actualObject(), A.CONSTANT); // Noncompliant
    assertEquals(actualObject(), NOT_A_CONSTANT_1);
    assertEquals(actualObject(), NOT_A_CONSTANT_2);
    assertEquals(actualObject(), new A().NOT_A_CONSTANT_1);

    assertArrayEquals(ARRAY_CONSTANT, actualStringArray());
    assertArrayEquals(new String[] {""}, actualStringArray());
    assertArrayEquals(new String[] {"", CONSTANT}, actualStringArray());
    assertArrayEquals(new String[] {""}, new String[] {actualString()});
    assertArrayEquals(new String[0], actualStringArray());
    assertArrayEquals(new String[] {""}, actualStringArray(), "message");
    assertArrayEquals(actualStringArray(), ARRAY_CONSTANT); // Noncompliant
    assertArrayEquals(actualStringArray(), new String[] {""}); // Noncompliant
    assertArrayEquals(actualStringArray(), new String[] {"", CONSTANT}); // Noncompliant
    assertArrayEquals(actualStringArray(), new String[0]); // Noncompliant
    assertArrayEquals(actualStringArray(), new String[] {""}, "message"); // Noncompliant

    assertIterableEquals(ITERABLE_CONSTANT, actualDoubleIterable());
    assertIterableEquals(singletonList(1.0), actualDoubleIterable());
    assertIterableEquals(singletonList(1.0), actualDoubleIterable(), () -> "messageSupplier");
    assertIterableEquals(singletonList(actualDouble()), actualDoubleIterable());
    assertIterableEquals(ITERABLE_CONSTANT, singletonList(actualDouble()));
    assertIterableEquals(actualDoubleIterable(), singletonList(actualDouble()));
    assertIterableEquals(actualDoubleIterable(), ITERABLE_CONSTANT); // Noncompliant
    assertIterableEquals(actualDoubleIterable(), singletonList(1.0)); // Noncompliant
    assertIterableEquals(actualDoubleIterable(), singleton(1.0)); // Noncompliant
    assertIterableEquals(actualDoubleIterable(), emptyList()); // Noncompliant
    assertIterableEquals(actualDoubleIterable(), EMPTY_LIST); // Noncompliant
    assertIterableEquals(actualDoubleIterable(), emptySet()); // Noncompliant
    assertIterableEquals(actualDoubleIterable(), Arrays.asList(1.0)); // Noncompliant
    assertIterableEquals(actualDoubleIterable(), singletonList(1.0), () -> "messageSupplier"); // Noncompliant

    assertLinesMatch(singletonList(""), actualLines());
    assertLinesMatch(actualLines(), singletonList("")); // Noncompliant

    assertNotEquals(CONSTANT, actualObject());
    assertNotEquals(actualObject(), CONSTANT); // Noncompliant

    assertSame(0, new A().actual());
    assertSame(0, new A().actual(), "message");
    assertSame(new A().actual(), 0); // Noncompliant
    assertSame(new A().actual(), 0, "message"); // Noncompliant

    assertNotSame(0, new A().actual());
    assertNotSame(0, new A().actual(), () -> "messageSupplier");
    assertNotSame(new A().actual(), 0); // Noncompliant
    assertNotSame(new A().actual(), 0, () -> "messageSupplier"); // Noncompliant
  }

  int actual() {
    return 0;
  }

  double actualDouble() {
    return 0;
  }

  String[] actualStringArray() {
    return new String[0];
  }

  String actualString() {
    return "";
  }

  Iterable<Double> actualDoubleIterable() {
    return null;
  }

  List<String> actualLines() {
    return null;
  }

  int actualObject() {
    return 0;
  }

}
