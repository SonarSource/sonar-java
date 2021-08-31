package checks;

class ArrayDesignatorOnVariableCheck {
  int[] a,
        b,
        c[][][][][], // Noncompliant [[sc=10;ec=20;quickfixes=!]] {{Move the array designator from the variable to the type.}}
        d[], // Noncompliant [[sc=10;ec=12;quickfixes=!]] {{Move the array designator from the variable to the type.}}
        e,
        f
        []; // Noncompliant [[sc=9;ec=11;quickfixes=!]] {{Move the array designator from the variable to the type.}}

  interface B {
    int a[] = null; // Noncompliant [[sc=10;ec=12;quickfixes=qf1]] {{Move the array designator from the variable to the type.}}
    // fix@qf1 {{Move [] to the variable type}}
    // edit@qf1 [[sc=10;ec=12]] {{}}
    // edit@qf1 [[sc=8;ec=8]]{{[]}}
    int[] b = null; // Compliant
  }

  class C {
    private void foo(
      int[] a,
      int b[]) { // Noncompliant [[sc=12;ec=14;quickfixes=qf2]] {{Move the array designator from the variable to the type.}}
      // fix@qf2 {{Move [] to the variable type}}
      // edit@qf2 [[sc=12;ec=14]] {{}}
      // edit@qf2 [[sc=10;ec=10]]{{[]}}

      for (String c[] : new String[0][]) { // Noncompliant [[sc=20;ec=22;quickfixes=qf3]] {{Move the array designator from the variable to the type.}}
        // fix@qf3 {{Move [] to the variable type}}
        // edit@qf3 [[sc=20;ec=22]] {{}}
        // edit@qf3 [[sc=18;ec=18]]{{[]}}
      }

      for (String d[] = new String[0];;) { // Noncompliant [[sc=20;ec=22;quickfixes=qf4]]
        // fix@qf4 {{Move [] to the variable type}}
        // edit@qf4 [[sc=20;ec=22]] {{}}
        // edit@qf4 [[sc=18;ec=18]]{{[]}}
        break;
      }

      for (String[] f : new String[0][]) { // Compliant
      }

      {
        int g;
        int h[]; // Noncompliant [[sc=14;ec=16;quickfixes=qf5]]
        // fix@qf5 {{Move [] to the variable type}}
        // edit@qf5 [[sc=14;ec=16]] {{}}
        // edit@qf5 [[sc=12;ec=12]]{{[]}}
        int i;
        bar();
        int l[], m; // Noncompliant [[sc=14;ec=16;quickfixes=!]]
        int[] n[][]; // Noncompliant [[sc=16;ec=20;quickfixes=qf6]]
        // fix@qf6 {{Move [][] to the variable type}}
        // edit@qf6 [[sc=16;ec=20]] {{}}
        // edit@qf6 [[sc=14;ec=14]]{{[][]}}
        int[] o[][][]; // Noncompliant [[sc=16;ec=22;quickfixes=qf7]]
        // fix@qf7 {{Move [][][] to the variable type}}
        // edit@qf7 [[sc=16;ec=22]] {{}}
        // edit@qf7 [[sc=14;ec=14]]{{[][][]}}
      }
    }

    private int bar()[] { // Compliant
      return new int[0];
    }

    private int lum(int... a) {// Compliant
      return 0;
    }

  }
}
