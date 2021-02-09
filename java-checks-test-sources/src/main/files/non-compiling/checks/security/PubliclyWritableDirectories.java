package checks.security;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

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
    
    
  }
}
