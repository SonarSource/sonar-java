package checks.naming;

class ClassNamedLikeExceptionCheck {

  class AException {} // Noncompliant [[sc=9;ec=19]] {{Rename this class to remove "Exception" or correct its inheritance.}}
  class BException extends NullPointerException {}
  class EException extends Exception {} // Compliant
  class FException extends AException {} // Noncompliant [[sc=9;ec=19]]
  class ExceptionHandler {}
}
