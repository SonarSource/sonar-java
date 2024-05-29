package symbolicexecution.behaviorcache;

import java.util.List;
import org.apache.commons.lang.Validate;

class CommonsLangValidate {
  void foo() {
    List<Object> objects = null;
    Validate.notEmpty(objects); // throw on null collection
    objects.size();
  }

  void bar() {
    List<Object> objects = null;
    org.apache.commons.lang3.Validate.notEmpty(objects); // throw on null collection
    objects.size();
  }
  void checkNotNull1(@javax.annotation.Nullable Object param) {
    Validate.notNull(param);
    param.toString();
  }
  void checkNotNull2(@javax.annotation.Nullable Object param) {
    org.apache.commons.lang3.Validate.notNull(param);
    param.toString();
  }
  void checkNotNull3(@javax.annotation.Nullable Object param) {
    org.apache.commons.lang3.Validate.notNull(param, "some message %s", "format");
    param.toString();
  }
}
