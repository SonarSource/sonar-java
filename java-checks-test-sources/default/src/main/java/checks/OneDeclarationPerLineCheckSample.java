package checks;

import java.util.Collections;

class OneDeclarationPerLineCheckSample {

  int no; int spaceBefore; // Noncompliant [[quickfixes=qf_indentation2]]
//            ^^^^^^^^^^^
  // fix@qf_indentation2 {{Declare on separated lines}}
  // edit@qf_indentation2 [[sc=10;ec=11]] {{\n  }}
}
