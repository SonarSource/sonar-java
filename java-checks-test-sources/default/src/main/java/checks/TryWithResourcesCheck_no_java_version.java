package checks;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

class TryWithResourcesCheck_no_java_version {
  String foo(String fileName) {
    FileReader fr = null;
    BufferedReader br = null;
    try { // Noncompliant {{Change this "try" to a try-with-resources. (sonar.java.source not set. Assuming 7 or greater.)}}
//  ^^^
      fr = new FileReader(fileName);
//         ^^^^^^^^^^^^^^^^^^^^^^^^<
      br = new BufferedReader(fr);
//         ^^^^^^^^^^^^^^^^^^^^^^<
      return br.readLine();
    } catch (Exception e) {
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch(IOException e){}
      }
      if (fr != null ) {
        try {
          br.close();
        } catch(IOException e){}
      }
    }
    try (
        FileReader fr2 = new FileReader(fileName);
        BufferedReader br2 = new BufferedReader(fr)
    ) { //compliant
      return br.readLine();
    }
    catch (Exception e) {}

    return null;
  }
}
