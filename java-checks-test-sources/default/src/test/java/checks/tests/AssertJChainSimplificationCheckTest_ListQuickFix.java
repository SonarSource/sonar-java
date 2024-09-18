package checks.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AssertJChainSimplificationCheckTest_ListQuickFix {

  List<String> stringsList = new ArrayList<>();
  String[] stringArray = new String[0];
  String string = new String();

  // test https://sonarsource.atlassian.net/browse/SONARJAVA-5111
  void contextFreeQuickFixes() {

    assertThat(stringsList).isEmpty(); // Compliant
    assertThat(stringsList.hashCode()).isEqualTo(0); // Noncompliant {{Use isZero() instead.}}

    assertThat(stringsList.size()).isEqualTo(0); // Noncompliant {{Use assertThat(actual).isEmpty() instead.}} [[quickfixes=qf_context1]]
//                                 ^^^^^^^^^
    // fix@qf_context1 {{Use "assertThat(actual).isEmpty()"}}
    // edit@qf_context1 [[sc=27;ec=34]] {{}}
    // edit@qf_context1 [[sc=36;ec=48]] {{isEmpty()}}

    assertThat(string).isEmpty(); // Compliant
    assertThat(string.hashCode()).isEqualTo(0); // Noncompliant {{Use isZero() instead.}}

    assertThat(string.length()).isEqualTo(0); // Noncompliant {{Use assertThat(actual).isEmpty() instead.}} [[quickfixes=qf_context2]]
//                              ^^^^^^^^^
    // fix@qf_context2 {{Use "assertThat(actual).isEmpty()"}}
    // edit@qf_context2 [[sc=22;ec=31]] {{}}
    // edit@qf_context2 [[sc=33;ec=45]] {{isEmpty()}}

    assertThat(stringArray).isEmpty(); // Compliant
    assertThat(stringArray.hashCode()).isEqualTo(0); // Noncompliant {{Use isZero() instead.}}

    assertThat(stringArray.length).isEqualTo(0); // Noncompliant {{Use assertThat(actual).isEmpty() instead.}} [[quickfixes=qf_context3]]
//                                 ^^^^^^^^^
    // fix@qf_context3 {{Use "assertThat(actual).isEmpty()"}}
    // edit@qf_context3 [[sc=27;ec=34]] {{}}
    // edit@qf_context3 [[sc=36;ec=48]] {{isEmpty()}}

  }

}
