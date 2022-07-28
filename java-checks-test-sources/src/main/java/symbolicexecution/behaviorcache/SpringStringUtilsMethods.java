package symbolicexecution.behaviorcache;

import org.springframework.util.StringUtils;

class SpringStringUtilsMethods {

  String test_isEmpty() {
    String fileName = null;
    if (!StringUtils.isEmpty(fileName)) {
      return fileName.substring(1);  // FP, fileName cannot be null when isEmpty is false
    }
    return fileName;
  }
}



