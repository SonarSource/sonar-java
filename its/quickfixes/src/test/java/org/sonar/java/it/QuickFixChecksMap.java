package org.sonar.java.it;

import java.util.HashMap;
import java.util.Map;
import org.sonar.plugins.java.api.JavaCheck;

public class QuickFixChecksMap {

  private static final String DEFAULT = "default/src/main/java/";

  public static final Map<JavaCheck, String> CHECKS_TO_SAMPLE_FILE_MAP = new HashMap<>(){{
    put(new org.sonar.java.checks.CombineCatchCheck(), DEFAULT+"checks/CombineCatchCheck_no_version.java");
  }};

}
