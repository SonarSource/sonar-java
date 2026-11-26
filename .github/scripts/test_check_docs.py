import tempfile
import unittest
from pathlib import Path

from check_docs import CURSOR_RULE, Checker, Repo

HEADER = '---\ntitle: Example\ndescription: A concise description.\ngenerated:\n  by: codex\n'
ROOT_CONTENT = '# Guide\n\n`mise install`\n\n`docs/`\n\n`.agents/skills/new-rule/SKILL.md`\n'
SKILL = '.agents/skills/new-rule/SKILL.md'
BUILD = 'docs/systems/build.md'
ADAPTER = '.claude/skills/new-rule'


class DocsTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.tracked = set()
        self.untracked = set()
        self.write('AGENTS.md', ROOT_CONTENT)
        self.doc('docs/index.md', '`docs/systems/`\n', "okf_version: '0.2'\n")
        self.doc('docs/systems/index.md', f'`{BUILD}`\n')
        self.doc(BUILD, '```bash\nmise exec java@21 -- mvn verify -f component/pom.xml -P sanity\n```\n\n'
                        '```bash\ncd component\nmvn test -f pom.xml -Psanity\n```\n')
        self.write('pom.xml', '<project><artifactId>example</artifactId></project>')
        self.write('component/pom.xml', '''<project xmlns="http://maven.apache.org/POM/4.0.0">
            <artifactId>component</artifactId><profiles><profile><id>sanity</id></profile></profiles>
          </project>''')
        self.write('mise.toml', '[tasks."test:unit"]\nrun = "mvn test"\nalias = "unit"\n')
        self.write(SKILL, '# New rule\n')
        self.write(CURSOR_RULE, f'# Cursor rule\n\n`{SKILL}`\n')
        self.symlink(ADAPTER, '../../.agents/skills/new-rule')
        self.write('README.md', 'This public readme does not require OKF.\n')
        self.write('docs/assets/image.svg', 'SVG\n')
        self.write('docs/example/src/Foo.java', 'class Foo {}\n')

    def write(self, path, contents, tracked=True):
        file = self.root / path
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(contents, encoding='utf-8')
        (self.tracked if tracked else self.untracked).add(path)

    def doc(self, path, body, metadata=''):
        self.write(path, f'{HEADER}{metadata}---\n\n# Example\n\n{body}')

    def symlink(self, path, target):
        link = self.root / path
        link.parent.mkdir(parents=True, exist_ok=True)
        link.symlink_to(target)
        self.tracked.add(path)

    def check(self):
        return Checker(Repo(self.root, self.tracked, self.untracked)).run()

    def assert_errors(self, *messages):
        errors, _ = self.check()
        for message in messages:
            self.assertTrue(any(message in error for error in errors), f'{message!r} not in {errors}')

    def test_clean_repo_and_untracked_docs(self):
        self.write('docs/systems/new.md', HEADER + '---\n\n# New\n', tracked=False)
        self.doc('docs/systems/index.md', f'`{BUILD}` and `docs/systems/new.md`\n')
        self.assertEqual(([], []), self.check())

    def test_path_and_link_failures(self):
        self.doc(BUILD, '''[relative][ref] and [heading](#not-a-heading).

[ref]: missing.md

![image](../assets/missing.svg)

<a href="missing.html">HTML link</a>

`docs/systems/Build.md`, `component/.../pom.xml` and `nonexistent/`.

```text
`ignored/example.java`
```
''')
        self.assert_errors('broken internal link: missing.md', 'unknown Markdown anchor: #not-a-heading',
                           'broken internal link: ../assets/missing.svg', 'broken internal link: missing.html',
                           'unknown repo path: `docs/systems/Build.md`', 'unknown repo path: `component/.../pom.xml`',
                           'unknown repo path: `nonexistent/`')
        self.assertNotIn('ignored/example', '\n'.join(self.check()[0]))

    def test_valid_internal_links_and_duplicate_headings(self):
        self.doc(BUILD, '## Example\n\n<a id="explicit"></a>\n\n'
                        '[Duplicate](#example-1) [Manual](#explicit) [Outside](https://example.com/no-check) '
                        '[Public readme](../../README.md) ![Asset](../assets/image.svg)\n')
        self.assertEqual(([], []), self.check())

    def test_reachability_uses_local_links_and_requires_an_index_for_markdown(self):
        self.doc('docs/index.md', '[Systems](systems/index.md)\n', "okf_version: '0.2'\n")
        self.assertEqual(([], []), self.check())
        self.doc('docs/index.md', '```text\n`docs/systems/index.md`\n```\n', "okf_version: '0.2'\n")
        self.doc('docs/example/README.md', 'Unindexed Maven example.\n')
        self.write('component/AGENTS.md', '# Component guide\n')
        self.assert_errors('docs/systems/build.md: not reachable', 'docs/example: documentation directory is missing index.md',
                           'docs/example/README.md: not reachable', 'component/AGENTS.md: not reachable')

    def test_frontmatter_errors_and_expiry_warning(self):
        self.doc('docs/index.md', '`docs/systems/`\n', 'okf_version: 0.2\nsources:\n  - url: example.org\n')
        self.write(BUILD, '---\ntitle: []\ndescription: false\nstale_after: 2020-01-01\n---\n# Example\n')
        self.assert_errors('okf_version must be the string', 'nonempty resource string', 'title must be a nonempty string',
                           'description must be a nonempty string', 'generated.by must be a nonempty string')
        self.assertEqual([f'{BUILD}: stale_after date has passed: 2020-01-01'], self.check()[1])
        self.write(BUILD, '---\n[malformed:\n---\n')
        self.assert_errors('expected a YAML frontmatter mapping')
        self.doc(BUILD, '', 'stale_after: someday\n')
        self.assert_errors('stale_after must be a YYYY-MM-DD date')

    def test_size_limits_and_sonar_marker_integrity(self):
        block = '<!-- sonar:begin:analysis -->\n' + 'managed\n' * 150 + '<!-- sonar:end:analysis -->\n'
        self.write('AGENTS.md', ROOT_CONTENT + block)
        errors, warnings = self.check()
        self.assertEqual([], errors)
        self.assertIn('total lines including Sonar managed blocks (target 150)', warnings[0])
        self.write('AGENTS.md', ROOT_CONTENT + 'handwritten\n' * 150 + block)
        self.doc(BUILD, 'line\n' * 300)
        self.assert_errors('maximum 150', 'maximum 300')
        self.write('AGENTS.md', ROOT_CONTENT + block.replace('end:analysis', 'end:other'))
        self.assert_errors('unbalanced Sonar managed block marker', 'unclosed Sonar managed block')

    def test_stub_symlink_and_claude_md_failures(self):
        self.write('.cursor/skills/new-rule/SKILL.md', f'Use `{SKILL}`.\n')
        self.assertEqual(([], []), self.check())
        (self.root / ADAPTER).unlink()
        self.symlink(ADAPTER, '../../.agents/skills/nonexistent')
        self.write('.cursor/skills/new-rule/SKILL.md', '# Not a pointer\n')
        self.write(CURSOR_RULE, '# Not a pointer\n')
        self.symlink('component/CLAUDE.md', '../AGENTS.md')
        self.assert_errors(f'{ADAPTER}: adapter must resolve', '.cursor/skills/new-rule/SKILL.md: adapter must resolve',
                           f'{CURSOR_RULE}: adapter must resolve', 'component/CLAUDE.md: CLAUDE.md must not be tracked')

    def test_canonical_skill_requires_an_adapter(self):
        (self.root / ADAPTER).unlink()
        self.assertEqual(([], []), self.check())
        (self.root / CURSOR_RULE).unlink()
        self.assert_errors(f'{SKILL}: missing Claude or Cursor skill adapter')

    def test_command_failures_and_dynamic_mise_tasks(self):
        self.write('.mise/tasks/check/_default', 'unused\n')
        self.doc(BUILD, '''```bash
mise run unit
mise run check
mise run missing:task
mise exec java@21 -- mvn installl -f missing/pom.xml -P missing -pl nonexistent
cd component && mvn test -Psanity
cd missing && mvn clean
mvn unknown:goal
./missing-tool
```
''')
        self.assert_errors('unknown mise task: missing:task', 'unknown Maven phase or goal: installl',
                           'unknown Maven POM: missing/pom.xml', 'unknown Maven profile: missing',
                           'unknown Maven project: nonexistent', 'unknown working directory: missing',
                           'unknown Maven phase or goal: unknown:goal', 'unknown repository command: ./missing-tool')
        self.assertNotIn('unknown mise task: unit', '\n'.join(self.check()[0]))
        self.assertNotIn('unknown mise task: check', '\n'.join(self.check()[0]))
        self.write('AGENTS.md', ROOT_CONTENT.replace('mise install', 'mise run test:unit'))
        self.assert_errors('document `mise install` as the setup command')
        self.doc(BUILD, '```bash\nmise install\n```\n')
        self.assertEqual(([], []), self.check())
