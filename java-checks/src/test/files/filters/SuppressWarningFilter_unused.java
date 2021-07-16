class UnusedSuppression // WithIssue
  <Unused> { // WithIssue
  private int field = 1; // WithIssue

  private void f(int p // WithIssue
  ) { // WithIssue
    int local = 1; // WithIssue

    unused_label: for (int i = 0; i < 1; i++) { } // WithIssue

    local = 2; // WithIssue


    if (false) {
      f(p); // WithIssue
    }
  }
}

@SuppressWarnings("unused")
class UnusedSuppressionSuppressed // NoIssue
  <Unused> { // NoIssue
  private int field = 1; // NoIssue

  private void f( // NoIssue
                  int p) { // NoIssue
    int local = 1; // NoIssue

    unused_label: for (int i = 0; i < 1; i++) { } // NoIssue

    local = 2; // NoIssue


    if (false) {
      f(p); // WithIssue
    }
  }
}
