package org.sonar.java.ecj;

@Deprecated
class NotImplementedException extends UnsupportedOperationException {

  NotImplementedException() {
  }

  public NotImplementedException(String msg) {
    super(msg);
  }
}
