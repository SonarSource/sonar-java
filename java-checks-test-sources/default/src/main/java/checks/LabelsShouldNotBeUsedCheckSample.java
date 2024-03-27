package checks;

class LabelsShouldNotBeUsedCheckSample {
  void foo() {
    int matrix[][] = {
        {1, 2, 3},
        {4, 5, 6},
        {7, 8, 9}
      };

      outer: for (int row = 0; row < matrix.length; row++) {   // Noncompliant [[sc=7;ec=12]] {{Refactor the code to remove this label and the need for it.}}
        for (int col = 0; col < matrix[row].length; col++) {
          if (col == row) {
            continue outer;
          }
          System.out.println(matrix[row][col]);                // Prints the elements under the diagonal, i.e. 4, 7, 8
        }
      }


      for (int row = 1; row < matrix.length; row++) {          // Compliant
        for (int col = 0; col < row; col++) {
          System.out.println(matrix[row][col]);                // Also prints 4, 7, 8
        }
      }

      switch (0) {
        case 0:
        case 1:
        default:
            break;
      }
  }
}
