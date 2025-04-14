/*
 * Header
 */
import com.google.common.annotations.Beta;

/**
 * Javadoc
 */
@Beta
abstract class module {
  String open, transitive;

  void requires(Object exports, Object opens) {
    int to;
    double with;
    String uses;
    provides();
  }

  /// Markedown javadoc begin
  /// Markedown javadoc end
  abstract void provides();
}
