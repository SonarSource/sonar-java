public class TestClassInstanceof1 {

  @Override
  public boolean equals(Object that) { // Compliant
    if (that instanceof Object) {
    }
    return false;
  }

}

public class TestClassInstanceof2 {

  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    if (this instanceof Object) {
    }
    return false;
  }

}

public class TestClassInstanceof3 {

  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    if (0 == 0 || ((that.getClass()) instanceof Object)) {
    }
    return false;
  }

}

public class TestClassGetClassEqual {

  @Override
  public boolean equals(Object that) { // Compliant
    if ((that.getClass()) == (Object.class)) {
    }
    return false;
  }

}

public class TestClassGetClassNotEqual {

  @Override
  public boolean equals(Object that) { // Compliant
    if (that.getClass() != Object.class) {
      return false;
    }
    return true;
  }

}

public class TestClassGetClassEqual2 {

  @Override
  public boolean equals(Object that) { // Compliant
    if ((Object.class) == (that.getClass())) {
    }
    return false;
  }

}

public class TestClassGetClassEqual3 {

  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    if (0 == 0) {
    }
    return false;
  }

}

public class TestClassGetClassEqual4 {

  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    if (getClass() == Object.class) {
    }
    return false;
  }

}

public class TestClassGetClassEqual5 {

  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    if ((that.hashCode()) == 0) {
    }
    return false;
  }

}

public class TestClassAssign {

  @Override
  public boolean equals(Object that) { // Compliant
    boolean result;
    result = that.getClass() == Object.class && true;
    return result;
  }

}

public class TestClassExplicitComparison1 {

  @Override
  public boolean equals(Object that) { // Compliant
    return (this) == that;
  }

}

public class TestClassExplicitComparison2 {

  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    return (that) == that;
  }

}

public class TestClassExplicitComparison3 {

  @Override
  public boolean equals(Object that) { // Compliant
    return that == this;
  }

}

public class TestClassExplicitComparison4 {

  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    return that == that;
  }

}

public class TestClassExplicitComparison5 {

  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    return that == "";
  }

}

public class TestClassEqualsCall {

  @Override
  public boolean equals(Object that) { // Compliant
    return this.equals(that);
  }

}

public class TestMethodCall {

  public boolean method() {
    return true;
  }

  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    return method();
  }

}

public class TestClassInitializer {

  @Override
  public boolean equals(Object that) { // Compliant
    boolean result = that.getClass() == Object.class && true;
    return result;
  }

}

public class TestClassNestedIf {

  @Override
  public boolean equals(Object that) { // Compliant
    if (true) {
      return that.getClass() == Object.class;
    }
  }

}

public class TestClassReturn {

  @Override
  public boolean equals(Object that) { // Compliant
    return that.getClass() == Object.class && true;
  }

}

public interface testInterface {

  public boolean equals(Object that); // Compliant

}

public class TestClass {

  public boolean method(Object that) { // Compliant, different name
  }

  public boolean equals() { // Compliant, different signature
  }

  public boolean equals(String that) { // Compliant, different signature
  }

  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    return false;
  }

}
