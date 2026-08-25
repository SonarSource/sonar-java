package checks;

class CopyConstructorWithUnresolvedHelper {
  private int value;

  CopyConstructorWithUnresolvedHelper(CopyConstructorWithUnresolvedHelper other) {
    this.initializeFromMissingDependency(other);
  }
}

class CopyConstructorWithImplicitUnresolvedHelper {
  private int value;

  CopyConstructorWithImplicitUnresolvedHelper(CopyConstructorWithImplicitUnresolvedHelper other) {
    initializeFromMissingDependency(other);
  }
}

class ConstructorWithUnresolvedParameter {
  private int value;

  ConstructorWithUnresolvedParameter(MissingType other) {
  }
}

class CopyConstructorWithUnresolvedDelegation {
  private int value;

  CopyConstructorWithUnresolvedDelegation(CopyConstructorWithUnresolvedDelegation other) {
    this(other, MissingDependency.VALUE);
  }
}
