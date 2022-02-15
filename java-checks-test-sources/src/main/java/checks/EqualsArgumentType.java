package checks;

import java.util.Random;

class EqualsArgumentType {

  public class TestClassInstanceof1 {
    @Override
    public boolean equals(Object that) { // Compliant
      if (0 == 0 || ((that) instanceof Object)) { // explicit type checking of argument
        return ((Object) that) == null;
      }
      return false;
    }
  }

  public class TestClassInstanceof2 {
    @Override
    public boolean equals(Object that) { // Noncompliant [[sc=20;ec=26]] {{Add a type test to this method.}}
      if (this instanceof Object) {
        return ((Object) that) == null;
      }
      return false;
    }
  }

  public class TestClassInstanceof3 {
    @Override
    public boolean equals(Object that) {
      if (!(that instanceof Object)) {
        return false;
      }
      return ((Object) that) == null;
    }
  }

  public class TestClassConditional1 {
    @Override
    public boolean equals(Object that) { // Noncompliant
      return condition() ? (Object) that == null : false;
    }
  }

  public class TestClassConditional2 {
    @Override
    public boolean equals(Object that) { // Compliant
      return that instanceof Object ? (Object) that == null : false; // explicit type checking of argument
    }
  }

  public class TestClassLogical1 {
    @Override
    public boolean equals(Object that) { // Noncompliant
      return condition() && (Object) that == null;
    }
  }

  public class TestClassLogical2 {
    @Override
    public boolean equals(Object that) { // Compliant
      return that instanceof Object && (Object) that == null; // explicit type checking of argument
    }
  }

  public class TestClassGetClassEqual {
    @Override
    public boolean equals(Object that) { // Compliant
      if ((that.getClass()) == (Object.class)) { // explicit type checking of argument
        return ((Object) that) == null;
      }
      return false;
    }
  }

  public class TestClassGetClassEqual1 {
    @Override
    public boolean equals(Object that) { // Compliant
      if ((Object.class) == (that.getClass())) { // explicit type checking of argument
        return ((Object) that) == null;
      }
      return false;
    }
  }

  public class TestClassGetClassEqual2 {
    @Override
    public boolean equals(Object that) { // Noncompliant
      if ((Object.class).equals(this.getClass())) {
        return ((Object) that) == null;
      }
      return false;
    }
  }

  public class TestClassGetClassEqual3 {
    @Override
    public boolean equals(Object that) { // Noncompliant
      if ((Object.class).equals(getClass())) {
        return ((Object) that) == null;
      }
      return false;
    }
  }

  public class TestClassGetClassEqual4 {
    @Override
    public boolean equals(Object that) { // Compliant
      if ((Object.class) != (that.getClass())) { // explicit type checking of argument
        return false;
      }
      return ((Object) that) == null;
    }
  }

  public class TestClassGetClassEqual5 {
    @Override
    public boolean equals(Object that) { // Compliant
      if ((that.getClass()) != (Object.class)) { // explicit type checking of argument
        return false;
      }
      return ((Object) that) == null;
    }
  }

  public class TestClassGetClassEquals6 {
    @Override
    public boolean equals(Object that) { // Compliant
      if (that.getClass().equals(Object.class)) {
      }
      return true;
    }
  }

  public class TestClassGetClassEquals7 {
    @Override
    public boolean equals(Object that) { // Compliant
      if (Object.class.equals(that.getClass())) {
      }
      return true;
    }
  }

  public class TestMethodCall {
    protected boolean method(Object that) {
      return that instanceof TestMethodCall;
    }
  }

  public class TestMethodCall1 {
    public boolean method(Object that) {
      return that instanceof TestMethodCall1;
    }

    @Override
    public boolean equals(Object that) { // Compliant
      if (method(that)) {
      }
      return ((Object) that) == null; // false negative
    }
  }

  public class TestMethodCall2 {
    public boolean method(Object that) {
      return that instanceof TestMethodCall2;
    }

    @Override
    public boolean equals(Object that) { // Compliant, implicit call of a method in this with that as argument
      if (method(that)) { // explicit type checking of argument
        return ((Object) that) == null;
      }
      return false;
    }
  }

  public class TestMethodCall3 extends TestMethodCall {
    public boolean method(Object that) {
      return that instanceof TestMethodCall3;
    }

    @Override
    public boolean equals(Object that) { // Compliant, explicit call of a method in super with that as argument
      if (super.method(that)) {
        return ((Object) that) == null;
      }
      return false;
    }
  }

  public class TestMethodCall4 {
    public boolean method(Object o1, Object o2) {
      return o2 instanceof TestMethodCall4;
    }

    @Override
    public boolean equals(Object that) { // Compliant, explicit call of a method in this with that as argument
      if (this.method(null, that)) {
        return ((Object) that) == null;
      }
      return false;
    }
  }

  public class TestMethodCall5 {
    @Override
    public boolean equals(Object that) { // Noncompliant
      if (that.equals(this)) {
        return ((Object) that) == null;
      }
      return false;
    }
  }

  public class TestMethodCall6 {
    Object[] objects;

    public boolean method(Object that) {
      return that instanceof TestMethodCall6;
    }

    @Override
    public boolean equals(Object that) { // Noncompliant
      if (objects[0].hashCode() > Integer.MIN_VALUE) {
        return ((Object) that) == null;
      }
      return false;
    }
  }

  public class TestMethodCall7 {
    @Override
    public boolean equals(Object that) { // Noncompliant
      if (that.hashCode() == this.hashCode()) {
        return ((Object) that) == null;
      }
      return false;
    }
  }

  public interface testInterface {
    public boolean equals(Object that); // Compliant
  }

  public class TestClass {
    private Object that;
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
      ((Object) this).hashCode(); // cast is allowed if the operand is not the argument.
      double d = (double) hashCode(); // cast is allowed if the operand is not the argument.
      return false;
    }

  }

  private static boolean condition() {
    return (new Random()).nextBoolean();
  }
}
