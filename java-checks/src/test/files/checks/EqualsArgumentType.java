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

public class TestClassConditional1 {
  @Override
  public boolean equals(Object that) { // Noncompliant
    return condition ? (Object) that == null : false;
  }
}

public class TestClassConditional2 {
  @Override
  public boolean equals(Object that) { // Compliant
    return that instanceof Object ? (Object) that == null : false;
  }
}

public class TestClassLogical1 {
  @Override
  public boolean equals(Object that) { // Noncompliant
    return condition && (Object) that == null;
  }
}

public class TestClassLogical2 {
  @Override
  public boolean equals(Object that) { // Compliant
    return that instanceof Object && (Object) that == null;
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

public class TestClassGetClassEqual1 {
  @Override
  public boolean equals(Object that) { // Compliant
    if ((Object.class) == (that.getClass())) {
      return ((Object) that) == null;
    }
    return false;
  }
}

public class TestClassGetClassEqual2 {
  @Override
  public boolean equals(Object that) { // Compliant
    // call to any method in this or super can contain a typecast
    if ((Object.class) == (this.getClass())) {
      return ((Object) that) == null;
    }
    return false;
  }
}

public class TestClassGetClassEqual3 {
  @Override
  public boolean equals(Object that) { // Compliant
    // call to any method in this or super can contain a typecast
    if ((Object.class) == (getClass())) {
      return ((Object) that) == null;
    }
    return false;
  }
}

public class TestClassGetClassEqual4 {
  @Override
  public boolean equals(Object that) { // Noncompliant {{Add a type test to this method.}}
    if (0 == 0) {
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

public class TestClassEqualsCall3 {
  @Override
  public boolean equals(Object that) { // Noncompliant
    if (that.equals(this)) {
      return ((Object) that) == null;
    }
  }
}

public class TestMethodCall1 {
  public boolean method(Object that) {
    return that instanceof TestMethodCall;
  }

  @Override
  public boolean equals(Object that) { // Noncompliant
    if (method(that)) {
    }
    return ((Object) that) == null; // not guarded by an if
  }
}

public class TestMethodCall2 {
  public boolean method(Object that) {
    return that instanceof TestMethodCall;
  }

  @Override
  public boolean equals(Object that) { // Compliant, implicit call of a method in this
    if (method(that)) {
      return ((Object) that) == null;
    }
  }
}

public class TestMethodCall3 {
  public boolean method(Object that) {
    return that instanceof TestMethodCall;
  }

  @Override
  public boolean equals(Object that) { // Compliant, explicit call of a method in super
    if (super.method(that)) {
      return ((Object) that) == null;
    }
  }
}

public class TestMethodCall4 {
  public boolean method(Object that) {
    return that instanceof TestMethodCall;
  }

  @Override
  public boolean equals(Object that) { // Compliant, explicit call of a method in this
    if (this.method(that)) {
      return ((Object) that) == null;
    }
  }
}

public class TestMethodCall5 {
  Object[] objects;

  public boolean method(Object that) {
    return that instanceof TestMethodCall;
  }

  @Override
  public boolean equals(Object that) { // Noncompliant
    if (objects[0].hashCode()) {
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
