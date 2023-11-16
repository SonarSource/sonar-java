package symbolicexecution.behaviorcache;

import com.google.common.base.Verify;

class GuavaVerifyMethods {

  void verify(@javax.annotation.Nullable Object param) {
    Verify.verify(param != null); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate) {
    Verify.verify(param != null, errorMessageTemplate); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, char p1) {
    Verify.verify(param != null, errorMessageTemplate, p1); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, char p1, char p2) {
    Verify.verify(param != null, errorMessageTemplate, p1, p2); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, char p1, int p2) {
    Verify.verify(param != null, errorMessageTemplate, p1, p2); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, char p1, long p2) {
    Verify.verify(param != null, errorMessageTemplate, p1, p2); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, char p1, Object p2) {
    Verify.verify(param != null, errorMessageTemplate, p1, p2); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, int p1) {
    Verify.verify(param != null, errorMessageTemplate, p1); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, int p1, char p2) {
    Verify.verify(param != null, errorMessageTemplate, p1, p2); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, int p1, int p2) {
    Verify.verify(param != null, errorMessageTemplate, p1, p2); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, int p1, long p2) {
    Verify.verify(param != null, errorMessageTemplate, p1, p2); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, int p1, Object p2) {
    Verify.verify(param != null, errorMessageTemplate, p1, p2); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, long p1) {
    Verify.verify(param != null, errorMessageTemplate, p1); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, long p1, char p2) {
    Verify.verify(param != null, errorMessageTemplate, p1, p2); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, long p1, int p2) {
    Verify.verify(param != null, errorMessageTemplate, p1, p2); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, long p1, long p2) {
    Verify.verify(param != null, errorMessageTemplate, p1, p2); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, long p1, Object p2) {
    Verify.verify(param != null, errorMessageTemplate, p1, p2); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, Object p1) {
    Verify.verify(param != null, errorMessageTemplate, p1); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, Object p1, char p2) {
    Verify.verify(param != null, errorMessageTemplate, p1, p2); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, Object p1, int p2) {
    Verify.verify(param != null, errorMessageTemplate, p1, p2); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, Object p1, long p2) {
    Verify.verify(param != null, errorMessageTemplate, p1, p2); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, Object p1, Object p2) {
    Verify.verify(param != null, errorMessageTemplate, p1, p2); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, Object p1, Object p2, Object p3) {
    Verify.verify(param != null, errorMessageTemplate, p1, p2, p3); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, Object p1, Object p2, Object p3, Object p4) {
    Verify.verify(param != null, errorMessageTemplate, p1, p2, p3, p4); param.toString();
  }

  void verifyWithMessage(@javax.annotation.Nullable Object param, String errorMessageTemplate, Object... ps) {
    Verify.verify(param != null, errorMessageTemplate, ps); param.toString();
  }

  void verifyNotNull(@javax.annotation.Nullable Object param) {
    Verify.verifyNotNull(param); param.toString();
  }

  void verifyNotNullMessage(@javax.annotation.Nullable Object param) {
    Verify.verifyNotNull(param, "Message"); param.toString();
  }
}
