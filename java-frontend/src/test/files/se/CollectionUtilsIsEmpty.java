import org.apache.commons.collections.CollectionUtils;
import java.util.List;

class CollectionUtilsIsEmpty {
  void fun() {
    List<Object> objects = null;
    if (CollectionUtils.isEmpty(objects)) { // returns true if objects is null
      doSomething();
    } else if (objects.size() == 1) {
      doSomethingElse();
    }
  }
  void fun2() {
    List<Object> objects = null;
    if (org.apache.commons.collections4.CollectionUtils.isEmpty(objects)) { // returns true if objects is null
      doSomething();
    } else if (objects.size() == 1) {
      doSomethingElse();
    }
  }
  void fun3() {
    List<Object> objects = null;
    if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(objects)) { // returns false if objects is null
      objects.size();
    } else {
      doSomethingElse();
    }
  }
  void fun4() {
    CollectionUtils.emptyCollection();
    List<Object> objects = null;
    if (CollectionUtils.isNotEmpty(objects)) { // returns false if objects is null
      objects.size();
    } else {
      doSomethingElse();
    }
  }
}
