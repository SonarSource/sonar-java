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

  abstract void provides();
}
