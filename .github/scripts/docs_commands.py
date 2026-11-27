"""Check Maven and mise snippets against repository configuration without running them."""

from functools import cached_property
from itertools import dropwhile
import posixpath
import re
import shlex
import tomllib
import xml.etree.ElementTree as ET

BUILD_DOC = 'docs/systems/build.md'
LICENSE_DOC = 'docs/systems/licenses.md'
MAVEN_PHASES = set('''pre-clean clean post-clean validate initialize generate-sources process-sources generate-resources
process-resources compile process-classes generate-test-sources process-test-sources generate-test-resources
process-test-resources test-compile process-test-classes test prepare-package package pre-integration-test integration-test
post-integration-test verify install deploy pre-site site post-site site-deploy'''.split())
MAVEN_CI_GOAL = 'org.codehaus.mojo:license-maven-plugin:aggregate-add-third-party'
MAVEN_OPTIONS = {'-f': 'pom', '--file': 'pom', '-P': 'profile', '--activate-profiles': 'profile', '-pl': 'project',
                 '--projects': 'project', '-D': 'skip', '--define': 'skip', '-T': 'skip', '--threads': 'skip',
                 '-s': 'settings', '--settings': 'settings', '-rf': 'project', '--resume-from': 'project'}
MAVEN_WRAPPER = './mvnw'
MAVEN_COMMANDS = ('mvn', MAVEN_WRAPPER)
ASSIGNMENT = re.compile(r'^\w+=')
MISE_TASK_DIRS = ('mise-tasks/', '.mise/tasks/', '.config/mise/tasks/')


def shell_commands(snippet):
    snippet = snippet.strip()
    if snippet.startswith('$') and snippet[1:2].isspace():
        snippet = snippet[1:].lstrip()
    command = next(dropwhile(ASSIGNMENT.match, snippet.split()), '')
    if command not in ('cd', 'mvn', 'mise') and not command.startswith('./'):
        return
    lexer = shlex.shlex(snippet, posix=True, punctuation_chars=';&|')
    lexer.whitespace_split = True
    for words in split_shell_commands(lexer):
        args = list(dropwhile(ASSIGNMENT.match, words))
        if args:
            yield args


def split_shell_commands(words):
    args = []
    for word in words:
        if word and all(char in ';&|' for char in word):
            yield args
            args = []
        else:
            args.append(word)
    yield args


def maven_option(arg):
    for flag, kind in MAVEN_OPTIONS.items():
        if arg == flag:
            return kind, None
        if arg.startswith(flag + '='):
            return kind, arg[len(flag) + 1:]
        if flag in ('-P', '-D') and arg.startswith(flag):
            return kind, arg[len(flag):]
    return None, None


def maven_arguments(args):
    queue = iter(args)
    for arg in queue:
        kind, value = maven_option(arg)
        if kind:
            yield arg, kind, value if value is not None else next(queue, None)
        elif not arg.startswith('-'):
            yield arg, 'goal', arg


def read_pom(path):
    try:
        pom = ET.parse(path).getroot()
    except (ET.ParseError, OSError):
        return None
    for elem in pom.iter():
        elem.tag = elem.tag.split('}')[-1]
    return pom if pom.tag == 'project' else None


def maven_projects(pom):
    artifact = pom.findtext('artifactId')
    if artifact:
        group = pom.findtext('groupId') or pom.findtext('parent/groupId')
        yield f':{artifact}'
        yield f'{group}:{artifact}'


def maven_goals(pom):
    for plugin in pom.findall('.//plugin'):
        artifact = plugin.findtext('artifactId')
        if not artifact:
            continue
        prefix = artifact.removeprefix('maven-').removesuffix('-maven-plugin').removesuffix('-plugin')
        group = plugin.findtext('groupId', 'org.apache.maven.plugins')
        for node in plugin.findall('./executions/execution/goals/goal'):
            yield f'{prefix}:{node.text}'
            yield f'{group}:{artifact}:{node.text}'


def configured_mise_tasks(config):
    with config.open('rb') as handle:
        for name, task in tomllib.load(handle).get('tasks', {}).items():
            yield name
            if isinstance(task, dict):
                aliases = task.get('alias', [])
                yield from [aliases] if isinstance(aliases, str) else aliases


def file_mise_task(path):
    for prefix in MISE_TASK_DIRS:
        if path.startswith(prefix):
            name = path.removeprefix(prefix).removesuffix('/_default')
            if all(not part.startswith(('.', '_')) for part in name.split('/')):
                return name.removesuffix('.sh').replace('/', ':')
    return None


