package checks.security;

import java.util.Map;
import java.util.HashMap;
import java.nio.file.Path;

public class PubliclyWritableDirectories {
  private static final String PATH_NAME = "/run/lock";
  
  private Map<String, String> map1;
  private static Map<String, String> map2;
  private final Map<String, String> map3;
  private static final Map<String, String> map4;
  private static final Map<String, String> map5 = new HashMap<>();


  public String compliant(Map<String, String> map) throws IOException {
    map.get("TMP");
    map.get("TMP1");
    
    new HashMap().get("TMP");
    
    map1.get("TMP");
    map2.get("TMP");
    map4.get("TMP");
    map5.get("TMP");
    
    // Since Java 11
    Path.of("\\Windows\\Temp\\my.txt"); // Noncompliant 
    Path.of("\\Windows\\Temp\\", "my.txt"); // Noncompliant 
    Path.of("\\Windows\\Temp\\", "my", "my.txt"); // Noncompliant 
    
  }
}
