package org.sonar.java.ecj;

/**
 * Indicates cases with lower priority than {@link NotImplementedException}
 */
@Deprecated
class UnexpectedAccessException extends UnsupportedOperationException {

  UnexpectedAccessException() {
  }

  public UnexpectedAccessException(String msg) {
    super(msg);
  }
}
