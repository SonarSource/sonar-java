package foo;

import java.util.List;
import java.util.ArrayList;

public class Main {

  public static void main(String[] args) {
    List<String> list = new ArrayList<>();
    list.add("Hello");
    list.add("World");
    
    String unused;

    for (String word : list) {
      if (word.isEmpty()) {
        System.out.println("empty..");
      }
    }
  }

}
