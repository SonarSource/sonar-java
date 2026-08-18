package checks;

import java.util.Arrays;

class EqualsMismatchedMembersCheckSample {

  static class User {
    private String firstName;
    private String lastName;

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof User other) {
        return java.util.Objects.equals(this.firstName, other.firstName)
          && java.util.Objects.equals(this.lastName, other.firstName); // Noncompliant {{This equals() implementation compares mismatched members; pairing "lastName" with "firstName" breaks the equality contract.}}
      }
      return false;
    }
  }

  static class CompliantUser {
    private String firstName;
    private String lastName;

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof CompliantUser other) {
        return java.util.Objects.equals(this.firstName, other.firstName)
          && java.util.Objects.equals(this.lastName, other.lastName);
      }
      return false;
    }
  }

  static class Point {
    private int x;
    private int y;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Point that)) {
        return false;
      }
      return x == that.x && y == that.x; // Noncompliant {{This equals() implementation compares mismatched members; pairing "y" with "x" breaks the equality contract.}}
    }
  }

  static class CompliantPoint {
    private int x;
    private int y;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof CompliantPoint that)) {
        return false;
      }
      return x == that.x && y == that.y;
    }
  }

  static class NotEqual {
    private int x;
    private int y;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof NotEqual that)) {
        return false;
      }
      return x != that.y; // Noncompliant {{This equals() implementation compares mismatched members; pairing "x" with "y" breaks the equality contract.}}
    }
  }

  static class Box {
    private Object key;
    private Object value;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Box that)) {
        return false;
      }
      return key.equals(that.key) && value.equals(that.key); // Noncompliant {{This equals() implementation compares mismatched members; pairing "value" with "key" breaks the equality contract.}}
    }
  }

  static class CompliantBox {
    private Object key;
    private Object value;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof CompliantBox that)) {
        return false;
      }
      return key.equals(that.key) && value.equals(that.value);
    }
  }

  static class Person {
    private String firstName;
    private String lastName;

    String getFirstName() {
      return firstName;
    }

    String getLastName() {
      return lastName;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Person that)) {
        return false;
      }
      return getFirstName().equals(that.getFirstName())
        && getLastName().equals(that.getFirstName()); // Noncompliant {{This equals() implementation compares mismatched members; pairing "getLastName()" with "getFirstName()" breaks the equality contract.}}
    }
  }

  static class CompliantPerson {
    private String firstName;
    private String lastName;

    String getFirstName() {
      return firstName;
    }

    String getLastName() {
      return lastName;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof CompliantPerson that)) {
        return false;
      }
      return getFirstName().equals(that.getFirstName())
        && getLastName().equals(that.getLastName());
    }
  }

  static class PrimitiveGetters {
    private int a;
    private int b;

    int getA() {
      return a;
    }

    int getB() {
      return b;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof PrimitiveGetters that)) {
        return false;
      }
      return getA() == that.getB() && getB() == that.getB(); // Noncompliant {{This equals() implementation compares mismatched members; pairing "getA()" with "getB()" breaks the equality contract.}}
    }
  }

  record NamedPoint(int x, int y) {
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof NamedPoint that)) {
        return false;
      }
      return this.x() == that.y(); // Noncompliant {{This equals() implementation compares mismatched members; pairing "x()" with "y()" breaks the equality contract.}}
    }
  }

  static class ArraysHolder {
    private int[] left;
    private int[] right;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof ArraysHolder that)) {
        return false;
      }
      return Arrays.equals(this.left, that.right); // Noncompliant {{This equals() implementation compares mismatched members; pairing "left" with "right" breaks the equality contract.}}
    }
  }

  static class ArraysRangeHolder {
    private int[] left;
    private int[] right;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof ArraysRangeHolder that)) {
        return false;
      }
      return Arrays.equals(this.left, 0, 1, that.right, 0, 1); // Noncompliant {{This equals() implementation compares mismatched members; pairing "left" with "right" breaks the equality contract.}}
    }
  }

  static class GuavaHolder {
    private Object a;
    private Object b;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof GuavaHolder that)) {
        return false;
      }
      return com.google.common.base.Objects.equal(a, that.b) && com.google.common.base.Objects.equal(b, that.b); // Noncompliant {{This equals() implementation compares mismatched members; pairing "a" with "b" breaks the equality contract.}}
    }
  }

  static class MultipleMismatches {
    private int a;
    private int b;
    private int c;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof MultipleMismatches that)) {
        return false;
      }
      return a == that.b // Noncompliant {{This equals() implementation compares mismatched members; pairing "a" with "b" breaks the equality contract.}}
        && b == that.c; // Noncompliant {{This equals() implementation compares mismatched members; pairing "b" with "c" breaks the equality contract.}}
    }
  }

  static class UnorderedPair {
    private int a;
    private int b;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof UnorderedPair that)) {
        return false;
      }
      return (a == that.a && b == that.b) || (a == that.b && b == that.a);
    }
  }

  static class MixedFieldAndGetter {
    private String name;

    String getName() {
      return name;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof MixedFieldAndGetter that)) {
        return false;
      }
      return this.name.equals(that.getName());
    }
  }

  static class Parent {
    int inherited;
  }

  static class Child extends Parent {
    int own;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Child that)) {
        return false;
      }
      return this.inherited == that.own;
    }
  }

  static class Statics {
    static int DEFAULT;
    int value;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Statics that)) {
        return false;
      }
      return this.value == DEFAULT && this.value == that.value;
    }
  }

  static class SuperCall {
    int a;
    int b;

    @Override
    public boolean equals(Object obj) {
      return super.equals(obj);
    }
  }

  static class NotEqualsMethod {
    int a;
    int b;

    public boolean equals(NotEqualsMethod other) {
      return a == other.b;
    }

    public int compareTo(Object other) {
      return a == ((NotEqualsMethod) other).b ? 0 : 1;
    }
  }

  static class Locals {
    int a;
    int b;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Locals that)) {
        return false;
      }
      int temp = that.a;
      return this.a == temp && this.b == that.b;
    }
  }

  static class Parentheses {
    int a;
    int b;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Parentheses that)) {
        return false;
      }
      return (a) == (that.b); // Noncompliant {{This equals() implementation compares mismatched members; pairing "a" with "b" breaks the equality contract.}}
    }
  }

  static class SameObjectFields {
    int start;
    int end;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof SameObjectFields that)) {
        return false;
      }
      if (this.start == this.end && that.start == that.end) {
        return true;
      }
      return this.start == that.start && this.end == that.end;
    }
  }

  static class OtherFirst {
    int a;
    int b;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof OtherFirst that)) {
        return false;
      }
      return that.a == this.b; // Noncompliant {{This equals() implementation compares mismatched members; pairing "b" with "a" breaks the equality contract.}}
    }
  }

  static class Holder<T> {
    private T left;
    private T right;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Holder<?> that)) {
        return false;
      }
      return java.util.Objects.equals(this.left, that.right); // Noncompliant {{This equals() implementation compares mismatched members; pairing "left" with "right" breaks the equality contract.}}
    }
  }

  static class GenericSameMember<T> {
    private T left;
    private T right;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof GenericSameMember<?> that)) {
        return false;
      }
      return java.util.Objects.equals(this.left, that.left)
        && java.util.Objects.equals(this.right, that.right);
    }
  }

  static class GenericUnordered<T> {
    private T a;
    private T b;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof GenericUnordered<?> that)) {
        return false;
      }
      return (java.util.Objects.equals(a, that.a) && java.util.Objects.equals(b, that.b))
        || (java.util.Objects.equals(a, that.b) && java.util.Objects.equals(b, that.a));
    }
  }

  static class StaticSingleton {
    static final StaticSingleton EMPTY = new StaticSingleton();
    int a;
    int b;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof StaticSingleton that)) {
        return false;
      }
      return this.a == EMPTY.b && this.a == that.a && this.b == that.b;
    }
  }

  static class DistinctStatements {
    int a;
    int b;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof DistinctStatements that)) {
        return false;
      }
      if (a == that.b) { // Noncompliant {{This equals() implementation compares mismatched members; pairing "a" with "b" breaks the equality contract.}}
        return true;
      }
      return b == that.a; // Noncompliant {{This equals() implementation compares mismatched members; pairing "b" with "a" breaks the equality contract.}}
    }
  }

  static class NestedTypeInEquals {
    int a;
    int b;

    @Override
    public boolean equals(Object obj) {
      class Local {
      }
      if (!(obj instanceof NestedTypeInEquals that)) {
        return false;
      }
      return a == that.a && b == that.b;
    }
  }

  static class NonMemberOperand {
    int a;
    int b;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof NonMemberOperand that)) {
        return false;
      }
      return a == (that.b + 0) && a == that.a;
    }
  }

  static class UnqualifiedEquals {
    Object a;
    Object b;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof UnqualifiedEquals that)) {
        return false;
      }
      return equals(that.a) && a.equals(that.a);
    }
  }

  static class NestedReceiver {
    int a;
    NestedReceiver child;

    NestedReceiver child() {
      return child;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof NestedReceiver that)) {
        return false;
      }
      return this.a == that.child().a && this.a == that.a;
    }
  }

  static class CustomEqualHelper {
    Object a;
    Object b;

    static boolean equal(Object x, Object y) {
      return x == y;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof CustomEqualHelper that)) {
        return false;
      }
      return equal(a, that.b); // Noncompliant {{This equals() implementation compares mismatched members; pairing "a" with "b" breaks the equality contract.}}
    }
  }

  static class NonBooleanEqualHelper {
    int a;
    int b;

    static void equal(Object x, Object y) {
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof NonBooleanEqualHelper that)) {
        return false;
      }
      equal(a, that.b);
      return a == that.a;
    }
  }

  static class StaticGetter {
    int a;
    int b;

    static int getA() {
      return 0;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof StaticGetter that)) {
        return false;
      }
      return getA() == that.b && a == that.a;
    }
  }

  static class FieldReceiver {
    int a;
    int b;
    FieldReceiver other;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof FieldReceiver that)) {
        return false;
      }
      return this.a == other.b && this.a == that.a;
    }
  }

  static class GetterWithArgument {
    int a;
    int b;

    int value(int ignored) {
      return a;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof GetterWithArgument that)) {
        return false;
      }
      return value(0) == that.b && a == that.a;
    }
  }
}
