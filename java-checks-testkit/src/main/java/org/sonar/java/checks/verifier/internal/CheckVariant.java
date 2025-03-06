/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.verifier.internal;

import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.JavaCheck;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public final class CheckVariant {

  public static List<List<JavaCheck>> createCheckVariants(List<JavaCheck> initialCombination) {
    List<List<JavaCheck>> allCombinations = new ArrayList<>(List.of(new ArrayList<>()));
    for (JavaCheck checkFromInitialCombination : initialCombination) {
      for (List<JavaCheck> oneCheckCombination : allCombinations) {
        oneCheckCombination.add(checkFromInitialCombination);
      }

      for (var checkVariant : findCheckVariants(checkFromInitialCombination)) {
        for (var checkList : new ArrayList<>(allCombinations)) {
          var newCheckList = new ArrayList<>(checkList);
          newCheckList.set(newCheckList.size() - 1, checkVariant);
          allCombinations.add(newCheckList);
        }
      }
    }
    return allCombinations;
  }

  public static List<JavaCheck> findCheckVariants(JavaCheck check) {
    var props = getProperties(check);

    var variantsDir = new File("../java-checks/src/main/java/org/sonar/java/checks/queryAPI/");
    if (!variantsDir.exists() || !variantsDir.isDirectory()) {
      return List.of();
    }

    var variantFiles = variantsDir.listFiles((dir, name) -> name.startsWith(check.getClass().getSimpleName()));
    if (variantFiles == null) {
      return List.of();
    }

    var variants = Arrays.stream(variantFiles)
      // Get the name without the file extension
      .map(f -> f.getName().substring(0, f.getName().lastIndexOf('.')))
      .toList();

    var result = new ArrayList<JavaCheck>();
    for (var variant : variants) {
      try {
        var clazz = Class.forName("org.sonar.java.checks.queryAPI." + variant)
          .asSubclass(JavaCheck.class);

        var checkVariant = clazz
          .getDeclaredConstructor()
          .newInstance();

        // Set RuleProperty values
        for (var entry : props.entrySet()) {
          var field = clazz.getDeclaredField(entry.getKey());
          field.setAccessible(true);
          field.set(checkVariant, entry.getValue());
        }

        result.add(checkVariant);
      } catch (ClassNotFoundException e) {
        // variant does not exist
      } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException | NoSuchFieldException e) {
        throw new IllegalStateException(e);
      }
    }

    return result;
  }

  private static Map<String, Object> getProperties(JavaCheck check) {
    var fields = Arrays.stream(check.getClass().getDeclaredFields())
      .filter(f -> f.isAnnotationPresent(RuleProperty.class))
      .toList();

    var props = new HashMap<String, Object>();

    for (var f : fields) {
      try {
        f.setAccessible(true);
        var value = f.get(check);
        if (value != null) {
          props.put(f.getName(), value);
        }
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }

    return props;
  }

  public static void addCheckVariantsMessageToExceptions(Runnable validation, List<JavaCheck> combination, int variantCount) {
    if (variantCount == 1) {
      validation.run();
      return;
    }
    try {
      validation.run();
    } catch (RuntimeException e) {
      throw new RuntimeException(e.getClass() + " for checks: " + toClassNameString(combination) + "\n" + e.getMessage(), e);
    } catch (AssertionError e) {
      throw new AssertionError(e.getClass() + " for checks: " + toClassNameString(combination) + "\n" + e.getMessage(), e);
    }
  }

  public static String toClassNameString(List<JavaCheck> combination) {
    return combination.stream()
      .map(JavaCheck::getClass)
      .map(Class::getName)
      .collect(Collectors.joining(", ", "[", "]"));
  }
}
