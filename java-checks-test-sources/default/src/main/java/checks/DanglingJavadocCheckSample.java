package checks;

class DanglingJavadocCheckSample {
  private String name;

  /**
   * This is a dangling javadoc comment that will be ignored.
   */
  /**
   * This is the actual javadoc that will be used.
   * @param name the name parameter
   */
  public void setName(String name) { // Noncompliant {{Remove or merge the dangling Javadoc comment(s).}}
//            ^^^^^^^
    this.name = name;
  }

  /**
   * Sets the name parameter.
   * @param name the name parameter
   */
  public void setNameCompliant(String name) { // Compliant
    this.name = name;
  }

  /**
   * First dangling comment
   */
  /**
   * Second dangling comment
   */
  /**
   * The actual comment for this field
   */
  public String multipleJavadocs; // Noncompliant
//              ^^^^^^^^^^^^^^^^
  /**
   * Dangling method comment
   */
  /**
   * Actual method comment
   * @return the value
   */
  public int getValue() { // Noncompliant
    return 42;
  }
}

/**
 * Old documentation that is no longer relevant.
 */
/**
 * Updated documentation for this class.
 */
class OldUser { // Noncompliant
  private String name;
}

/**
 * This traditional javadoc will be ignored.
 */
/// This markdown javadoc will be used.
/// Represents a customer entity.
class Customer { // Noncompliant
//    ^^^^^^^^
  private String customerId;
}

/**
 * Updated documentation for this class.
 */
class UserCompliant { // Compliant
  private String name;
}

/// Represents a customer entity.
class CustomerCompliant { // Compliant
  private String customerId;
}

class NoJavadoc { // Compliant
  private String field;
}
