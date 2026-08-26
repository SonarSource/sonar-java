package checks;

import java.util.List;

public class ContinueInLoopCheckSample {

  void unlabeledContinueInForLoop(String[] items) {
    for (int i = 0; i < items.length; i++) {
      if (items[i] == null) {
        continue; // Noncompliant {{Remove this "continue" statement.}}
      }
      process(items[i]);
    }
  }

  void unlabeledContinueInForEachLoop(List<String> items) {
    for (String item : items) {
      if (item.isEmpty()) {
        continue; // Noncompliant
      }
      process(item);
    }
  }

  void unlabeledContinueInWhileLoop(int[] data) {
    int i = 0;
    while (i < data.length) {
      i++;
      if (data[i - 1] < 0) {
        continue; // Noncompliant
      }
      process(String.valueOf(data[i - 1]));
    }
  }

  void unlabeledContinueInDoWhileLoop(int[] data) {
    int i = 0;
    do {
      if (data[i] == 0) {
        continue; // Noncompliant
      }
      process(String.valueOf(data[i]));
    } while (++i < data.length);
  }

  void labeledContinueInNestedLoop(int[][] matrix) {
    outer:
    for (int[] row : matrix) {
      for (int value : row) {
        if (value < 0) {
          continue outer; // Noncompliant
        }
        process(String.valueOf(value));
      }
    }
  }

  void multipleContinuesInOneLoop(List<String> items) {
    for (String item : items) {
      if (item == null) {
        continue; // Noncompliant
      }
      if (item.startsWith("#")) {
        continue; // Noncompliant
      }
      process(item);
    }
  }

  void continueInsideNestedIf(List<String> items, boolean flag) {
    for (String item : items) {
      if (flag) {
        if (item == null) {
          continue; // Noncompliant
        }
      }
      process(item);
    }
  }

  void compliantInvertedCondition(String[] items) {
    for (int i = 0; i < items.length; i++) {
      if (items[i] != null) { // compliant
        process(items[i]);
      }
    }
  }

  void compliantBreakOnly(List<String> items) {
    for (String item : items) {
      if ("STOP".equals(item)) {
        break; // compliant
      }
      process(item);
    }
  }

  void compliantReturnOnly(List<String> items) {
    for (String item : items) {
      if ("target".equals(item)) {
        return; // compliant
      }
    }
  }

  void compliantEmptyLoop(int count) {
    for (int i = 0; i < count; i++) {
      // compliant - no jump statement
    }
  }

  void compliantSimpleLoop(List<String> items) {
    for (String item : items) {
      process(item);
    }
  }

  private void process(String value) {
  }

}
