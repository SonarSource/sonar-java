package checks;

class ErrorClassExtendedCheckSampleA extends Error { // Noncompliant {{Extend "java.lang.Exception" or one of its subclasses.}}
//                                           ^^^^^
}

class ErrorClassExtendedCheckSampleB extends java.lang.Error { // Noncompliant
//                                           ^^^^^^^^^^^^^^^
}

class ErrorClassExtendedCheckSampleC { // Compliant
}

class ErrorClassExtendedCheckSampleD extends Exception { // Compliant
}

class ErrorClassExtendedCheckSampleF extends java.lang.Exception { // Compliant
}

class ErrorClassExtendedCheckSampleG extends java.lang.OutOfMemoryError { // Noncompliant
}

class ErrorClassExtendedCheckSampleH extends OutOfMemoryError { // Noncompliant
}

class ErrorClassExtendedCheckSampleI extends StackOverflowError { // Noncompliant
}

class ErrorClassExtendedCheckSampleJ extends IllegalStateException { // Compliant
}

class ErrorClassExtendedCheckSampleK extends java.lang.IllegalStateException { // Compliant
}
