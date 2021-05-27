package checks;

import org.apache.commons.io.FileSystem;

class SwitchLastCaseIsDefaultCheck {
  void ignoreWhenSwitchIsIncompleteButEnumIsUnknown(FileSystem fileSystem) {
    switch (fileSystem) { // Compliant, with incomplete semantics (or an empty classpath), we do not report an issue.
      case GENERIC:
        break;
    }
  }
}
