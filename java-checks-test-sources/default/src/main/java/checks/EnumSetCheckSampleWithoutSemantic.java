package checks;

import java.util.Set;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

class EnumSetCheckSampleWithoutSemantic {

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
    Set<COLOR> warm = new HashSet<COLOR>(); // Noncompliant

    warm.add(COLOR.RED);
    warm.add(COLOR.ORANGE);
    Set foo = new HashSet();
    SetString ss;
    ExtendedSet<COLOR> es; // Compliant, we check only initializer.
    Set warm2 = new HashSet<COLOR>(); // Noncompliant

    EnumSet<COLOR> warm3 = EnumSet.of(COLOR.RED, COLOR.ORANGE);
    Set<COLOR> warm4 = EnumSet.of(COLOR.RED, COLOR.ORANGE);
    Set<Integer> ports2 = new HashSet<>();
    Set<COLOR> ports = new HashSet<>(); // Noncompliant

    Set<COLOR> ports4 = Sets.immutableEnumSet(COLOR.RED); // Compliant - guava use an enum set with constraint of immutability
    Set<COLOR> ports5 = Sets.immutableEnumSet(Lists.newArrayList(COLOR.RED)); // Compliant - guava use an enum set with constraint of immutability
    Collection<COLOR> col = new ArrayList<>();
    Set<COLOR> col2 = Collections.unmodifiableSet(EnumSet.of(COLOR.RED, COLOR.ORANGE));
    Set<COLOR> col3 = Collections.unmodifiableSet(new HashSet<COLOR>()); // Noncompliant
    Set<COLOR> col4 = rgb(); // Compliant

    Set<COLOR> col8 = Set.of(COLOR.RED); // Noncompliant
    Set set= EnumSet.of(E.E1,E.E2,E.E3,E.E4,E.E5,E.E6); //Compliant, overload of(E first, E... rest) properly resolved

    // We are not computing the exact runtime type when the initializer is not a method invocation/new class.
    Set<COLOR> col3_1 = Collections.unmodifiableSet(warm3); // Compliant, created from EnumSet
    Set<COLOR> col3_2 = Collections.unmodifiableSet(warm4); // Compliant, created from EnumSet

    Set<COLOR> ternaryInit = param.isEmpty() ? EnumSet.allOf(COLOR.class) : EnumSet.of(COLOR.GREEN); // Compliant

    int i = 42;
    Set<COLOR> switchExpressionInit = switch (i) { // Compliant
      case 1 -> EnumSet.of(COLOR.GREEN);
      default -> EnumSet.allOf(COLOR.class);
    };
  }

  private Set<COLOR> rgb() {
    return EnumSet.of(COLOR.RED, COLOR.GREEN, COLOR.BLUE);
  }
}
