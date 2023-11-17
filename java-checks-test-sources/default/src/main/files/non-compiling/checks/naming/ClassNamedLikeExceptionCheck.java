package checks.naming;

import something.Exception;

class ClassNamedLikeExceptionCheck {
  class CException extends UnknownException {} // Compliant
  class DException extends CException {} // Compliant

  class MyException extends Exception {} // Compliant
}
