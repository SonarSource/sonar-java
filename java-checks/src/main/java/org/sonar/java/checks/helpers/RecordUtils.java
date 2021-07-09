package org.sonar.java.checks.helpers;

import java.util.List;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.VariableTree;

public class RecordUtils {
  private RecordUtils() {
  }

  public static boolean isACanonicalConstructor(MethodTree method) {
    // Check if the method is a constructor
    if (!isARecordConstructor(method)) {
      return false;
    }
    // Check if constructor has a throw clause
    if (!method.throwsClauses().isEmpty()) {
      return false;
    }
    ClassTree theRecord = (ClassTree) method.symbol().owner().declaration();
    // Check if the number of parameters matches the number of components
    List<VariableTree> components = theRecord.recordComponents();
    List<VariableTree> parameters = method.parameters();
    if (components.size() != parameters.size()) {
      return false;
    }
    // Check if components and parameters are ordered int the same way
    for (int i = 0; i < components.size(); i++) {
      VariableTree component = components.get(i);
      VariableTree parameter = parameters.get(i);
      if (!component.simpleName().name().equals(parameter.simpleName().name()) ||
        !component.symbol().type().equals(parameter.symbol().type())) {
        return false;
      }
    }
    return true;
  }

  public static boolean isACompactConstructor(MethodTree method) {
    return isARecordConstructor(method) &&
      method.openParenToken() == method.closeParenToken() &&
      method.parameters().isEmpty();
  }

  public static boolean isARecordConstructor(MethodTree method) {
    // Check if the method is a constructor
    if (!"<init>".equals(method.symbol().name())) {
      return false;
    }
    // Check if the owner is a record
    return method.symbol().owner().type().isSubtypeOf("java.lang.Record");
  }
}
