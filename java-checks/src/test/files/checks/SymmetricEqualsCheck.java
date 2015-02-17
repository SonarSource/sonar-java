public class Fruit extends Food {
  private Season ripe;

  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (Fruit.class == obj.getClass()) { // Noncompliant; broken for child classes
      return ripe.equals(((Fruit) obj).getRipe());
    }
    if (obj instanceof Fruit) {  // Noncompliant; broken for child classes
      return ripe.equals(((Fruit) obj).getRipe());
    } else if (obj instanceof Season) { // Noncompliant; symmetry broken for Season class
      // ...
    }
    if (Fruit.class != obj.getClass()) { // Noncompliant; broken for child classes
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
        if (Foo.class==obj.getClass()){ //NonCompliant

        }
        if (obj instanceof Season) { // Noncompliant; symmetry broken for Season class
          // ...
        }
        return false;
      }
    }
    return false;
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