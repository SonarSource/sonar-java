import java.util.Set;
import java.util.EnumSet;
import java.util.HashSet;

class A {

  public enum COLOR {
    RED, GREEN, BLUE, ORANGE;
  }

  class SetString implements Set<String> {
  }
  class SetColor implements Set<COLOR> {
  }
  class ExtendedSet<E> implements Set<E> {
  }

  public void doSomething(Set<COLOR> param) { // Noncompliant [[sc=27;ec=37]] {{Convert this Set to an EnumSet.}}
    Set<COLOR> warm = new HashSet<COLOR>(); // Noncompliant [[sc=23;ec=43]] {{Convert this Set to an EnumSet.}}
    Set foo = new HashSet();
    warm.add(COLORS.RED);
    warm.add(COLORS.ORANGE);
    SetString ss;
    SetColor sc;
    ExtendedSet<COLOR> es; // Noncompliant [[sc=5;ec=23]] {{Convert this Set to an EnumSet.}}
    Set warm2 = new HashSet<COLOR>(); // Noncompliant [[sc=17;ec=37]] {{Convert this Set to an EnumSet.}}
    EnumSet<COLOR> warm3 = EnumSet.of(COLOR.RED, COLOR.ORANGE);
    Set<COLOR> warm4 = EnumSet.of(COLOR.RED, COLOR.ORANGE);
    Set<Integer> ports = new HashSet<>();
    Set<COLOR> ports = new HashSet<>(); // Noncompliant [[sc=5;ec=15]] {{Convert this Set to an EnumSet.}}
  }
}