class Commands:
    def __init__(self, repo):
        self.repo = repo
        self.has_setup = False

    def local_path(self, cwd, arg):
        return self.repo.resolve(posixpath.normpath(posixpath.join(cwd, arg)))

    @cached_property
    def maven(self):
        profiles, projects, poms, goals = set(), set(), set(), set()
        workflow = '.github/workflows/build.yml'
        if self.repo.resolve(workflow) and MAVEN_CI_GOAL in (self.repo.root / workflow).read_text(encoding='utf-8'):
            goals.update((MAVEN_CI_GOAL, 'license:aggregate-add-third-party'))
        for path in sorted(self.repo.files):
            name = posixpath.basename(path)
            if not (name.startswith('pom') and name.endswith('.xml')) or not self.repo.resolve(path):
                continue
            pom = read_pom(self.repo.root / path)
            if pom is None:
                continue
            poms.add(path)
            profiles.update(node.text for node in pom.findall('./profiles/profile/id'))
            projects.update(maven_projects(pom))
            goals.update(maven_goals(pom))
        return profiles, projects, poms, goals

    @cached_property
    def mise_tasks(self):
        tasks = {task for path in self.repo.files if (task := file_mise_task(path))}
        for config in ('mise.toml', '.mise.toml', '.mise/config.toml'):
            if self.repo.resolve(config):
                tasks.update(configured_mise_tasks(self.repo.root / config))
        return tasks

    def maven_path_errors(self, kind, value, cwd):
        _, _, poms, _ = self.maven
        if kind == 'settings':
            if not value.startswith(('~/', '$')) and not self.local_path(cwd, value):
                yield f'unknown Maven settings path: {value}'
        else:
            path = self.local_path(cwd, value)
            if path in self.repo.dirs:
                path = self.local_path(path, 'pom.xml')
            if path not in poms:
                yield f'unknown Maven POM: {value}'

    def maven_selector_errors(self, kind, value, cwd):
        profiles, projects, poms, _ = self.maven
        for selector in value.split(','):
            name = selector.lstrip('!?-+')
            if kind == 'profile':
                if name not in profiles:
                    yield f'unknown Maven profile: {selector}'
            elif name not in projects and self.local_path(cwd, f'{name}/pom.xml') not in poms:
                yield f'unknown Maven project: {name}'

    def maven_argument_errors(self, arg, kind, value, cwd):
        if not value or value.startswith('-'):
            yield f'Maven option {arg} needs a value'
        elif kind in ('pom', 'settings'):
            yield from self.maven_path_errors(kind, value, cwd)
        elif kind in ('profile', 'project'):
            yield from self.maven_selector_errors(kind, value, cwd)
        elif kind == 'goal':
            _, _, _, goals = self.maven
            coordinates = value.split(':')
            goal = ':'.join(coordinates[:2] + coordinates[-1:]) if len(coordinates) == 4 else value
            if goal not in MAVEN_PHASES and goal not in goals:
                yield f'unknown Maven phase or goal: {value}'

    def maven_errors(self, args, cwd):
        _, _, poms, _ = self.maven
        kinds = set()
        for arg, kind, value in maven_arguments(args):
            yield from self.maven_argument_errors(arg, kind, value, cwd)
            if value and not value.startswith('-'):
                kinds.add(kind)
        if 'goal' not in kinds:
            if not {'-v', '--version', '-h', '--help'}.intersection(args):
                yield 'Maven command has no phase or goal'
        elif 'pom' not in kinds and self.local_path(cwd, 'pom.xml') not in poms:
            yield f'no Maven POM in {cwd or "."}'

    def mise_exec_errors(self, args, cwd):
        if '--' not in args or args[-1] == '--':
            yield '`mise exec` needs a command after --'
            return
        nested, *nested_args = args[args.index('--') + 1:]
        if nested in MAVEN_COMMANDS:
            yield from self.maven_errors(nested_args, cwd)

    def mise_errors(self, args, cwd, allow_setup):
        action = args[0] if args else None
        if args == ['install']:
            self.has_setup |= allow_setup
            if not self.repo.resolve('mise.toml'):
                yield '`mise install` requires mise.toml'
        elif action in ('run', 'r'):
            if len(args) < 2 or args[1] not in self.mise_tasks:
                yield f'unknown mise task: {" ".join(args[1:]) or "(missing)"}'
        elif action in ('exec', 'x'):
            yield from self.mise_exec_errors(args, cwd)
        elif action not in ('install', 'tasks', 'doctor', 'trust', 'use', '--version'):
            yield f'unknown mise command: {" ".join(args) or "(missing)"}'

    def command_errors(self, command, args, cwd, allow_setup):
        if command in MAVEN_COMMANDS:
            if command == MAVEN_WRAPPER and not self.local_path(cwd, command):
                yield 'Maven wrapper does not exist'
            yield from self.maven_errors(args, cwd)
        elif command == 'mise':
            yield from self.mise_errors(args, cwd, allow_setup)
        elif command.startswith('./'):
            script = self.local_path(cwd, command)
            if script not in self.repo.files or not (self.repo.root / script).is_file():
                yield f'unknown repository command: {command}'

    def working_directory(self, args, cwd):
        directory = self.local_path(cwd, args[0]) if len(args) == 1 else None
        return directory if directory and (self.repo.root / directory).is_dir() else None

    def errors(self, snippets, allow_setup=False):
        cwd = '.'
        for snippet in snippets:
            try:
                for command, *args in shell_commands(snippet):
                    if command == 'cd':
                        directory = self.working_directory(args, cwd)
                        if directory is None:
                            yield f'unknown working directory: {" ".join(args)}'
                        else:
                            cwd = directory
                        continue
                    yield from self.command_errors(command, args, cwd, allow_setup)
            except ValueError as error:
                yield f'invalid shell command {snippet!r}: {error}'
