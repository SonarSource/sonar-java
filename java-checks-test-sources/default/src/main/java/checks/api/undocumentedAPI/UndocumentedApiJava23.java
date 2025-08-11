package com.example.api.mypackage;

/// Undocumented Api for Java 23 (and an example of bad documentation).
public class UndocumentedApiJava23 {

  public class BadNested {} // Noncompliant {{Document this public class by adding an explicit description.}}

  ///  Documented.
  public class GoodNested {} // Compliant

  public String badField; // Noncompliant {{Document this public field by adding an explicit description.}}

  /// Documented.
  public String goodField; // Compliant

  public void bad(int value) { // Noncompliant {{Document this public method by adding an explicit description.}}
  }

  /// Valid descriptions.
  /// @param value Valid descriptions.
  public void goodMethod1(int value) { // Compliant
  }

  /// Valid descriptions.
  /// @param value Valid descriptions.
  /// @return some text
  /// @throws NumberFormatException sometimes
  public String goodMethod2(int value) throws NumberFormatException { // Compliant
    return "";
  }

  /// Valid descriptions.
  /// @return some text
  /// @throws NumberFormatException sometimes
  public String missingParameted(int value) throws NumberFormatException { // Noncompliant {{Document the parameter(s): value}}
    return "";
  }

  /// Valid descriptions.
  /// @param value Valid descriptions.
  /// @throws NumberFormatException sometimes
  public String missingReturn(int value) throws NumberFormatException { // Noncompliant {{Document this method return value.}}
    return "";
  }

  /// Valid descriptions.
  /// @param value Valid descriptions.
  /// @return some text
  public String missingThrows(int value) throws NumberFormatException { // Noncompliant {{Document this method thrown exception(s): NumberFormatException}}
    return "";
  }

  /// Documented, but not the type.
  public class SomethingGenericBad<T> { // Noncompliant {{Document the parameter(s): <T>}}
  }

  /// Documented.
  /// @param <T> also documented
  public class SomethingGenericGood<T> { // Compliant
  }
}
