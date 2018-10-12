import java.util.Set;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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

  public void doSomething(Set<COLOR> param) { // compliant, we ignore parameters.
    Set<COLOR> warm = new HashSet<COLOR>(); // Noncompliant [[sc=23;ec=43]] {{Convert this Set to an EnumSet.}}
    warm.add(COLOR.RED);
    warm.add(COLOR.ORANGE);
    Set foo = new HashSet();
    SetString ss;
    SetColor sc;
    ExtendedSet<COLOR> es; // Compliant, we check only initializer.
    Set warm2 = new HashSet<COLOR>(); // Noncompliant [[sc=17;ec=37]] {{Convert this Set to an EnumSet.}}
    EnumSet<COLOR> warm3 = EnumSet.of(COLOR.RED, COLOR.ORANGE);
    Set<COLOR> warm4 = EnumSet.of(COLOR.RED, COLOR.ORANGE);
    Set<Integer> ports2 = new HashSet<>();
    Set<COLOR> ports = new HashSet<>(); // Noncompliant [[sc=24;ec=39]] {{Convert this Set to an EnumSet.}}
    SetColor ports3 = new HashSet<>();
    Set<COLOR> ports4 = Sets.immutableEnumSet(COLOR.RED); // Compliant - guava use an enum set with constraint of immutability
    Set<COLOR> ports5 = Sets.immutableEnumSet(Lists.newArrayList(COLOR.RED)); // Compliant - guava use an enum set with constraint of immutability
    Set<COLOR> ports6 = Sets.newHashSet(COLOR.RED); // Noncompliant {{Convert this Set to an EnumSet.}}
    Collection<COLOR> col = new ArrayList<>();
    Set<COLOR> col2 = Collections.unmodifiableSet(EnumSet.of(COLOR.RED, COLOR.ORANGE));
    Set<COLOR> col3 = Collections.unmodifiableSet(new HashSet<COLOR>()); // Noncompliant
    Set<COLOR> col4 = rgb(); // Compliant

    Set<COLOR> col5 = ImmutableSet.<COLOR>of(); // Noncompliant
    Set<COLOR> col6 = ImmutableSet.of(COLOR.RED, COLOR.ORANGE); // Noncompliant
    Set<COLOR> col7 = ImmutableSet.of(COLOR.RED, COLOR.BLUE, COLOR.RED, COLOR.ORANGE, COLOR.GREEN, COLOR.ORANGE, COLOR.BLUE); // Noncompliant

    Set<COLOR> col8 = Set.of(COLOR.RED); // Compliant - not resolved in java versions < 9
  }

  private Set<COLOR> rgb() {
    return EnumSet.of(COLOR.RED, COLOR.GREEN, COLOR.BLUE);
  }
}
