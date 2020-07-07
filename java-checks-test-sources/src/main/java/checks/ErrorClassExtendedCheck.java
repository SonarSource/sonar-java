package checks;

class ErrorClassExtendedCheckA extends Error { // Noncompliant [[sc=40;ec=45]] {{Extend "java.lang.Exception" or one of its subclasses.}}
}

class ErrorClassExtendedCheckB extends java.lang.Error { // Noncompliant [[sc=40;ec=55]]
}

class ErrorClassExtendedCheckC { // Compliant
}

class ErrorClassExtendedCheckD extends Exception { // Compliant
}

class ErrorClassExtendedCheckF extends java.lang.Exception { // Compliant
}

class ErrorClassExtendedCheckG extends java.lang.OutOfMemoryError { // Noncompliant
}

class ErrorClassExtendedCheckH extends OutOfMemoryError { // Noncompliant
}

class ErrorClassExtendedCheckI extends StackOverflowError { // Noncompliant
}

class ErrorClassExtendedCheckJ extends IllegalStateException { // Compliant
}

class ErrorClassExtendedCheckK extends java.lang.IllegalStateException { // Compliant
}
