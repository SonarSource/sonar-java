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

use tree_sitter_language::LanguageFn;

unsafe extern "C" {
    fn tree_sitter_sample() -> *const ();
}

pub const LANGUAGE: LanguageFn = unsafe { LanguageFn::from_raw(tree_sitter_sample) };

pub fn language() -> tree_sitter::Language {
    LANGUAGE.into()
}

#[cfg(test)]
mod tests {
    use udg_model::location_index::LineRange;
    use udg_model::syntax_unit::{SyntaxSpan, SyntaxUnit};

    use super::language;

    #[test]
    fn sample_rule_parses_hello() {
        let mut parser = tree_sitter::Parser::new();
        parser.set_language(&language()).unwrap();

        let tree = parser.parse("hello", None).unwrap();
        let root = tree.root_node();
        assert_eq!(root.kind(), "source_file");
        assert!(!root.has_error());
        assert_eq!(root.child(0).unwrap().kind(), "hello");

        let unit = SyntaxUnit {
            span: SyntaxSpan {
                syntax_kind: root.kind().to_string(),
                lines: LineRange::point(1),
            },
            nested: Vec::new(),
        };
        assert_eq!(unit.span.syntax_kind, "source_file");
        assert_eq!(unit.span.lines, LineRange::point(1));
    }
}
