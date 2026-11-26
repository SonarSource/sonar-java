---
title: Writing Custom Java Rules 101
description: Find the custom Java rules tutorial and follow links from its original section bookmarks.
generated:
  by: tirion
---

# Writing Custom Java Rules 101

The tutorial now has a page for each step, from setting up the example Maven project to deploying a custom rule. Start at the [tutorial index](custom-rules/index.md), or follow the links below if you arrived via a section bookmark.

## Content

- [Getting started](custom-rules/getting-started.md)
- [Writing a rule](custom-rules/writing-a-rule.md)
- [Implementing the rule](custom-rules/implementing-the-rule.md)
- [Registering the rule in the custom plugin](custom-rules/registering-the-rule.md)
- [Testing a custom plugin](custom-rules/testing-a-custom-plugin.md)
- [References](custom-rules/index.md#references)

## Getting started

[Set up the custom Java rules example project.](custom-rules/getting-started.md)

### Looking at the POM

[Choose a Maven POM, install the example, and configure the plugin.](custom-rules/getting-started.md#looking-at-the-pom)

## Writing a rule

[Start writing a custom Java rule.](custom-rules/writing-a-rule.md)

### Three files to forge a rule

[Create the three files you need to write and test the rule.](custom-rules/writing-a-rule.md#three-files-to-forge-a-rule)

### A specification to make it right

[Read the rule specification.](custom-rules/writing-a-rule.md#a-specification-to-make-it-right)

### A test file to rule them all

[Write examples and mark the lines where the rule should report issues.](custom-rules/writing-a-rule.md#a-test-file-to-rule-them-all)

### A test class to make it pass

[Run the rule against the examples using `CheckVerifier`.](custom-rules/writing-a-rule.md#a-test-class-to-make-it-pass)

### First version: Using syntax trees and API basics

[Use a syntax tree visitor to implement the first version of the rule.](custom-rules/implementing-the-rule.md#first-version-using-syntax-trees-and-api-basics)

### Second version: Using semantic API

[Use the semantic API to compare types and finish the implementation.](custom-rules/implementing-the-rule.md#second-version-using-semantic-api)

### What you can use, and what you can't

[Check which parts of the Java Analyzer API are available to custom rules.](custom-rules/implementing-the-rule.md#what-you-can-use-and-what-you-cant)

## Registering the rule in the custom plugin

[Register the rule so SonarQube can list and run it.](custom-rules/registering-the-rule.md)

### Rule Metadata

[Add rule metadata and a description.](custom-rules/registering-the-rule.md#rule-metadata)

### Rule Activation

[Add the rule to the plugin's list of Java checks.](custom-rules/registering-the-rule.md#rule-activation)

### Rule Registrar

[Review the registrar and update its test.](custom-rules/registering-the-rule.md#rule-registrar)

### Rules repository

[Customize the rule repository name and key.](custom-rules/registering-the-rule.md#rules-repository)

## Testing a custom plugin

[Deploy the custom plugin to SonarQube and activate the rule.](custom-rules/testing-a-custom-plugin.md)

### How to define rule parameters

[Define configurable parameters for a rule.](custom-rules/testing-a-custom-plugin.md#how-to-define-rule-parameters)

### How to test sources requiring external binaries

[Add external libraries to the test classpath.](custom-rules/testing-a-custom-plugin.md#how-to-test-sources-requiring-external-binaries)

### How to test precise issue location

[Assert an issue's exact location in a test sample.](custom-rules/testing-a-custom-plugin.md#how-to-test-precise-issue-location)

### How to test the Source Version in a rule

[Set the Java source version in a rule test.](custom-rules/testing-a-custom-plugin.md#how-to-test-the-source-version-in-a-rule)

## References

[Find related documentation and example code.](custom-rules/index.md#references)
