public static class Class {

  private static class Class {
    Object field;

    Object method() {
      return null;
    }
  }

  private boolean field;

  public void assign(boolean parameter) {
    parameter = false;
    if (parameter) { // Compliant, false negative
      if (parameter) { // Compliant, false negative
      }
    }
    if (!parameter) { // Compliant, false negative
      if (!parameter) { // Compliant, false negative
      }
    }
  }

  public void conditional_and(boolean parameter1, boolean parameter2) {
    if (false && false) { // Noncompliant, always false
    }
    if (false && true) { // Noncompliant, always false
    }
    if (false && parameter2) { // Noncompliant, always false
    }
    if (true && false) { // Noncompliant, always false
    }
    if (true && true) { // Noncompliant, always true
    }
    if (true && parameter2) { // Compliant, unknown
    }
    if (parameter1 && false) { // Noncompliant, always false
    }
    if (parameter1 && true) { // Compliant, unknown
    }
    if (parameter1 && parameter2) { // Compliant, unknown
    }
  }

  public void conditional_or(boolean parameter1, boolean parameter2) {
    if (false || false) { // Noncompliant, always false
    }
    if (false || true) { // Noncompliant, always true
    }
    if (false || parameter2) { // Compliant, unknown
    }
    if (true || false) { // Noncompliant, always true
    }
    if (true || true) { // Noncompliant, always true
    }
    if (true || parameter2) { // Noncompliant, always true
    }
    if (parameter1 || false) { // Compliant, unknown
    }
    if (parameter1 || true) { // Noncompliant, always true
    }
    if (parameter1 || parameter2) { // Compliant, unknown
    }
  }

  public void identifier_field() {
    if (field == false && field == true) { // Compliant
    }
    if (field == false || field == true) { // Compliant
    }
  }

  public void identifier_local() {
    // local variables
    boolean localFalse = false;
    if (localFalse && !localFalse) { // Noncompliant, condition is always false
    }
    boolean localTrue = true;
    if (localTrue || !localTrue) { // Noncompliant, condition is always true
    }
    boolean localUnknown;
    if (localUnknown) { // Compliant
    }
  }

  public void identifier_parameter(boolean parameter) {
    if (parameter) { // Compliant
    }
    if (parameter && !parameter) { // Noncompliant, condition is always false
    }
    if (parameter || !parameter) { // Noncompliant, condition is always true
    }
  }

  public void instanceOf() {
    Object object = new Object();
    if (object instanceof Object) { // Compliant, false negative
    }
    if (object instanceof String) { // Compliant
    }
    object = "string";
    if (object instanceof String) { // Compliant, false negative
    }
  }

  public void literals() {
    // literals
    if (false) { // Noncompliant, always false
    }
    if (true) { // Noncompliant, always true
    }
  }

  public void member_select() {
    // member select
    Class instance = new Class();
    if (instance.field != null && instance.field == null) { // Compliant
    }
  }

  public void method_invocation() {
    Class instance = new Class();
    if (instance.method() != null && instance.method() == null) { // Compliant
    }
  }

  public void unary_logical_complement() {
    // unary logical complement
    if (!false) { // Noncompliant, always true
    }
    if (!true) { // Noncompliant, always false
    }
  }

  public void relational_equal(boolean parameter1, boolean parameter2, boolean condition) {
    if (parameter1 == parameter2) {
      if (parameter1 == parameter2) { // False negative, always true
      }
      if (parameter1 >= parameter2) { // False negative, always true
      }
      if (parameter1 == parameter2 && parameter1 > parameter2) { // Noncompliant, always false
      }
      if (parameter1 <= parameter2) { // False negative, always true
      }
      if (parameter1 == parameter2 && parameter1 < parameter2) { // Noncompliant, always false
      }
    }
    if (parameter1 == parameter2) { // Compliant
    }
  }

  public void relational_ge(boolean parameter1, boolean parameter2) {
    if (parameter1 >= parameter2) {
      if (parameter1 >= parameter2 && parameter1 == parameter2) { // Compliant
      }
      if (parameter1 >= parameter2) { // False negative, always true
      }
      if (parameter1 > parameter2) { // Compliant
      }
      if (parameter1 <= parameter2) { // Compliant
      }
      if (parameter1 >= parameter2 && parameter1 < parameter2) { // Noncompliant, always false
      }
    }
    if (parameter1 >= parameter2) { // Compliant
    }
  }

  public void relational_g(boolean parameter1, boolean parameter2) {
    if (parameter1 > parameter2) {
      if (parameter1 > parameter2 && parameter1 == parameter2) { // Noncompliant, always false
      }
      if (parameter1 >= parameter2) { // False negative, always true
      }
      if (parameter1 > parameter2) { // False negative, always true
      }
      if (parameter1 > parameter2 && parameter1 <= parameter2) { // Noncompliant, always false
      }
      if (parameter1 > parameter2 && parameter1 < parameter2) { // Noncompliant, always false
      }
    }
    if (parameter1 > parameter2) { // Compliant
    }
  }

  public void relationa_le(boolean parameter1, boolean parameter2) {
    if (parameter1 <= parameter2) {
      if (parameter1 <= parameter2 && parameter1 == parameter2) { // Compliant
      }
      if (parameter1 <= parameter2 && parameter1 >= parameter2) { // Compliant
      }
      if (parameter1 <= parameter2 && parameter1 > parameter2) { // Noncompliant, always false
      }
      if (parameter1 <= parameter2) { // False negative, always true
      }
      if (parameter1 < parameter2) { // Compliant
      }
    }
    if (parameter1 <= parameter2) { // Compliant
    }
  }

  public void relational_l(boolean parameter1, boolean parameter2) {
    if (parameter1 < parameter2) {
      if (parameter1 < parameter2 && parameter1 == parameter2) { // Noncompliant, always false
      }
      if (parameter1 < parameter2 && parameter1 >= parameter2) { // Noncompliant, always false
      }
      if (parameter1 < parameter2 && parameter1 > parameter2) { // Noncompliant, always false
      }
      if (parameter1 <= parameter2) { // False negative, always true
      }
      if (parameter1 < parameter2 && parameter1 < parameter2) { // False negative, always true
      }
    }
    if (parameter1 < parameter2) { // Compliant
    }
  }

  public void tests(boolean parameter1, boolean parameter2, boolean condition) {
    if (parameter1 == parameter2) { // Compliant
    }
    if (parameter1 == parameter2 && parameter1 == parameter2) { // Compliant
    }
    if (parameter1 == parameter2 || parameter1 == parameter2) { // Compliant
    }
    if (parameter1 == parameter2 && parameter1 != parameter2) { // Noncompliant, always false
    }
    if (parameter1 == parameter2 && parameter1 > parameter2) { // Noncompliant, always false
    }
    if (parameter1 == parameter2 && parameter1 < parameter2) { // Noncompliant, always false
    }
    if (parameter1 == parameter2 || parameter1 != parameter2) { // Noncompliant, always true
    }
    if (condition && !condition) { // Noncompliant, always false
    }
    if (condition || !condition) { // Noncompliant, always true
    }
    if ((parameter1 == parameter2 || condition) && !(parameter1 == parameter2 || condition)) { // Noncompliant, always false
    }
    if ((parameter1 == parameter2 || condition) || !(parameter1 == parameter2 || condition)) { // Noncompliant, always true
    }
    if (!(parameter1 == parameter2 || condition) && (parameter1 == parameter2 || condition)) { // Noncompliant, always false
    }
    if (!(parameter1 == parameter2 || condition) || (parameter1 == parameter2 || condition)) { // Noncompliant, always true
    }
  }
}
