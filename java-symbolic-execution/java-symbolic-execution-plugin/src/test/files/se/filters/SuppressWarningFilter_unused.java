class UnusedSuppression {

  // issues raises by java:S2583
  public void assign(boolean parameter) {
    parameter = false;
    if (parameter) { // WithIssue
      if (parameter) { // Compliant, unreachable
      }
    }
    if (!parameter) { // WithIssue
      if (!parameter) { // WithIssue
      }
    }
  }
}

@SuppressWarnings("unused")
class UnusedSuppressionSuppressed {
  public void assign(boolean parameter) {
    parameter = false;
    if (parameter) { // NoIssue
      if (parameter) { // Compliant, unreachable
      }
    }
    if (!parameter) { // NoIssue
      if (!parameter) { // NoIssue
      }
    }
  }
}

@SuppressWarnings("java:S2583")
class UnusedSuppressionSuppressed2 {
  public void assign(boolean parameter) {
    parameter = false;
    if (parameter) { // NoIssue
      if (parameter) { // Compliant, unreachable
      }
    }
    if (!parameter) { // NoIssue
      if (!parameter) { // NoIssue
      }
    }
  }
}
