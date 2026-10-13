package checks;

class ConnectionFailure extends Throwable { // Noncompliant {{Extend "Exception" or "RuntimeException" instead.}}
//                              ^^^^^^^^^
  ConnectionFailure(String message) {
    super(message);
  }
}

class ServiceFailure extends java.lang.Throwable { // Noncompliant
//                           ^^^^^^^^^^^^^^^^^^^
}

abstract class AbstractFailure extends Throwable { // Noncompliant
//                                     ^^^^^^^^^
}

class RequestHandler {
  class NestedFailure extends Throwable { // Noncompliant
//                            ^^^^^^^^^
  }
}

class InvalidRequestException extends Exception { // Compliant
}

class RetryableFailure extends RuntimeException { // Compliant
}

class DomainException extends Exception { // Compliant
}

class CustomerNotFoundException extends DomainException { // Compliant
}

class SystemFailure extends Error { // Compliant
}

class MemoryFailure extends OutOfMemoryError { // Compliant
}

class RegularService { // Compliant
}

class LocalTypes {
  class Throwable { // Compliant
  }

  class LocalThrowableFailure extends Throwable { // Compliant
  }
}
