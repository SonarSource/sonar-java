package symbolicexecution.behaviorcache;

import org.springframework.util.StringUtils;

class SpringStringUtilsMethods {
  String test_isEmpty() {
    String fileName = null;
    if (!StringUtils.isEmpty(fileName)) {
      return fileName.substring(1);
    }
    return fileName;
  }

  String test_string_hasLength() {
    String fileName = null;
    if (StringUtils.hasLength(fileName)) {
      return fileName.substring(1);
    }
    return fileName;
  }

  CharSequence test_chars_hasLength() {
    CharSequence fileName = null;
    if (StringUtils.hasLength(fileName)) {
      return fileName.subSequence(0, 2);
    }
    return fileName;
  }

  String test_string_hasText() {
    String fileName = null;
    if (StringUtils.hasText(fileName)) {
      return fileName.substring(1);
    }
    return fileName;
  }

  CharSequence test_chars_hasText() {
    CharSequence fileName = null;
    if (StringUtils.hasText(fileName)) {
      return fileName.subSequence(0, 2);
    }
    return fileName;
  }
}



