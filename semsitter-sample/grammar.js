/// <reference types="tree-sitter-cli/dsl" />

module.exports = grammar({
  name: "sample",
  rules: {
    source_file: ($) => "hello",
  },
});
