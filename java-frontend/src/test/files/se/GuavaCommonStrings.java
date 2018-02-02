import com.google.common.base.Strings;
import javax.annotation.Nullable;

class GoogleCommonStrings {

  void isNullOrEmpty(@Nullable String a) {
    if (!Strings.isNullOrEmpty(a)) {
      a.length(); // Compliant
    }
  }
}
