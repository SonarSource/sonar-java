package symbolicexecution.behaviorcache;

import javax.annotation.Nullable;
import org.apache.commons.lang3.ArrayUtils;

class StringArrayMethodsLang3 {
  void isEmpty(@Nullable int[] params) {
    if (!ArrayUtils.isEmpty(params)) {
      params[0] = 1; // params is not null
    }
  }

  void isNotEmpty(@Nullable int[] params) {
    if (ArrayUtils.isNotEmpty(params)) {
      params[0] = 1; // params is not null
    }
  }

  int getLength(@Nullable int[] params) {
    int length = ArrayUtils.getLength(params);
    return 42 / length; // FIXME SONARJAVA-4286: FN S3518 (div by zero)
  }

}
