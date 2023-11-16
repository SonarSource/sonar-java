package symbolicexecution.behaviorcache;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.ListUtils;

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
    List<Object> objects = null;
    if (CollectionUtils.isNotEmpty(objects)) { // returns false if objects is null
      objects.size();
    } else {
      doSomethingElse();
    }
  }

  void test_map(@Nullable Map<String, String> map) {
    if (!org.apache.commons.collections4.MapUtils.isEmpty(map)) { // returns true if objects is null
      map.clear();
    }
    if (org.apache.commons.collections4.MapUtils.isNotEmpty(map)) { // returns false if objects is null
      map.clear();
    }
  }

  void test_iterator(@Nullable Iterator<?> iterator) {
    if (!org.apache.commons.collections4.IteratorUtils.isEmpty(iterator)) { // returns true if objects is null
      iterator.next();
    }
  }

  void test_iterable(@Nullable Iterable<?> iterable) {
    if (!org.apache.commons.collections4.IterableUtils.isEmpty(iterable)) { // returns true if objects is null
      iterable.iterator().next();
    }
  }

  void test_list(@Nullable List<String> list, List<String> defaultValue) {
    List<String> listA = ListUtils.defaultIfNull(new ArrayList<>(), null);
    listA.clear();

    List<String> listB = ListUtils.defaultIfNull(null, new ArrayList<>());
    listB.clear();

    List<String> listC = ListUtils.defaultIfNull(null, null);
    // listC is null and usage would raise S2259

    List<String> listD = ListUtils.defaultIfNull(list, defaultValue);
    listD.clear();
  }

  void doSomething() { }
  void doSomethingElse() { }
}
