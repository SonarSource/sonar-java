package checks.naming;

class ClassNamedLikeExceptionCheck {

  class AException {} // Noncompliant {{Rename this class to remove "Exception" or correct its inheritance.}}
//      ^^^^^^^^^^
  class BException extends NullPointerException {}
  class EException extends Exception {} // Compliant
  class FException extends AException {} // Noncompliant
//      ^^^^^^^^^^
  class ExceptionHandler {}
}
