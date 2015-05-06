public class TestClassInstanceof1 {
  @Override
  public boolean equals(Object that) { // Compliant
    if (that instanceof Object) {
      return ((Object) that) == null;
    }
    return false;
  }
}

public class TestClassInstanceof2 {
  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    if (this instanceof Object) {
      return ((Object) that) == null;
    }
    return false;
  }
}

public class TestClassInstanceof3 {
  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    if (0 == 0 || ((that.getClass()) instanceof Object)) {
      return ((Object) that) == null;
    }
    return false;
  }
}

public class TestClassGetClassEqual {
  @Override
  public boolean equals(Object that) { // Compliant
    if ((that.getClass()) == (Object.class)) {
      return ((Object) that) == null;
    }
    return false;
  }
}

public class TestClassGetClassNotEqual {
  @Override
  public boolean equals(Object that) { // Compliant
    if (that.getClass() != Object.class) {
      return ((Object) that) == null;
    }
    return true;
  }
}

public class TestClassGetClassEqual2 {
  @Override
  public boolean equals(Object that) { // Compliant
    if ((Object.class) == (that.getClass())) {
      return ((Object) that) == null;
    }
    return false;
  }
}

public class TestClassGetClassEqual3 {
  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    if (0 == 0) {
      return ((Object) that) == null;
    }
    return false;
  }
}

public class TestClassGetClassEqual4 {
  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    if (getClass() == Object.class) {
      return ((Object) that) == null;
    }
    return false;
  }
}

public class TestClassGetClassEqual5 {
  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    if ((that.hashCode()) == 0) {
      return ((Object) that) == null;
    }
    return false;
  }
}

public class TestClassAssign {
  @Override
  public boolean equals(Object that) { // Compliant
    boolean result;
    result = that.getClass() == Object.class && true;
    return ((Object) that) == null;
  }
}

public class TestClassExplicitComparison1 {
  @Override
  public boolean equals(Object that) { // Compliant
    if ((this) == that) {
      return ((Object) that) == null;
    }
  }
}

public class TestClassExplicitComparison2 {
  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    if ((that) == that) {
      return ((Object) that) == null;
    }
  }
}

public class TestClassExplicitComparison3 {
  @Override
  public boolean equals(Object that) { // Compliant
    if (that == this) {
      return ((Object) that) == null;
    }
  }
}

public class TestClassExplicitComparison5 {
  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    if (that == "") {
      return ((Object) that) == null;
    }
  }
}

public class TestClassEqualsCall1 {
  @Override
  public boolean equals(Object that) { // Compliant
    if (this.equals(that)) {
      return ((Object) that) == null;
    }
  }
}

public class TestClassEqualsCall2 {
  @Override
  public boolean equals(Object that) { // Compliant
    if (that.equals(this)) {
      return ((Object) that) == null;
    }
  }
}

public class TestClassEqualsCall3 {
  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    if (this.equals(this)) {
      return ((Object) that) == null;
    }
  }
}

public class TestClassEqualsCall4 {
  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    if (equals(this)) {
      return ((Object) that) == null;
    }
  }
}

public class TestMethodCall {
  public boolean method() {
    return true;
  }

  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    if (method()) {
      return ((Object) that) == null;
    }
  }
}

public interface testInterface {

  public boolean equals(Object that); // Compliant

}

public class TestClass {

  public boolean method(Object that) { // Compliant, different name
    return ((Object) that) == null;
  }

  public Object equals() { // Compliant, different signature
    return ((Object) that);
  }

  public boolean equals(String that) { // Compliant, different signature
    return ((Object) that) == null;
  }

  @Override
  public boolean equals(Object that) { // Compliant
    if (true) {
      return (Object) this;
    }
    if (true) {
      return (Object) equals();
    }
    return false;
  }

}
