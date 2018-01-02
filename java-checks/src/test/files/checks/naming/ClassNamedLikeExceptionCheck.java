class AException {} // Noncompliant [[sc=7;ec=17]] {{Rename this class to remove "Exception" or correct its inheritance.}}
class BException extends NullPointerException {}
class CException extends UnknownException {} // Compliant
class DException extends CException {} // Compliant
class EException extends Exception {} // Compliant
class FException extends AException {} // Noncompliant [[sc=7;ec=17]]
class ExceptionHandler {}
