package checks;

import java.util.List;

class ArrayDesignatorAfterTypeCheckSample {

  public int getVector()[] { return new int[0]; }    // Noncompliant [[sc=25;ec=27;quickfixes=qf1]] {{Move the array designators [] to the end of the return type.}}
  // fix@qf1 {{Move [] to the return type}}
  // edit@qf1 [[sc=25;ec=27]] {{}}
  // edit@qf1 [[sc=13;ec=13]] {{[]}}

  public List<String> getList()[] { return null; } // Noncompliant [[sc=32;ec=34;quickfixes=qf2]]
  // fix@qf2 {{Move [] to the return type}}
  // edit@qf2 [[sc=32;ec=34]] {{}}
  // edit@qf2 [[sc=22;ec=22]] {{[]}}

  public int[] getVectorCompliant() { return new int[0]; }    // Compliant

  public int[] getMatrix()[] { return new int[0][]; }  // Noncompliant {{Move the array designators [] to the end of the return type.}}

  public int[][] getMatrixCompliant() { return new int[0][]; }  // Compliant

  public int foo()[][][][] { return new int[0][][][]; } // Noncompliant[[sc=19;ec=27;quickfixes=qf3]] {{Move the array designators [][][][] to the end of the return type.}}
  // fix@qf3 {{Move [][][][] to the return type}}
  // edit@qf3 [[sc=19;ec=27]] {{}}
  // edit@qf3 [[sc=13;ec=13]] {{[][][][]}}

  public int
    bar()[] { return new int[0]; }            // Noncompliant {{Move the array designators [] to the end of the return type.}}

  public int
    qix()
    [] { return new int[0]; }                 // Noncompliant {{Move the array designators [] to the end of the return type.}}

  public int[]
    goo() { return new int[0]; }              // Compliant

  interface B {
    int foo()[];                              // Noncompliant {{Move the array designators [] to the end of the return type.}}
    int[] bar();                              // Compliant
  }

}
