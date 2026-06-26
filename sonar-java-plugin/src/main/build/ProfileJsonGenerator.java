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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProfileJsonGenerator {

  private static final String PROFILES_RELATIVE_PATH = "src/main/resources/profiles";

  private static final Map<String, String> PROFILES = Map.of(
    "sonar_way", "Sonar way",
    "sonar_agentic_ai", "Sonar agentic AI"
  );

  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      throw new IllegalArgumentException("Expected module and output directories as arguments.");
    }

    Path profilesDirectory = Path.of(args[0]).resolve(PROFILES_RELATIVE_PATH);
    Path outputDirectory = Path.of(args[1]).resolve("org/sonar/l10n/java/rules/java");

    Files.createDirectories(outputDirectory);

    for (Map.Entry<String, String> profile : PROFILES.entrySet()) {
      String profileKey = profile.getKey();
      String profileName = profile.getValue();
      Path profileDir = profilesDirectory.resolve(profileKey);

      if (!Files.exists(profileDir)) {
        throw new IllegalStateException("Profile directory does not exist: " + profileDir);
      }

      List<String> ruleKeys = collectRuleKeys(profileDir);
      String outputFileName = profileName.replace(" ", "_") + "_profile.json";
      writeProfile(outputDirectory.resolve(outputFileName), profileName, ruleKeys);
    }
  }

  private static List<String> collectRuleKeys(Path profileDirectory) throws IOException {
    try (Stream<Path> files = Files.list(profileDirectory)) {
      return files
        .filter(Files::isRegularFile)
        .map(Path::getFileName)
        .map(Path::toString)
        .filter(ProfileJsonGenerator::isValidRuleKey)
        .sorted(Comparator.comparingInt(ProfileJsonGenerator::numericKey))
        .collect(Collectors.toList());
    }
  }

  private static boolean isValidRuleKey(String ruleKey) {
    // Rule keys should start with 'S' followed by digits
    return ruleKey.matches("S\\d+");
  }

  private static int numericKey(String ruleKey) {
    // Extract numeric part from rule key (e.g., "S100" -> 100)
    return Integer.parseInt(ruleKey.substring(1));
  }

  private static void writeProfile(Path outputFile, String profileName, List<String> ruleKeys) throws IOException {
    String renderedRuleKeys = ruleKeys.stream()
      .map("    \"%s\""::formatted)
      .collect(Collectors.joining(",\n"));

    String content = """
      {
        "name": "%s",
        "ruleKeys": [
      %s
        ]
      }
      """.formatted(profileName, renderedRuleKeys);

    Files.writeString(outputFile, content, StandardCharsets.UTF_8);
  }
}
