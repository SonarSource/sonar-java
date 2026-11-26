"""Static repository documentation checks. Run from the repository root."""

from collections import defaultdict
from datetime import date
from html.parser import HTMLParser
import os
import posixpath
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys
from urllib.parse import unquote, urlsplit

from markdown_it import MarkdownIt
import yaml

from docs_commands import BUILD_DOC, LICENSE_DOC, Commands

ROOT_DOC = 'AGENTS.md'
DOCS_INDEX = 'docs/index.md'
SKILLS = '.agents/skills/'
CURSOR_RULE = '.cursor/rules/rules-implementation.mdc'
ADAPTER_ROOTS = ('.claude/skills', '.cursor/skills')
MARKDOWN = MarkdownIt('commonmark').enable('table')
SONAR_MARKER = re.compile(r'\s*<!-- sonar:(begin|end):([\w.-]+) -->\s*')
FILENAME_STEM = re.compile(r'[\w-][\w.-]*')
DOTFILE = re.compile(r'\.git[a-z]+', re.I)
PATH_SUFFIXES = set('md mdc xml toml yml yaml json properties txt sh py java sarif'.split())
SKILL = re.compile(r'\.agents/skills/[^/]+/SKILL\.md')


def git_paths(root, *options):
    output = subprocess.check_output(['git', 'ls-files', '-z', *options], cwd=root)
    return {os.fsdecode(entry) for entry in output.split(b'\0') if entry}


class Repo:
    def __init__(self, root, tracked, untracked=()):
        self.root = root.resolve()
        self.tracked = tracked
        candidates = set(tracked).union(untracked)
        self.files = {name for name in candidates if os.path.lexists(root / name)}
        self.dirs = {parent.as_posix() for name in self.files for parent in PurePosixPath(name).parents}

    def resolve(self, name):
        """Resolve tracked files/directories (including symlinks) without leaving the checkout."""
        if name not in self.files and name not in self.dirs:
            if not any(parent.as_posix() in self.files for parent in PurePosixPath(name).parents):
                return None
        try:
            target = (self.root / name).resolve(strict=True).relative_to(self.root).as_posix()
        except (OSError, ValueError, RuntimeError):
            return None
        return target if target in self.files or target in self.dirs else None

    def is_pointer(self, text):
        if (any(char.isspace() for char in text) or ':' in text or text.startswith(('@', '//', '$'))
                or (text.startswith('<') and text.endswith('>') and '</' in text)
                or re.fullmatch(r'[A-Z]{2,}(?:/[A-Z]{2,})+', text)):
            return False
        return '/' in text or text in self.files or text in self.dirs or looks_like_filename(text)


def looks_like_filename(text):
    stem, _, suffix = text.rpartition('.')
    return bool(DOTFILE.fullmatch(text) or (suffix.lower() in PATH_SUFFIXES and FILENAME_STEM.fullmatch(stem)))


def is_doc(path):
    return path.startswith('docs/') and path.lower().endswith('.md')


def managed(path):
    if PurePosixPath(path).name == ROOT_DOC or is_doc(path):
        return True
    if path.startswith('.cursor/'):
        return path.lower().endswith(('.md', '.mdc'))
    return path.startswith((SKILLS, '.claude/skills/', '.claude/rules/', '.gitar/rules/')) and path.lower().endswith('.md')


def nonempty_string(value):
    return isinstance(value, str) and bool(value.strip())


def has_string_field(mapping, key):
    return isinstance(mapping, dict) and nonempty_string(mapping.get(key))


class HtmlReferences(HTMLParser):
    def __init__(self):
        super().__init__()
        self.links = []
        self.anchors = set()

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        for key in ('id', 'name') if tag == 'a' else ('id',):
            if attrs.get(key):
                self.anchors.add(attrs[key])
        if tag in ('a', 'img') and (value := attrs.get('href' if tag == 'a' else 'src')):
            self.links.append(value)


