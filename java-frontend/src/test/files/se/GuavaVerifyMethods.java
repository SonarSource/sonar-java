import com.google.common.base.Verify;

class GuavaVerifyMethods {

  void verifyBoolean(@javax.annotation.Nullable Object param) {
    Verify.verify(param != null);
    param.toString();
  }

  void verifyBooleanWithMessage(@javax.annotation.Nullable Object param) {
    Verify.verify(param != null, "Message");
    param.toString();
  }

  void verifyNotNull(@javax.annotation.Nullable Object param) {
    Verify.verifyNotNull(param);
    param.toString();
  }

  void verifyNotNullMessage(@javax.annotation.Nullable Object param) {
    Verify.verifyNotNull(param, "Message");
    param.toString();
  }
}
