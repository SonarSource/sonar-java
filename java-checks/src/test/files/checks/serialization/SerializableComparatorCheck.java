import java.io.Serializable;
import java.util.Comparator;

class A implements Comparator<String> {} // Noncompliant {{Make this class "Serializable".}}
//    ^
class B implements Comparator<String>, Serializable {}
abstract class C implements Comparator<String> {}
class D extends C {} // Noncompliant
class E implements Cloneable {
  Comparator comp = new Comparator() {
    @Override
    public int compare(Object o, Object o2) {
      return 0;
    }
  };

}
