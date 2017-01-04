import java.io.*;
import java.util.*;

public class A {

  void ARM() {
    try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("output1.txt"), "UTF-8"))) {  // FP
      writer.write("This is my content.");
    }

    try (Writer writer = new OutputStreamWriter(new FileOutputStream("output1.txt"), "UTF-8")) {  // Compliant
      writer.write("This is my content.");
    }

    try (Writer writer1 = new OutputStreamWriter(new FileOutputStream("output1.txt"), "UTF-8");
      Writer writer2 = new OutputStreamWriter(new FileOutputStream("output1.txt"), "UTF-8");
    ) {  // Compliant
      writer1.write("This is my content.");
      writer2.write("This is my content.");
    }

    try (FileWriter fw = new FileWriter("")) { // Compliant - JLS8 - 14.20.3 : try-with-resources
      fw.write("hello");
    } catch (Exception e) {
      // ...
    }
  }
}
