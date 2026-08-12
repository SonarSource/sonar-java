package checks;

import static com.example.UnknownClass.NaN;

class NanEqualityCheckSample {

  void unknownNaN() {
    double x = 1.0;
    if (x == NaN) { } // Compliant - NaN symbol is unknown, cannot determine type
    if (NaN == x) { } // Compliant
  }

  void unknownMemberSelect() {
    double x = 1.0;
    if (x == UnknownType.NaN) { } // Compliant - UnknownType is not resolvable
  }

}
