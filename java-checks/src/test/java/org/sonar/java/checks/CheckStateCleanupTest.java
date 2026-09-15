/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class CheckStateCleanupTest {

  private static final DescribedPredicate<JavaClass> CHECK_CLASSES = new DescribedPredicate<>("check classes") {
    @Override
    public boolean test(JavaClass input) {
      return input.isAnnotatedWith(Rule.class);
    }
  };

  private static final ArchCondition<JavaClass> CLEAR_COLLECTION_FIELDS = new ArchCondition<>("clear collection fields at file boundaries") {
    @Override
    public void check(JavaClass checkClass, ConditionEvents events) {
      Set<JavaField> collectionFields = checkClass.getFields().stream()
        .filter(field -> !Modifier.isStatic(field.reflect().getModifiers()))
        .filter(CheckStateCleanupTest::isCollectionField)
        .filter(field -> !isPropertyCache(field))
        .collect(java.util.stream.Collectors.toSet());
      Set<JavaMethod> lifecycleMethods = lifecycleMethods(checkClass);
      for (JavaField field : collectionFields) {
        if (lifecycleMethods.isEmpty()) {
          events.add(SimpleConditionEvent.violated(checkClass,
            checkClass.getFullName() + " has collection state but no lifecycle method"));
        } else {
          for (JavaMethod lifecycleMethod : lifecycleMethods) {
            if (!clearsField(lifecycleMethod, field, checkClass, new HashSet<>())) {
              events.add(SimpleConditionEvent.violated(checkClass,
                checkClass.getFullName() + " does not clear " + field.getName() + " in " + lifecycleMethod.getName()));
            }
          }
        }
      }
    }
  };

  @Test
  void collection_fields_are_cleared_at_file_boundaries() {
    classes().that(CHECK_CLASSES).should(CLEAR_COLLECTION_FIELDS).check(new ClassFileImporter()
      .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
      .importPackages("org.sonar.java.checks"));
  }

  private static boolean isCollectionField(JavaField field) {
    return field.getRawType().isAssignableTo(Collection.class) || field.getRawType().isAssignableTo(Map.class);
  }

  private static boolean isPropertyCache(JavaField field) {
    var valueTypes = field.getAllInvolvedRawTypes().stream()
      .filter(type -> !type.isAssignableTo(Collection.class) && !type.isAssignableTo(Map.class))
      .toList();
    return !valueTypes.isEmpty() && valueTypes.stream().allMatch(CheckStateCleanupTest::isPropertyType);
  }

  private static boolean isPropertyType(JavaClass type) {
    String name = type.getName();
    return name.equals(String.class.getName())
      || name.equals("java.math.BigDecimal")
      || name.equals("java.util.regex.Pattern")
      || name.equals("org.sonar.api.utils.WildcardPattern")
      || name.equals("java.lang.Boolean")
      || name.endsWith("PrimitiveCheck")
      || type.isAssignableTo(Number.class);
  }

  private static Set<JavaMethod> lifecycleMethods(JavaClass checkClass) {
    if (checkClass.isAssignableTo(EndOfAnalysis.class)) {
      return methodsNamed(checkClass, "endOfAnalysis", ModuleScannerContext.class);
    }
    if (checkClass.isAssignableTo(IssuableSubscriptionVisitor.class)) {
      Set<JavaMethod> methods = methodsNamed(checkClass, "setContext", JavaFileScannerContext.class);
      methods.addAll(methodsNamed(checkClass, "leaveFile", JavaFileScannerContext.class));
      return methods;
    }
    return methodsNamed(checkClass, "scanFile", JavaFileScannerContext.class);
  }

  private static Set<JavaMethod> methodsNamed(JavaClass checkClass, String name, Class<?> parameterType) {
    Set<JavaMethod> methods = new HashSet<>();
    for (JavaMethod method : checkClass.getMethods()) {
      if (method.getName().equals(name)
        && method.getRawParameterTypes().size() == 1
        && method.getRawParameterTypes().get(0).isEquivalentTo(parameterType)) {
        methods.add(method);
      }
    }
    return methods;
  }

  private static boolean clearsField(JavaCodeUnit method, JavaField field, JavaClass checkClass, Set<JavaCodeUnit> visited) {
    if (!visited.add(method)) {
      return false;
    }
    boolean clearsField = method.getMethodCallsFromSelf().stream()
      .filter(CheckStateCleanupTest::isClearCall)
      .anyMatch(call -> field.getAccessesToSelf().stream()
        .anyMatch(access -> access.getOrigin().equals(method) && access.getLineNumber() == call.getLineNumber()));
    boolean resetsField = field.getAccessesToSelf().stream()
      .anyMatch(access -> access.getOrigin().equals(method) && access.getAccessType() == JavaFieldAccess.AccessType.SET);
    if (clearsField || resetsField) {
      return true;
    }
    return method.getMethodCallsFromSelf().stream()
      .map(JavaMethodCall::getTarget)
      .map(target -> target.resolveMember().orElse(null))
      .filter(javaMethod -> javaMethod != null && javaMethod.getOwner().equals(checkClass))
      .anyMatch(calledMethod -> clearsField(calledMethod, field, checkClass, visited));
  }

  private static boolean isClearCall(JavaMethodCall call) {
    return call.getName().equals("clear")
      && (call.getTargetOwner().isAssignableTo(Collection.class) || call.getTargetOwner().isAssignableTo(Map.class));
  }
}
