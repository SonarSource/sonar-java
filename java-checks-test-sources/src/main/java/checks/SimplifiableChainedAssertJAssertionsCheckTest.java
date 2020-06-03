package checks;

import static org.assertj.core.api.Assertions.assertThat;

public class SimplifiableChainedAssertJAssertionsCheckTest {

  private Object getObject() {
    return new Object();
  }

  private boolean getBoolean() {
    return true;
  }

  void objectRelatedAssertionChains() {
    Comparable x = getBoolean();
    Object y = getObject();

    assertThat(getObject()).isEqualTo(null); // Noncompliant [[sc=29;ec=38]] {{Replace with isNull()}}
    assertThat(getObject()).isNotEqualTo(null); // Noncompliant {{Replace with isNotNull()}}
    assertThat(getObject()).isEqualTo(null).isNotEqualTo(getObject()); // Noncompliant
    assertThat(getObject()).as("some message)").isEqualTo(getObject()).withFailMessage("another message)").isNotEqualTo(null).as("a third message"); // Noncompliant [[sc=108;ec=120]]
    assertThat(new Object()).isEqualTo(null) // Noncompliant
      .isNotEqualTo(null); // Noncompliant

    assertThat(getBoolean()).isEqualTo(true); // Noncompliant {{Replace with isTrue()}}
    assertThat(getBoolean()).isEqualTo(false); // Noncompliant {{Replace with isFalse()}}
    assertThat(x.equals(y)).isTrue(); // Noncompliant [[sc=29;ec=35;secondary=28]] {{Replace with assertThat(actual).isEqualTo(expected)}}
    assertThat(x.equals(y)).isFalse(); // Noncompliant {{Replace with assertThat(actual).isNotEqualTo(expected)}}
    assertThat(x == y).isTrue(); // Noncompliant {{Replace with assertThat(actual).isSameAs(expected)}}
    assertThat(x == y).isFalse(); // Noncompliant {{Replace with assertThat(actual).isNotSameAs(expected)}}
    assertThat(x != y).isTrue(); // Noncompliant {{Replace with assertThat(actual).isNotSameAs(expected)}}
    assertThat(x != y).isFalse(); // Noncompliant {{Replace with assertThat(actual).isSameAs(expected)}}
    assertThat(x == null).isTrue(); // Noncompliant {{Replace with assertThat(actual).isNull()}}
    assertThat(x != null).isTrue(); // Noncompliant {{Replace with assertThat(actual).isNotNull()}}
    assertThat(x == null).isFalse(); // Noncompliant {{Replace with assertThat(actual).isNotNull()}}
    assertThat(x != null).isFalse(); // Noncompliant {{Replace with assertThat(actual).isNull()}}
    assertThat(x.toString()).isEqualTo(y); // Noncompliant {{Replace with assertThat(actual).hasToString(expectedString)}}
    assertThat(x.hashCode()).isEqualTo(y.hashCode()); // Noncompliant {{Replace with assertThat(actual).hasSameHashCodeAs(expected)}}
    assertThat(getObject() instanceof String).isTrue(); // Noncompliant {{Replace with assertThat(actual).isInstanceOf(ExpectedClass.class)}}
    assertThat(getObject() instanceof String).isFalse(); // Noncompliant {{Replace with assertThat(actual).isNotInstanceOf(ExpectedClass.class)}}

    assertThat(x.compareTo(y)).isEqualTo(0); // Noncompliant {{Replace with assertThat(actual).isEqualByComparingTo(expected)}}
    assertThat(x.compareTo(y)).isNotEqualTo(0); // Noncompliant {{Replace with assertThat(actual).isNotEqualByComparingTo(expected)}}
    assertThat(x.compareTo(y)).as("message").isNotEqualTo(0); // Noncompliant
    assertThat(x.compareTo(y)).isNotEqualTo(0).isNotEqualTo(-1); // Compliant as we have >1 context-dependant predicate
    assertThat(x.compareTo(y)).isNotEqualTo(0).isNotEqualTo(null).isNotEqualTo(-1); // Noncompliant [[sc=48;ec=60]] - but only raise issue on the 'null' check

    assertThat(x.compareTo(y)).isZero(); // Noncompliant {{Replace with assertThat(actual).isEqualByComparingTo(expected)}}
    assertThat(x.compareTo(y)).isNotZero(); // Noncompliant {{Replace with assertThat(actual).isNotEqualByComparingTo(expected)}}
    assertThat(x.compareTo(y)).isGreaterThan(-1); // Noncompliant {{Replace with assertThat(actual).isGreaterThanOrEqualTo(expected)}}
    assertThat(x.compareTo(y)).isGreaterThan(0); // Noncompliant {{Replace with assertThat(actual).isGreaterThan(expected)}}
    assertThat(x.compareTo(y)).isGreaterThanOrEqualTo(0); // Noncompliant {{Replace with assertThat(actual).isGreaterThanOrEqualTo(expected)}}
    assertThat(x.compareTo(y)).isGreaterThanOrEqualTo(1); // Noncompliant {{Replace with assertThat(actual).isGreaterThan(expected)}}
    assertThat(x.compareTo(y)).isLessThan(1); // Noncompliant {{Replace with assertThat(actual).isLessThanOrEqualTo(expected)}}
    assertThat(x.compareTo(y)).isLessThan(0); // Noncompliant {{Replace with assertThat(actual).isLessThan(expected)}}
    assertThat(x.compareTo(y)).isLessThanOrEqualTo(0); // Noncompliant {{Replace with assertThat(actual).isLessThanOrEqualTo(expected)}}
    assertThat(x.compareTo(y)).isLessThanOrEqualTo(-1); // Noncompliant {{Replace with assertThat(actual).isLessThan(expected)}}
    assertThat(x.compareTo(y)).isNegative(); // Noncompliant {{Replace with assertThat(actual).isLessThan(expected)}}
    assertThat(x.compareTo(y)).isNotNegative(); // Noncompliant {{Replace with assertThat(actual).isGreaterThanOrEqualTo(expected)}}
    assertThat(x.compareTo(y)).isPositive(); // Noncompliant {{Replace with assertThat(actual).isGreaterThan(expected)}}
    assertThat(x.compareTo(y)).isNotPositive(); // Noncompliant {{Replace with assertThat(actual).isLessThanOrEqualTo(expected)}}

    assertThat(x.compareTo(y)).isOne(); // Compliant
  }
}
