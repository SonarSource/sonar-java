public class Fruit extends Food {
  private Season ripe;

  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (Fruit.class == obj.getClass()) { // Noncompliant [[sc=9;ec=20]] {{Compare to "this.getClass()" instead.}}
      return ripe.equals(((Fruit) obj).getRipe());
    }
    if (obj instanceof Fruit) {  // Noncompliant [[sc=9;ec=29]] {{Compare to "this.getClass()" instead.}}
      return ripe.equals(((Fruit) obj).getRipe());
    } else if (obj instanceof Season) { // Noncompliant [[sc=16;ec=37]] {{Remove this comparison to an unrelated class.}}
      // ...
    }
    if (Fruit.class != obj.getClass()) { // Noncompliant broken for child classes
    }
  }
}
public class Fruit2 extends Food {
  private Season ripe;

  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (this.getClass()==obj.getClass()){
      return ripe.equals(((Fruit2) obj).getRipe());
    }
    return false;
  }
}

public class Fruit3 extends Food {

  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (this.getClass()==obj.getClass()){
      return ripe.equals(((Fruit2) obj).getRipe());
    }
    class Foo {
      public boolean equals(Object j) {
        if (Foo.class==obj.getClass()){ // Noncompliant

        }
        if (obj instanceof Season) { // Noncompliant [[sc=13;ec=34]] symmetry broken for Season class
          // ...
        }
        return false;
      }
    }
    return false;
  }
}

public class BaseClass {

  @Override
  public final boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (!(obj instanceof BaseClass)) return false; // Compliant; equals is final
      return true;
  }
}

public interface I {
  public abstract boolean equals(Object anObject);
}
public final class Fruit4 extends Food {
  private Season ripe;

  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (Fruit4.class == obj.getClass()) { // Compliant; class is final
      return ripe.equals(((Fruit4) obj).getRipe());
    }
    if (obj instanceof Fruit4) {  // Compliant; class is final
      return ripe.equals(((Fruit4) obj).getRipe());
    }
    if (Fruit4.class != obj.getClass()) { // Compliant; class is final
    }
  }
}
final class Foo<T> {
  public boolean equals(Object o) {
    if(o instanceof Foo) {

    }
  }
}
class Parent {}
class Child extends UnrelatedClassLiteral {}
class UnrelatedClassLiteral extends Parent {
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (obj.getClass() == this.getClass()) {
      return f.equals(((MyObject) obj).f);
    }

    if (obj.getClass() == String.class) { // Noncompliant [[sc=27;ec=39]] {{Remove this comparison to an unrelated class.}}
      return f.equals(obj.toString());
    }
    if (obj.getClass() == Parent.class) { // Noncompliant
    }
    if (obj.getClass() == Child.class) { // Noncompliant
    }
    return false;
  }
}
