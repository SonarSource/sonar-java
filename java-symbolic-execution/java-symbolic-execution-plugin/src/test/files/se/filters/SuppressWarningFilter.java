package org.sonar.java.filters;

import java.io.Serializable;

// divzero suppresses S3518
@SuppressWarnings("divzero") // WithIssue
class Divzero {
  void f() {
    int j = 1 / 0; // NoIssue
  }
}

@SuppressWarnings("java:S3518") // WithIssue
class Divzero2 {
  void f() {
    int j = 1 / 0; // NoIssue
  }
}