def inline_tokens(tokens):
    for token in tokens:
        yield token
        if token.children:
            yield from inline_tokens(token.children)


class Page:
    def __init__(self, text):
        self.lines = text.splitlines()
        self.frontmatter, body = self.split_frontmatter(text)
        self.blocks = MARKDOWN.parse(body)
        self.codes = []
        self.links = []
        html = self.read_references()
        self.links.extend(html.links)
        self.anchors = self.heading_anchors() | html.anchors

    def split_frontmatter(self, text):
        if self.lines and self.lines[0] == '---':
            try:
                end = self.lines.index('---', 1)
                return yaml.safe_load('\n'.join(self.lines[1:end])), '\n'.join(self.lines[end + 1:])
            except (ValueError, yaml.YAMLError):
                pass
        return None, text

    def read_references(self):
        html = HtmlReferences()
        in_link = False
        for token in inline_tokens(self.blocks):
            if token.type == 'code_inline' and not in_link:
                self.codes.append(token.content)
            elif token.type == 'link_open':
                self.links.append(token.attrGet('href'))
                in_link = True
            elif token.type == 'link_close':
                in_link = False
            elif token.type == 'image':
                self.links.append(token.attrGet('src'))
            elif token.type in ('html_inline', 'html_block'):
                html.feed(token.content)
        return html

    def heading_anchors(self):
        anchors = set()
        for previous, token in zip(self.blocks, self.blocks[1:]):
            if previous.type != 'heading_open':
                continue
            title = ''.join(child.content for child in token.children if child.type in ('text', 'code_inline'))
            slug = re.sub(r'[^\w\s-]', '', title.lower()).replace(' ', '-')
            anchor, suffix = slug, 1
            while anchor in anchors:
                anchor, suffix = f'{slug}-{suffix}', suffix + 1
            anchors.add(anchor)
        return anchors

    def command_blocks(self):
        for code in self.codes:
            yield [code]
        for token in self.blocks:
            if token.type == 'code_block' or (token.type == 'fence' and token.info.split()[:1] in
                    ([], ['sh'], ['shell'], ['bash'], ['console'], ['zsh'])):
                yield token.content.replace('\\\n', ' ').splitlines()


