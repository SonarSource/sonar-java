package checks;

import java.util.Arrays;

class ArraysFillIncompatibleTypeCheckSample {

  void testUnresolved(UnknownType[] unknownArray, UnknownType unknownVal, String[] strArray, String[][] str2D, UnknownType[] unknownArray1D) {
    Arrays.fill(unknownArray, "value"); // Compliant: componentType is unknown
    Arrays.fill(unknownArray, unknownVal); // Compliant: both are unknown
    Arrays.fill(strArray, unknownVal); // Compliant: fillingType is unknown
    Arrays.fill(str2D, unknownArray1D); // Compliant: element of 2D array is unknown
  }

}
