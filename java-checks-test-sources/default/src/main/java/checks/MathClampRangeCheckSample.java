package checks;

public class MathClampRangeCheckSample {
  void test(
    double d1, double d2, double d3,
    float f1, float f2, float f3,
    long l1, long l2, long l3,
    int i1, int i2, int i3) {

    Math.clamp(d1, d2, d3); // Compliant
    Math.clamp(f1, f2, f3); // Compliant
    Math.clamp(l1, l2, l3); // Compliant
    Math.clamp(i1, i2, i3); // Compliant

    Math.clamp(d1, 0.0d, 10.0d); // Compliant
    Math.clamp(f1, 0.0f, 10.0f); // Compliant
    Math.clamp(l1, 0L, 10L); // Compliant
    Math.clamp(i1, 0, 10); // Compliant

    // equals
    Math.clamp(d1, d2, d2); // Noncompliant [[quickfixes=!]] {{Change the "clamp(value,min,max)"'s arguments so "min" is not equals to "max".}}
//                 ^^
    Math.clamp(f1, f1, f3); // Noncompliant {{Change the "clamp(value,min,max)"'s arguments so "min" is not equals to "value".}}
    Math.clamp(l1, l2, l1); // Noncompliant {{Change the "clamp(value,min,max)"'s arguments so "max" is not equals to "value".}}
    Math.clamp(i1, i2, i1); // Noncompliant {{Change the "clamp(value,min,max)"'s arguments so "max" is not equals to "value".}}

    Math.clamp(d1, 5.0d, 5.0d); // Noncompliant {{Change the "clamp(value,min,max)"'s arguments so "min" is not equals to "max".}}
    Math.clamp(5.0f, 5.0f, f3); // Noncompliant {{Change the "clamp(value,min,max)"'s arguments so "min" is not equals to "value".}}
    Math.clamp(10L, l2, 10L); // Noncompliant {{Change the "clamp(value,min,max)"'s arguments so "max" is not equals to "value".}}
    Math.clamp(10, l2, 10); // Noncompliant {{Change the "clamp(value,min,max)"'s arguments so "max" is not equals to "value".}}

    Math.clamp(d1, (unknownValue() + 42) * 666, (unknownValue() + 42) * 666); // Noncompliant {{Change the "clamp(value,min,max)"'s arguments so "min" is not equals to "max".}}

    // less than
    Math.clamp(i1, i2, i3); // Compliant

    Math.clamp(l1, 10L, 0L); // Noncompliant [[quickfixes=qf1]] {{Change the "clamp(value,min,max)"'s arguments so "max" is not always less than "min".}}
//                      ^^
//  ^^^<
    // fix@qf1 {{Swap "max" and "min" arguments}}
    // edit@qf1 [[sc=25;ec=27]] {{10L}}
    // edit@qf1 [[sc=20;ec=23]] {{0L}}

    Math.clamp(0, i2, 10); // Noncompliant {{Change the "clamp(value,min,max)"'s arguments so "value" is not always less than "max".}}
    Math.clamp(10L, 0, l3); // Noncompliant {{Change the "clamp(value,min,max)"'s arguments so "min" is not always less than "value".}}

    Math.clamp(d1, 10.0d, 0.0d); // FN limitation, ExpressionTree.asConstant() does not support "double"
    Math.clamp(0.0f, f2, 10.0f); // FN limitation, ExpressionTree.asConstant() does not support "float"
  }

  int unknownValue() {
    return hashCode();
  }
}
