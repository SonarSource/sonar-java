package checks;

import java.util.Set;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

class EnumSetCheck {

  public enum COLOR {
    RED, GREEN, BLUE, ORANGE;
  }

  public enum E {
    E1, E2, E3, E4, E5, E6
  }

  abstract class SetString implements Set<String> {
  }
  abstract class ExtendedSet<E> implements Set<E> {
  }

  public void doSomething(Set<COLOR> param) { // compliant, we ignore parameters.
    Set<COLOR> warm = new HashSet<COLOR>(); // Noncompliant [[sc=23;ec=43]] {{Convert this Set to an EnumSet.}}
    warm.add(COLOR.RED);
    warm.add(COLOR.ORANGE);
    Set foo = new HashSet();
    SetString ss;
    ExtendedSet<COLOR> es; // Compliant, we check only initializer.
    Set warm2 = new HashSet<COLOR>(); // Noncompliant [[sc=17;ec=37]] {{Convert this Set to an EnumSet.}}
    EnumSet<COLOR> warm3 = EnumSet.of(COLOR.RED, COLOR.ORANGE);
    Set<COLOR> warm4 = EnumSet.of(COLOR.RED, COLOR.ORANGE);
    Set<Integer> ports2 = new HashSet<>();
    Set<COLOR> ports = new HashSet<>(); // Noncompliant [[sc=24;ec=39]] {{Convert this Set to an EnumSet.}}
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

    Set<COLOR> col8 = Set.of(COLOR.RED); // Noncompliant
    Set set= EnumSet.of(E.E1,E.E2,E.E3,E.E4,E.E5,E.E6); //Compliant, overload of(E first, E... rest) properly resolved
  }

  private Set<COLOR> rgb() {
    return EnumSet.of(COLOR.RED, COLOR.GREEN, COLOR.BLUE);
  }
}
