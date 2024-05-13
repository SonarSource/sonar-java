import java.io.Serializable;
public class Fruit implements Serializable {
  private static final long serialVersionUID = 1;

  private Object readResolve() throws ObjectStreamException // Noncompliant
  {}
}

public class Raspberry extends Fruit implements Serializable {  // No access to parent's readResolve() method
}

public class Fruit2 implements Serializable {
  private static final long serialVersionUID = 1;

  protected Object readResolve() throws ObjectStreamException
  {}

}

public class Raspberry2 extends Fruit2 implements Serializable {
}

private class Fruit3 implements Serializable {
  private static final long serialVersionUID = 1;
  private Object readResolve() throws ObjectStreamException  // compliant owner is private
  {}
}
public final class Fruit4 implements Serializable {
  private static final long serialVersionUID = 1;
  private Object readResolve() throws ObjectStreamException  // compliant owner is final
  {}
}

public class Fruit5 {
  private Object readResolve() throws ObjectStreamException  // compliant, Fruit5 is not a subtype of Serializable
  {}
}
public class Fruit6 implements Serializable {
  private static final long serialVersionUID = 1;

  private Object readResolve(int a) throws ObjectStreamException  // compliant, not read resolve method
  {}
  void foo() {}
}
