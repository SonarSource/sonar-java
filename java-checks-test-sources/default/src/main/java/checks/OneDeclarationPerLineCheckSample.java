package checks;

class OneDeclarationPerLineCheckSample {

  int i = 0, j[] = null, k[][] = new int[0][0]; // Noncompliant [[quickfixes=qf_indentation1]]
  //         ^
  // fix@qf_indentation1 {{Declare on separated lines}}
  // edit@qf_indentation1 [[sc=12;ec=17]] {{;\n  int[] j }}
  // edit@qf_indentation1 [[sc=24;ec=31]] {{;\n  int[][] k }}

  int one, two; // Noncompliant
  //       ^^^

  int no; int spaceBefore; // Noncompliant [[quickfixes=qf_indentation2]]
  //          ^^^^^^^^^^^
  // fix@qf_indentation2 {{Declare on separated lines}}
  // edit@qf_indentation2 [[sc=10;ec=11]] {{\n  }}

  int arr1[] = null, arr2[] = null; // Noncompliant [[quickfixes=qf_indentation3]]
  //                 ^^^^
  // fix@qf_indentation3 {{Declare on separated lines}}
  // edit@qf_indentation3 [[sc=20;ec=28]] {{;\n  int[] arr2 }}

  public int three, four; // Noncompliant [[quickfixes=qf_indentation4]]
  //                ^^^^
  // fix@qf_indentation4 {{Declare on separated lines}}
  // edit@qf_indentation4 [[sc=19;ec=21]] {{;\n  public int  }}

}
