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

fn main() {
    let src_dir = std::path::Path::new("src");
    let mut c_config = cc::Build::new();
    c_config.std("c11").include(src_dir);
    c_config.file(src_dir.join("parser.c"));
    println!("cargo:rerun-if-changed=src/parser.c");
    c_config.compile("tree-sitter-sample");
}
