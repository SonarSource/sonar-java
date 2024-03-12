#!/usr/bin/env kotlin

// README: To use this script you need to have kotlin installed.
// You can do this e.g. with `sdk install kotlin <version>`.
// This script has been tested with Kotlin 1.8.20.

import kotlin.io.path.createTempDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.system.exitProcess

if (args.size < 2) {
  println("Usage: generate-rule-stubs.main.kts <rule-key> <check-name> [rspec-branch]")
  println("  For any RSPEC to be generated, you need to provide the path to the rule-api jar file in the RULE_API_JAR environment variable.")
  exitProcess(1)
}

val javaChecksModulePath = __FILE__.absoluteFile.parentFile

val ruleKey = args[0]

val checkNameParts = args[1].let {
  if (it.endsWith(".java")) {
    println("ERROR: Do not append \".java\" to check name")
    exitProcess(2)
  } else if (!it.endsWith("Check")) {
    print("INFO: Appending \"Check\" to check name")
    it + "Check"
  } else {
    it
  }
}.replace('/', '.').replace('\\', '.').split('.')

val checkQualifier = checkNameParts.dropLast(1)
val checkName = checkNameParts.last()

val checkPath = (listOf("src", "main", "java", "org", "sonar", "java", "checks") + checkQualifier + "$checkName.java")
  .fold(javaChecksModulePath) { acc, part ->
    acc.resolve(part)
  }

val testPath = (listOf("src", "test", "java", "org", "sonar", "java", "checks") + checkQualifier + "${checkName}Test.java")
  .fold(javaChecksModulePath) { acc, part ->
    acc.resolve(part)
  }

val samplePath = (listOf("..", "java-checks-test-sources", "default", "src", "main", "java", "checks") + checkQualifier + "${checkName}Sample.java")
  .fold(javaChecksModulePath) { acc, part ->
    acc.resolve(part)
  }

val samplePathForTest = if (checkQualifier.isEmpty()) {
  "checks/${checkName}Sample.java"
} else {
  "checks/" + checkQualifier.joinToString("/") + "/${checkName}Sample.java"
}

val pckg = listOf("org", "sonar", "java", "checks") + checkQualifier

if (checkPath.exists() || testPath.exists() || samplePath.exists()) {
  println("ERROR: Check already exists")
  exitProcess(3)
}

println("INFO: Generating check stubs for $ruleKey with check name $checkName...")

checkPath.writeText(
  """
  package ${pckg.joinToString(".")};
  
  import java.util.List;
  import org.sonar.check.Rule;
  import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
  import org.sonar.plugins.java.api.tree.Tree;

  
  @Rule(key = "$ruleKey")
  public class $checkName extends IssuableSubscriptionVisitor {
  
    @Override
    public List<Tree.Kind> nodesToVisit() {
      // TODO: Specify the kind of nodes you want to be called to visit here.
      return List.of();
    }
  
    @Override
    public void visitNode(Tree tree) {
      throw new UnsupportedOperationException("Not implemented yet");
    }
    
  }
  
""".trimIndent()
)

testPath.writeText(
  """
  package ${pckg.joinToString(".")};
  
  import org.junit.jupiter.api.Test;
  import org.sonar.java.checks.verifier.CheckVerifier;
  import org.sonar.java.checks.verifier.TestUtils;
  
  class ${checkName}Test {
  
    @Test
    void test() {
      CheckVerifier.newVerifier()
        .onFile(TestUtils.mainCodeSourcesPath("$samplePathForTest"))
        .withCheck(new $checkName())
        .verifyIssues();
    }
    
  }
  
""".trimIndent()
)

samplePath.writeText(
  """
  package ${(listOf("checks") + checkQualifier).joinToString(".")};
  
  public class ${checkName}Sample {
    // TODO: Implement the sample class
  }
  
""".trimIndent()
)

// Add check to check list
val checkListPath = listOf("..", "sonar-java-plugin", "src", "main", "java", "org", "sonar", "plugins", "java", "CheckList.java")
  .fold(javaChecksModulePath) { acc, part ->
    acc.resolve(part)
  }

val remainingLines = checkListPath.readLines().toMutableList()
val newLines = mutableListOf<String>()

while (!remainingLines.first().startsWith("import")) newLines.add(remainingLines.removeFirst())

val newCheckImportLine = "import ${pckg.joinToString(".")}.${checkName};"
while (remainingLines.first()
    .startsWith("import") && remainingLines.first().lowercase() < newCheckImportLine.lowercase()
) newLines.add(remainingLines.removeFirst())
newLines.add(newCheckImportLine)

while (newLines.isEmpty() || !newLines.last().contains("// IssuableSubscriptionVisitor")) newLines.add(remainingLines.removeFirst())

val checkListLine = "    $checkName.class,"
while (remainingLines.first().endsWith(".class,") && remainingLines.first().lowercase() < checkListLine.lowercase()) newLines.add(remainingLines.removeFirst())
newLines.add(checkListLine)

newLines.addAll(remainingLines)

checkListPath.writeText(newLines.joinToString("\n", postfix = "\n"))

// License headers using "mvn license:format"
val mvnCmd = if (System.getProperty("os.name").lowercase().startsWith("windows")) {
  "mvn.cmd"
} else {
  "mvn"
}
val runPath = javaChecksModulePath.parentFile

ProcessBuilder(mvnCmd, "license:format")
  .directory(runPath)
  .inheritIO()
  .start()
  .waitFor()

// Add to git
ProcessBuilder("git", "add", checkPath.toString(), testPath.toString(), samplePath.toString())
  .directory(runPath)
  .inheritIO()
  .start()
  .waitFor()

// Rule API
// First download latest version using maven
val tmpDir = createTempDirectory("rules-stubs-gen-script")
ProcessBuilder(mvnCmd, "dependency:copy", "-Dartifact=com.sonarsource.rule-api:rule-api:LATEST", "-DoutputDirectory=$tmpDir")
  .directory(runPath)
  .inheritIO()
  .start()
  .waitFor()


val ruleApiJar = tmpDir.listDirectoryEntries().firstOrNull().let {
  if (it == null || !it.name.endsWith(".jar")) {
    System.err.println("ERROR: Could not download rule-api jar ($it)")
    tmpDir.toFile().deleteRecursively()
    exitProcess(4)
  } else {
    println("INFO: Using rule-api jar $it")
    it
  }
}

val command = arrayOf("java", "-jar", ruleApiJar.toString(), "generate", "-rule", ruleKey).let {
  if (args.size > 2) it + "-branch" + args[2] else it
}

ProcessBuilder(*command)
  .directory(runPath)
  .inheritIO()
  .start()
  .waitFor()

tmpDir.toFile().deleteRecursively()
