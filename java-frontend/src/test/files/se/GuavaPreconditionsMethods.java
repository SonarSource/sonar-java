import com.google.common.base.Preconditions;

class GuavaPreconditionsMethods {
  void checkArgument(@javax.annotation.Nullable Object param) {
    Preconditions.checkArgument(param != null);
    param.toString();
  }
  void checkNotNull(@javax.annotation.Nullable Object param) {
    Preconditions.checkNotNull(param);
    param.toString();
  }
  void checkState(@javax.annotation.Nullable Object param) {
    Preconditions.checkState(param != null);
    param.toString();
  }
}
