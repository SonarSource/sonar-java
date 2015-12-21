class AException {} // Noncompliant [[sc=7;ec=17]] {{Rename this class to remove "Exception" or correct its inheritance.}}
class BException extends NullPointerException {}
class ExceptionHandler {}
