package checks;

import org.apache.commons.io.FileSystem;

class SwitchLastCaseIsDefaultCheckSample {
  
  void function(MyEnum myEnum) {
    switch (myEnum) { // Compliant
      case A:
        break;
      case default, B :
        break;
    }

    switch (myEnum) { // Compliant
      case A, default:
        break;
      case B:
        break;
    }
  }
  
  void ignoreWhenSwitchIsIncompleteButEnumIsUnknown(FileSystem fileSystem) {
    switch (fileSystem) { // Compliant, with incomplete semantics (or an empty classpath), we do not report an issue.
      case GENERIC:
        break;
    }
  }
}

enum MyEnum {
  A, B, C;

  MyEnum field;
}
