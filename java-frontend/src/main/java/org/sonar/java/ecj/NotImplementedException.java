package org.sonar.java.ecj;

@Deprecated
class NotImplementedException extends UnsupportedOperationException {

  NotImplementedException() {
  }

  NotImplementedException(String msg) {
    super(msg);
  }
}
