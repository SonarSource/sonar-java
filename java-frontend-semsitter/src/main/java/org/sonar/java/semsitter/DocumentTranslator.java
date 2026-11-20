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
package org.sonar.java.semsitter;

import com.google.gson.JsonObject;
import java.util.Set;
import java.util.function.Function;
import org.sonar.java.model.JParseResult;
import org.sonarsource.semsitter.wire.Document;

/**
 * Turns the document a SemSitter server answered for one file into the tree the checks run on. The loaders give the
 * declarations of the named types and whether a package exists, as the server answers them while the session is open.
 */
@FunctionalInterface
public interface DocumentTranslator {

  JParseResult translate(Document document, Function<Set<String>, JsonObject> types, Function<String, Boolean> packages, boolean noLibraries,
    int javaVersion);
}
