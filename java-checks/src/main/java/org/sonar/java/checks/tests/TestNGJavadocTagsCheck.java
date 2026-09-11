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
package org.sonar.java.checks.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.collections.MapBuilder;

@Rule(key = "S9387")
public class TestNGJavadocTagsCheck extends IssuableSubscriptionVisitor {

  private static final Pattern BLOCK_TAG_PATTERN = Pattern.compile("^\\s*\\*?\\s*@(\\w+)", Pattern.MULTILINE);

  private static final Map<String, String> TESTNG_TAGS_TO_ANNOTATIONS = MapBuilder.<String, String>newMap()
    .put("test", "@Test")
    .put("beforemethod", "@BeforeMethod")
    .put("aftermethod", "@AfterMethod")
    .put("beforeclass", "@BeforeClass")
    .put("afterclass", "@AfterClass")
    .put("beforesuite", "@BeforeSuite")
    .put("aftersuite", "@AfterSuite")
    .put("beforetest", "@BeforeTest")
    .put("aftertest", "@AfterTest")
    .put("beforegroups", "@BeforeGroups")
    .put("aftergroups", "@AfterGroups")
    .put("dataprovider", "@DataProvider")
    .put("factory", "@Factory")
    .put("parameters", "@Parameters")
    .put("listeners", "@Listeners")
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    tree.firstToken().trivias().stream()
      .filter(trivia -> trivia.isComment(SyntaxTrivia.CommentKind.JAVADOC))
      .forEach(trivia -> checkJavadoc(tree, trivia));
  }

  private void checkJavadoc(Tree tree, SyntaxTrivia trivia) {
    String commentText = trivia.comment();
    Matcher matcher = BLOCK_TAG_PATTERN.matcher(commentText);

    String firstTagOriginal = null;
    String firstAnnotation = null;
    List<JavaFileScannerContext.Location> secondaryLocations = new ArrayList<>();

    while (matcher.find()) {
      String tagName = matcher.group(1);
      String annotation = TESTNG_TAGS_TO_ANNOTATIONS.get(tagName.toLowerCase(Locale.ROOT));
      if (annotation != null) {
        if (firstTagOriginal == null) {
          firstTagOriginal = tagName;
          firstAnnotation = annotation;
        } else {
          secondaryLocations.add(new JavaFileScannerContext.Location(
            String.format("Also replace \"@%s\" with the TestNG \"%s\" annotation.",
              tagName, TESTNG_TAGS_TO_ANNOTATIONS.get(tagName.toLowerCase(Locale.ROOT))),
            tree));
        }
      }
    }

    if (firstTagOriginal != null) {
      String message = String.format("Replace this \"@%s\" Javadoc tag with the TestNG \"%s\" annotation.", firstTagOriginal, firstAnnotation);
      if (secondaryLocations.isEmpty()) {
        reportIssue(tree, message);
      } else {
        reportIssue(tree, message, secondaryLocations, null);
      }
    }
  }
}
