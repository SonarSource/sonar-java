package annotations.nullability.no_default;

import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

class ExternalGenericParent<T> {

  T inherited(T value) {
    return value;
  }
}

public class ExternalGenericNullability<T> extends ExternalGenericParent<T> {

  public ExternalGenericNullability(T value) {
  }

  @NonNull
  public T declaredNonNull(@NonNull T value) {
    return value;
  }

  @Nullable
  public T declaredNullable(@Nullable T value) {
    return value;
  }

  public List<@Nullable T> nestedNullable(List<@Nullable T> values) {
    return values;
  }

  public <U> U genericMethod(U value) {
    return value;
  }

  public <U> U genericVarargs(U... values) {
    return values[0];
  }

  @org.eclipse.jdt.annotation.NonNullByDefault
  public T defaulted(T value) {
    return value;
  }
}
