package checks;

class ErrorClassExtendedCheckSampleA extends Error { // Noncompliant [[sc=46;ec=51]] {{Extend "java.lang.Exception" or one of its subclasses.}}
}

class ErrorClassExtendedCheckSampleB extends java.lang.Error { // Noncompliant [[sc=46;ec=61]]
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