class Checker:
    def __init__(self, repo):
        self.repo = repo
        self.errors = []
        self.warnings = []
        self.pointers = defaultdict(set)
        self.pages = {}

    def error(self, path, message):
        self.errors.append(f'{path}: {message}')

    def warn(self, path, message):
        self.warnings.append(f'{path}: {message}')

    def page(self, path):
        if path not in self.pages:
            self.pages[path] = Page((self.repo.root / path).read_text(encoding='utf-8'))
        return self.pages[path]

    def root_body(self, page):
        body = []
        block = None
        for line in page.lines:
            if match := SONAR_MARKER.fullmatch(line):
                kind, name = match.groups()
                if kind == 'begin' and block is None:
                    block = name
                elif kind == 'end' and block == name:
                    block = None
                else:
                    self.error(ROOT_DOC, f'unbalanced Sonar managed block marker: {line.strip()}')
            elif block is None:
                body.append(line)
        if block:
            self.error(ROOT_DOC, f'unclosed Sonar managed block: {block}')
        return body

    def check_frontmatter(self, path, page):
        metadata = page.frontmatter
        if not isinstance(metadata, dict):
            self.error(path, 'expected a YAML frontmatter mapping between --- delimiters')
            return
        for field in ('title', 'description'):
            if not nonempty_string(metadata.get(field)):
                self.error(path, f'frontmatter {field} must be a nonempty string')
        if path == DOCS_INDEX and metadata.get('okf_version') != '0.2':
            self.error(path, 'frontmatter okf_version must be the string "0.2"')
        self.check_provenance(path, metadata)
        if 'stale_after' in metadata:
            self.check_expiry(path, str(metadata['stale_after']))

    def check_provenance(self, path, metadata):
        sources = metadata.get('sources', [])
        if not isinstance(sources, list) or not all(has_string_field(source, 'resource') for source in sources):
            self.error(path, 'frontmatter sources must be a list of mappings with a nonempty resource string')
        public_tutorial = path == 'docs/CUSTOM_RULES_101.md' or path.startswith(('docs/custom-rules/', 'docs/java-custom-rules-example/'))
        if not public_tutorial or 'generated' in metadata:
            if not has_string_field(metadata.get('generated'), 'by'):
                self.error(path, 'frontmatter generated.by must be a nonempty string for agent-written content')

    def check_expiry(self, path, value):
        try:
            if not re.fullmatch(r'\d{4}-\d\d-\d\d', value):
                raise ValueError
            expiry = date.fromisoformat(value)
        except ValueError:
            self.error(path, 'frontmatter stale_after must be a YYYY-MM-DD date')
            return
        if expiry < date.today():
            self.warn(path, f'stale_after date has passed: {expiry}')

    def check_pointers(self, path, page):
        for text in set(page.codes):
            if not self.repo.is_pointer(text):
                continue
            target = self.repo.resolve(text.rstrip('/'))
            if not target or (text.endswith('/') and not (self.repo.root / target).is_dir()):
                self.error(path, f'unknown repo path: `{text}` (use a literal repository-root path)')
                continue
            self.record_pointer(path, target)

    def record_pointer(self, path, target):
        index = f'{target}/index.md'
        self.pointers[path].add(index if target in self.repo.dirs and index in self.repo.files else target)

    def link_target(self, path, url):
        if not url:
            return path, ''
        parts = urlsplit(url)
        if parts.scheme or parts.netloc:
            return None
        link = unquote(parts.path)
        if link.startswith('/'):
            destination = posixpath.normpath(link.lstrip('/'))
        elif link:
            destination = posixpath.normpath(posixpath.join(posixpath.dirname(path), link))
        else:
            destination = path
        return self.repo.resolve(destination), unquote(parts.fragment)

    def check_links(self, path, page):
        for url in set(page.links):
            try:
                link = self.link_target(path, url)
            except ValueError:
                self.error(path, f'invalid link: {url}')
                continue
            if link is None:
                continue
            target, fragment = link
            if not target:
                self.error(path, f'broken internal link: {url}')
            elif (fragment and target.lower().endswith(('.md', '.mdc')) and
                  fragment not in self.page(target).anchors):
                self.error(path, f'unknown Markdown anchor: {url}')
            else:
                self.record_pointer(path, target)

    def stub_target(self, path, canonical):
        page = self.page(path)
        if len(page.lines) > 30:
            return None
        literals = (text.removeprefix('@') for text in (*page.codes, *page.lines))
        refs = {text for text in literals if SKILL.fullmatch(text)}
        for url in page.links:
            try:
                link = self.link_target(path, url)
            except ValueError:
                continue
            if link and link[0] in canonical:
                refs.add(link[0])
        return next(iter(refs)) if len(refs) == 1 else None

    def check_adapter(self, path, canonical, expected):
        target = self.repo.resolve(path)
        if target and (self.repo.root / target).is_dir():
            target = self.repo.resolve(f'{target}/SKILL.md')
        if target == path:
            target = self.stub_target(path, canonical)
        if target != expected or target not in canonical:
            self.error(path, f'adapter must resolve to {expected}')

    def check_adapters(self, canonical):
        covered = set()
        for directory in ADAPTER_ROOTS:
            tool = directory.split('/')[0]
            if any((self.repo.root / path).is_symlink() for path in (tool, directory)):
                self.error(directory, 'use per-skill adapters, not a symlink for the entire tool or skills directory')
                continue
            entries = {path.split('/')[2] for path in self.repo.files if path.startswith(directory + '/')}
            for name in sorted(entries):
                folder = f'{directory}/{name}'
                expected = f'{SKILLS}{name}/SKILL.md'
                covered.add(expected)
                self.check_adapter(folder if (self.repo.root / folder).is_symlink() else f'{folder}/SKILL.md',
                                   canonical, expected)
        if CURSOR_RULE in self.repo.files:
            expected = f'{SKILLS}new-rule/SKILL.md'
            covered.add(expected)
            self.check_adapter(CURSOR_RULE, canonical, expected)
        for skill in sorted(canonical - covered):
            self.error(skill, 'missing Claude or Cursor skill adapter')

    def check_page(self, path, commands):
        page = self.page(path)
        if path == ROOT_DOC:
            lines = self.root_body(page)
            if len(lines) > 150:
                self.error(path, f'{len(lines)} lines outside Sonar managed blocks (maximum 150)')
            elif len(page.lines) > 150:
                self.warn(path, f'{len(page.lines)} total lines including Sonar managed blocks (target 150)')
            page = Page('\n'.join(lines))
        elif len(page.lines) > 300:
            self.error(path, f'{len(page.lines)} lines (maximum 300)')
        if is_doc(path):
            self.check_frontmatter(path, page)
        self.check_pointers(path, page)
        self.check_links(path, page)
        if path in (ROOT_DOC, BUILD_DOC, LICENSE_DOC):
            for block in page.command_blocks():
                for error in commands.errors(block, allow_setup=path != LICENSE_DOC):
                    self.error(path, error)

    def check_reachability(self, docs, canonical):
        reachable, pending = {ROOT_DOC}, [ROOT_DOC]
        while pending:
            for target in self.pointers[pending.pop()] - reachable:
                reachable.add(target)
                pending.append(target)
        for path in sorted(docs - reachable):
            if is_doc(path) or PurePosixPath(path).name == ROOT_DOC or path.startswith(SKILLS):
                self.error(path, 'not reachable from AGENTS.md through repo pointers or internal links')
        for skill in sorted(canonical - self.pointers[ROOT_DOC]):
            self.error(skill, 'canonical skill must have a direct pointer from AGENTS.md')

    def check_layout(self, docs):
        for path in sorted(self.repo.tracked):
            if PurePosixPath(path).name.casefold() == 'claude.md' and path in self.repo.files:
                self.error(path, 'CLAUDE.md must not be tracked; use AGENTS.md')
        for path in (ROOT_DOC, DOCS_INDEX, BUILD_DOC):
            if not self.repo.resolve(path):
                self.error(path, 'required documentation file is missing')
        for directory in sorted({posixpath.dirname(path) for path in docs if is_doc(path)}):
            if f'{directory}/index.md' not in docs and directory != 'docs':
                self.error(directory, 'documentation directory is missing index.md')

    def run(self):
        docs = {path for path in self.repo.files if managed(path)}
        self.check_layout(docs)
        commands = Commands(self.repo)
        for path in sorted(docs):
            target = self.repo.resolve(path)
            if target == path and (self.repo.root / path).is_file():
                self.check_page(path, commands)
            elif not target or not path.startswith(('.claude/', '.cursor/')):
                self.error(path, 'managed documentation must be a file inside the repository')
        if not commands.has_setup:
            self.error(BUILD_DOC, 'document `mise install` as the setup command here or in AGENTS.md')
        canonical = {path for path in docs if SKILL.fullmatch(path) and self.repo.resolve(path) == path}
        self.check_adapters(canonical)
        self.check_reachability(docs, canonical)
        return self.errors, self.warnings


def main():
    root = Path(__file__).resolve().parents[2]
    repo = Repo(root, git_paths(root, '--cached'), git_paths(root, '--others', '--exclude-standard'))
    errors, warnings = Checker(repo).run()
    for notice in sorted(set(warnings)):
        print(f'Warning: {notice}', file=sys.stderr)
    for notice in sorted(set(errors)):
        print(f'Error: {notice}', file=sys.stderr)
    return bool(errors)


if __name__ == '__main__':
    sys.exit(main())
