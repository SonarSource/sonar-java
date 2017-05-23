import org.apache.commons.lang.Validate;

import java.util.List;

class CommonsLangValidate {
  void fun() {
    List<Object> objects = null;
    Validate.notEmpty(objects); // throw on null collection
    objects.size();
  }

  void fun() {
    List<Object> objects = null;
    org.apache.commons.lang3.Validate.notEmpty(objects); // throw on null collection
    objects.size();
  }
  void checkNotNull(@javax.annotation.Nullable Object param) {
    Validate.notNull(param);
    param.toString();
  }
  void checkNotNull(@javax.annotation.Nullable Object param) {
    org.apache.commons.lang3.Validate.notNull(param);
    param.toString();
  }
  void checkNotNull(@javax.annotation.Nullable Object param) {
    org.apache.commons.lang3.Validate.notNull(param, "some message %s", "format");
    param.toString();
  }
  void fun() {
    Validate.validState(1<2);
  }
}
