package checks;

import javax.annotation.Nullable;

public class StringLiteralDuplicatedCheckSample {

  public void f() {
    System.out.println("aaaaa");
    System.out.println("bbbbb");
    System.out.println("bbbbb");
    System.out.println("ccccc"); // Noncompliant {{Define a constant instead of duplicating this literal "ccccc" 3 times.}}
    System.out.println("ccccc");
    System.out.println("ccccc");
    System.out.println("dddd");
    System.out.println("dddd");
  }

  interface F {
    default void foo() {
      System.out.println("aaaaa");
      System.out.println("aaaaa");
      System.out.println("aaaaa");

    }
  }
}
class AllConstants {
  private static final String Constant1 = "allConstant";
  private static final String Constant2 = "allConstant";
}
class ConstantAlreadyDefined {

  private static final String A = "constant";
  private static final String B = "constant";
  private static final String C = "constant";

  private static final String REPORT_WITHOUT_THRESHOLD = "blabla";

  void test() {
    System.out.println("constant"); // Noncompliant {{Use already-defined constant 'A' instead of duplicating its value here.}}
//                     ^^^^^^^^^^
    System.out.println("constant");
//                     ^^^^^^^^^^<
    System.out.println("blabla"); // Noncompliant {{Use already-defined constant 'REPORT_WITHOUT_THRESHOLD' instead of duplicating its value here.}}
  }

  public ConstantAlreadyDefined setProject(Proj project) {
    setFieldValue("projectName", project.longName());
    setFieldValue("projectKey", project.key());
    return this;
  }

  private void setFieldValue(String projectName, Object longName) {

  }

  public ConstantAlreadyDefined setProject(String projectKey, String projectName) {
    setFieldValue("projectName", projectName);
    setFieldValue("projectKey", projectKey);
    return this;
  }

  private class Proj {
    public Object longName() {
      return null;
    }

    public Object key() {
      return null;
    }
  }
}

class CompleteCoverage {
  private final String notConstant = "blablah"; // Noncompliant {{Define a constant instead of duplicating this literal "blablah" 3 times.}}
  private final String notConstant2 = "blablah";
  static String notConstant3 = "blablah";
  String notConstant4;
  int notString = 42;

  private static final String NOT_USED = "this constant is not used anywhere";
}

class IgnoreLiteralFragments {

  private final String sqlStatement1 =
    " SELECT " + // Compliant, because part of fragmented literal
    "   c.id, " + // Compliant, because part of fragmented literal
    "   c.name, " +
    " FROM customers c " + // Compliant, because part of fragmented literal
    " WHERE max_number IS NULL";

  private final String sqlStatement2 =
    " SELECT " +
      "   c.id, " +
      "   c.age, " +
      " FROM customers c ";

  private final String sqlStatement3 =
    " SELECT " +
      "   c.id, " +
      "   c.name, " +
      "   c.birthDate, " +
      " FROM customers c " +
      " WHERE max_number IS NULL";
}

class Coverage {

  @Nullable
  Object coverAnnotations = null;

  private final String prevLeftNull = "SELECT" + 3;
  private final String prevRightNull = 3 + "SELECT";
}

class DuplicatedExceptionArguments {
  private void areCompliantByDefault(int r) {
    if (r == 0) {
      throw new IllegalArgumentException("simple IAE");
    } else if (r == 1) {
      throw new IllegalArgumentException("simple IAE");
    } else if (r == 2) {
      throw new IllegalArgumentException("simple IAE");
    }
  }

  static class MyException extends Exception {
    public MyException(String message, Throwable throwable) {
      super(message, throwable);
    }
  }

  private void twoArgs(int r, Throwable throwable) throws MyException {
    if (r == 0) {
      throw new MyException("my exception message", throwable);
    } else if (r == 1) {
      throw new MyException("my exception message", throwable);
    } else if (r == 2) {
      throw new MyException("my exception message", throwable);
    }
  }

  private Throwable constructButDoNotThrow(int r) {
    if (r == 0) {
      return new NullPointerException("build message"); // Noncompliant {{Define a constant instead of duplicating this literal "build message" 3 times.}}
    } else if (r == 1) {
      return new IllegalArgumentException("build message");
    } else {
      return new AssertionError("build message");
    }
  }

  public static void constructAndThenThrow(int r) {
    if (r == 0) {
      var first = new IllegalArgumentException("this is a repeated message"); // Noncompliant
      throw first;
    } else if (r < 0) {
      var second = new IllegalArgumentException("this is a repeated message");
      throw second;
    } else {
      var third = new IllegalArgumentException("this is a repeated message");
      throw third;
    }
  }

  private void nestedString1(int r) {
    if (r == 0) {
      throw new IllegalArgumentException("nested string 1".toLowerCase());
    } else if (r == 1) {
      throw new IllegalArgumentException("nested string 1".toUpperCase());
    } else if (r == 2) {
      throw new IllegalArgumentException("nested string 1");
    }
  }

  private String transform(String s, String suffix) {
    return s + suffix;
  }

  // This case could be revisited if it does not complicate the implementation.
  private void areNonCompliantWhenPassedToMethod(int r) {
    if (r == 0) {
      throw new IllegalArgumentException(transform("nested string 2", "AAA")); // Noncompliant {{Define a constant instead of duplicating this literal "nested string 2" 3 times.}}
    } else if (r == 1) {
      throw new IllegalArgumentException(transform("nested string 2", "BBB"));
    } else if (r == 2) {
      throw new IllegalArgumentException(transform("nested string 2", "CCC"));
    }
  }

  private void concatenationIsAllowed(int r) {
    if (r == 1) {
      throw new IllegalArgumentException("message" + "concatenation" + r);
    } else if (r == 2) {
      throw new IllegalArgumentException("message" + "concatenation" + 2 * r);
    } else {
      throw new IllegalArgumentException("message" + "concatenation" + 3 * r);
    }
  }

  public static final String NOT_IMPLEMENTED_MESSAGE = "Will do it on Tuesday!";

  private int reportConstantsForArguments(int k) {
    throw new RuntimeException("Will do it on Tuesday!"); // Noncompliant {{Use already-defined constant 'NOT_IMPLEMENTED_MESSAGE' instead of duplicating its value here.}}
  }

  private void devNull(String s, int i) {
  }

  private void reportCorrectCount(int r) {
    if (r == 1) {
      throw new RuntimeException("message shared between exception and fun calls"); // Noncompliant {{Define a constant instead of duplicating this literal "message shared between exception and fun calls" 4 times.}}
    } else if (r == 2) {
      devNull("message shared between exception and fun calls", 2);
    } else if  (r == 3) {
      devNull("message shared between exception and fun calls", 3);
    } else {
      devNull("message shared between exception and fun calls", 4);
    }
  }
}
