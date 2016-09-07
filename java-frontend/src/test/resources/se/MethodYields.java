import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;

import java.io.File;

class MethodYields {
  public boolean method(Object a, boolean b) {
    boolean result = true;
    if(a != null) {
      if (b == true) {
        result = false;
      } else {
        return b;
      }
    }
    return result;
  }


  private static List<String> readFile(File file) {
    try {
      return FileUtils.readLines(file);
    } catch (IOException e) {
      fail("can not read test file");
    }
    return Lists.newArrayList();
  }
}
