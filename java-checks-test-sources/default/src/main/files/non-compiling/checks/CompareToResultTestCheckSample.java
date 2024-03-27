package checks;

class CompareToResultTestCheckSample {
  class MyComparable implements Comparable<MyComparable> {
    public void aMethod(MyComparable other, NotComparable notComparable) {
      if (1 == compareTo("", "", "")) { // Compliant, unknown compareTo
      }
      int c4;
      if (c4 == 1) {  // Compliant, variable not initialized
      }
      if (unknownVar == 1) { // Compliant, unknown variable
      }
    }

    public int compareTo(MyComparable other1, MyComparable other2) {
      return 0;
    }

    @Override
    public int compareTo(MyComparable o) {
      return 0;
    }
  }
}
