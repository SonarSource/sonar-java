#!/usr/bin/env python3

import os
import sys
import subprocess

def main():
  if len(sys.argv) < 3:
    print("Usage: generate-rule-stubs.py <rule-key> <check-name> [rspec-branch]")
    print("  For any RSPEC to be generated, you need to provide the path to the rule-api jar file in the RULE_API_JAR environment variable.")
    sys.exit(1)

  javaChecksModulePath = os.path.dirname(os.path.realpath(__file__))
  ruleKey = sys.argv[1]
  checkName = sys.argv[2]

  if checkName.endswith(".java"):
    print("ERROR: Do not append \".java\" to check name")
    sys.exit(2)
  elif not checkName.endswith("Check"):
    print("INFO: Appending \"Check\" to check name")
    checkName = checkName + "Check"

  checkName = checkName.replace(".", "/")

  checkPath = javaChecksModulePath + "/src/main/java/org/sonar/java/checks/" + checkName + ".java"
  testPath = javaChecksModulePath + "/src/test/java/org/sonar/java/checks/" + checkName + "Test.java"
  samplePath = javaChecksModulePath + "/../java-checks-test-sources/src/main/java/checks/" + checkName + "Sample.java"
  samplePathForTest = "checks/" + checkName + "Sample.java"

  checkName = checkName.replace("/", ".")
  package = "org.sonar.java.checks"
  subpackag = ".".join(checkName.split(".")[0:-1])
  if subpackag != "":
    package = package + "." + subpackag
  checkName = checkName.split(".")[-1]

  if (os.path.exists(checkPath) or os.path.exists(testPath) or os.path.exists(samplePath)):
    print("ERROR: Check already exists")
    sys.exit(3)

  print("INFO: Generating stubs for rule \"" + ruleKey + "\" with check name \"" + checkName + "\"...")

  with open(checkPath, "w") as checkFile:
    checkFile.write("package " + package + ";\n\n")
    checkFile.write("import org.sonar.check.Rule;\n")
    checkFile.write("import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;\n\n")
    checkFile.write("@Rule(key = \"" + ruleKey + "\")\n")
    checkFile.write("public class " + checkName + " extends IssuableSubscriptionVisitor {\n\n")
    checkFile.write("  // TODO: Implement the check\n\n")
    checkFile.write("}\n")

  with open(testPath, "w") as testFile:
    testFile.write("package " + package + ";\n\n")
    testFile.write("import org.junit.jupiter.api.Test;\n")
    testFile.write("import org.sonar.java.checks.verifier.TestUtils;\n")
    testFile.write("import org.sonar.java.checks.verifier.CheckVerifier;\n\n")
    testFile.write("class " + checkName + "Test {\n\n")
    testFile.write("  @Test\n")
    testFile.write("  void test() {\n")
    testFile.write("    CheckVerifier.newVerifier()\n")
    testFile.write("      .onFile(TestUtils.mainCodeSourcesPath(\"" + samplePathForTest + "\"))\n")
    testFile.write("      .withCheck(new " + checkName + "())\n")
    testFile.write("      .verifyIssues();\n")
    testFile.write("  }\n\n")
    testFile.write("}\n")

  with open(samplePath, "w") as sampleFile:
    sampleFile.write("package " + package[15:] + ";\n\n")
    sampleFile.write("class " + checkName + "Sample {\n\n")
    sampleFile.write("  // TODO: Implement the sample class\n\n")
    sampleFile.write("}\n")

  # Add to CheckList
  checkListPath = javaChecksModulePath + "/src/main/java/org/sonar/java/checks/CheckList.java"
  with open(checkListPath, "r") as checkListFile:
    checkListLines = checkListFile.readlines()

  with open(checkListPath, "w") as checkListFile:
    newCheckListLines = []

    # Import
    while not (checkListLines[0].startswith("import")):
      newCheckListLines.append(checkListLines.pop(0))

    newCheckImportLine = "import " + package + "." + checkName + ";\n"
    while checkListLines[0].startswith("import") and checkListLines[0] < newCheckImportLine:
      newCheckListLines.append(checkListLines.pop(0))

    newCheckListLines.append(newCheckImportLine)

    while not (newCheckListLines[-1].__contains__("// IssuableSubscriptionVisitor")):
      newCheckListLines.append(checkListLines.pop(0))

    checkClassName = checkName + ".class"
    while checkListLines[0].__contains__(".class") and checkListLines[0].strip() < checkClassName:
      newCheckListLines.append(checkListLines.pop(0))

    newCheckListLines.append("    " + checkClassName + ",\n")

    for line in checkListLines:
      newCheckListLines.append(line)

    checkListFile.seek(0)
    checkListFile.writelines(newCheckListLines)

  # license headers
  runPath = javaChecksModulePath + "/.."
  subprocess.run(["mvn", "license:format"], cwd = runPath)
  subprocess.run(["git", "add", checkPath, testPath, samplePath], cwd = runPath)

  # rule api
  # get rule api binary from env variable
  ruleApiBinary = os.environ.get("RULE_API_JAR")
  if ruleApiBinary is not None:
    command = ["java", "-jar", ruleApiBinary, "generate", "-rule", ruleKey]
    if len(sys.argv) >= 4:
      rspecBranch = sys.argv[3]
      command += ["-branch", rspecBranch]
    subprocess.run(command, cwd = runPath)

main()
