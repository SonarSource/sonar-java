package checks.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AssertJChainSimplificationCheckTest_QuickFix {
  void contextFreeQuickFixes() {
    assertThat(getString()).hasSize(0); // Noncompliant [[sc=29;ec=36;quickfixes=qf_context_free1]] {{Use isEmpty() instead.}}
    // fix@qf_context_free1 {{Use "isEmpty()"}}
    // edit@qf_context_free1 [[sc=29;ec=39]] {{isEmpty()}}
    assertThat(getObject()).isEqualTo(null); // Noncompliant [[sc=29;ec=38;quickfixes=qf_context_free2]] {{Use isNull() instead.}}
    // fix@qf_context_free2 {{Use "isNull()"}}
    // edit@qf_context_free2 [[sc=29;ec=44]] {{isNull()}}
    assertThat(getString().indexOf(12)).isLessThan(0); // Noncompliant [[sc=41;ec=51;quickfixes=qf_context_free3]] {{Use isNegative() instead.}}
    // fix@qf_context_free3 {{Use "isNegative()"}}
    // edit@qf_context_free3 [[sc=41;ec=54]] {{isNegative()}}
  }

  void withContextQuickFixes() {
    String x = "x";
    String y = "y";
    int length = 42;

    assertThat(getString().length()).isZero(); // Noncompliant [[sc=38;ec=44;quickfixes=qf_context1]] {{Use assertThat(actual).isEmpty() instead.}}
    // fix@qf_context1 {{Use "assertThat(actual).isEmpty()"}}
    // edit@qf_context1 [[sc=27;ec=36]] {{}}
    // edit@qf_context1 [[sc=38;ec=46]] {{isEmpty()}}

    assertThat(getFile().isAbsolute()).isFalse(); // Noncompliant	[[sc=40;ec=47;quickfixes=qf_context2]] {{Use assertThat(actual).isRelative() instead.}}
    // fix@qf_context2 {{Use "assertThat(actual).isRelative()"}}
    // edit@qf_context2 [[sc=25;ec=38]] {{}}
    // edit@qf_context2 [[sc=40;ec=49]] {{isRelative()}}

    // Expected is in the subject argument:
    assertThat(getString().matches(x)).isTrue(); // Noncompliant [[sc=40;ec=46;quickfixes=qf_context3]] {{Use assertThat(actual).matches(expected) instead.}}
    // fix@qf_context3 {{Use "assertThat(actual).matches(expected)"}}
    // edit@qf_context3 [[sc=27;ec=36]] {{).matches(}}
    // edit@qf_context3 [[sc=38;ec=48]] {{}}

    assertThat(getString().indexOf(x)).isEqualTo(-1); // Noncompliant [[sc=40;ec=49;quickfixes=qf_context4]] {{Use assertThat(actual).doesNotContain(expected) instead.}}
    // fix@qf_context4 {{Use "assertThat(actual).doesNotContain(expected)"}}
    // edit@qf_context4 [[sc=27;ec=36]] {{).doesNotContain(}}
    // edit@qf_context4 [[sc=38;ec=53]] {{}}

    // Expected is in the predicate argument
    assertThat(getString().length()).isEqualTo(12); // Noncompliant [[sc=38;ec=47;quickfixes=qf_context5]] {{Use assertThat(actual).hasSize(expected) instead.}}
    // fix@qf_context5 {{Use "assertThat(actual).hasSize(expected)"}}
    // edit@qf_context5 [[sc=27;ec=36]] {{}}
    // edit@qf_context5 [[sc=38;ec=47]] {{hasSize}}

    assertThat(getCollection().size()).isGreaterThan(length); // Noncompliant [[sc=40;ec=53;quickfixes=qf_context6]] {{Use assertThat(actual).hasSizeGreaterThan(expected) instead.}}
    // fix@qf_context6 {{Use "assertThat(actual).hasSizeGreaterThan(expected)"}}
    // edit@qf_context6 [[sc=31;ec=38]] {{}}
    // edit@qf_context6 [[sc=40;ec=53]] {{hasSizeGreaterThan}}

    // With context because we have to check that the argument of the subject is of a given type, but the fix is the same as a context free one.
    assertThat(getString().indexOf(x)).isEqualTo(0); // Noncompliant [[sc=40;ec=49;quickfixes=qf_context7]] {{Use isZero() instead.}}
    // fix@qf_context7 {{Use "isZero()"}}
    // edit@qf_context7 [[sc=40;ec=52]] {{isZero()}}

    // If "y" is not a comparable, the fix will lead to non-compilable code. The assertion does not makes sense in the first place anyway, this is acceptable.
    assertThat(x.compareTo(y)).isPositive(); // Noncompliant [[sc=32;ec=42;quickfixes=qf_context8]] {{Use assertThat(actual).isGreaterThan(expected) instead.}}
    // fix@qf_context8 {{Use "assertThat(actual).isGreaterThan(expected)"}}
    // edit@qf_context8 [[sc=17;ec=28]] {{).isGreaterThan(}}
    // edit@qf_context8 [[sc=30;ec=44]] {{}}

    assertThat(getArray().length).isPositive(); // Noncompliant [[sc=35;ec=45;quickfixes=qf_context9]] {{Use assertThat(actual).isNotEmpty() instead.}}
    // fix@qf_context9 {{Use "assertThat(actual).isNotEmpty()"}}
    // edit@qf_context9 [[sc=26;ec=33]] {{}}
    // edit@qf_context9 [[sc=35;ec=47]] {{isNotEmpty()}}
    assertThat(getArray().length).isEqualTo(length); // Noncompliant [[sc=35;ec=44;quickfixes=qf_context10]] {{Use assertThat(actual).hasSize(expected) instead.}}
    // fix@qf_context10 {{Use "assertThat(actual).hasSize(expected)"}}
    // edit@qf_context10 [[sc=26;ec=33]] {{}}
    // edit@qf_context10 [[sc=35;ec=44]] {{hasSize}}

    // Parenthesis in the predicate is not a problem through
    assertThat(getCollection().size()).isGreaterThan(((length))); // Noncompliant [[sc=40;ec=53;quickfixes=qf_context11]] {{Use assertThat(actual).hasSizeGreaterThan(expected) instead.}}
    // fix@qf_context11 {{Use "assertThat(actual).hasSizeGreaterThan(expected)"}}
    // edit@qf_context11 [[sc=31;ec=38]] {{}}
    // edit@qf_context11 [[sc=40;ec=53]] {{hasSizeGreaterThan}}

    // No quick fix suggested with redundant parenthesis in the subject, it is a non-trivial fix and
    // the dev should anyway remove the redundant parenthesis, that will make the issue appear
    assertThat(((getArray().length))).isEqualTo(length); // Noncompliant [[sc=39;ec=48;quickfixes=!]]
    assertThat((((x.compareTo(y))))).isPositive(); // Noncompliant [[sc=38;ec=48;quickfixes=!]]



    // No quick fix provided (for now), should be done in case by case (non-exhaustive list)
    assertThat(x.hashCode()).isEqualTo(y.hashCode()); // Noncompliant [[sc=30;ec=39;quickfixes=!]] {{Use assertThat(actual).hasSameHashCodeAs(expected) instead.}}
    assertThat(getMap().get(x)).isEqualTo(y); // Noncompliant [[sc=33;ec=42;quickfixes=!]] {{Use assertThat(actual).containsEntry(key, value) instead.}}
  }

  private Map<Object, Object> getMap() {
    return new HashMap<>();
  }

  private Object getObject() {
    return new Object();
  }

  private String getString() {
    return "a string";
  }

  private java.io.File getFile() {
    return File.listRoots()[0];
  }

  private Collection<Object> getCollection() {
    return new ArrayList<>();
  }

  private Object[] getArray() {
    return new Object[1];
  }
}
