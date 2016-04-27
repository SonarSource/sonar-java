import java.io.BufferedReader;
import java.io.FileReader;

class A {
  void foo(String fileName) {
    FileReader fr = null;
    BufferedReader br = null;
    try { // Noncompliant [[sc=5;ec=8;secondary=9,10]] {{Change this "try" to a try-with-resources. (sonar.java.source not set. Assuming 7 or greater.)}}
      fr = new FileReader(fileName);
      br = new BufferedReader(fr);
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
        FileReader fr = new FileReader(fileName);
        BufferedReader br = new BufferedReader(fr)
    ) { //compliant
      return br.readLine();
    }
    catch (Exception e) {}


  }
}
