#!/usr/bin/env python3
"""Read-only readiness checks for the mise tasks; never print probe output."""

import base64
from concurrent.futures import Future, TimeoutError as FutureTimeout
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
from threading import Thread
from time import monotonic
from urllib.error import HTTPError
from urllib.parse import unquote, urlsplit
from urllib.request import HTTPRedirectHandler, Request, build_opener
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
HOME = Path.home()
GIB = 1024**3
SUBMODULE_FIX = "run git submodule update --init --recursive -- its/sources"
MAVEN_FIX = "configure a Repox mirror and matching server credentials in Maven settings"
ORCHESTRATOR_FIX = "set ORCHESTRATOR_CONFIG_URL to a local Orchestrator properties file (file:// URL)"
TOKEN_FIX = "refresh orchestrator.artifactory.accessToken in the Orchestrator properties file"
GITHUB_FIX = "export a valid GITHUB_TOKEN with read access to the QA licenses"
SONAR_FIX = "run sonar auth login with an authorized SonarQube Cloud account"


class ProbeFailure(Exception):
    """Contains only fixed, safe-to-print diagnostics."""


def fail(reason, fix):
    raise ProbeFailure(f"{reason} — {fix}")


def command(args, deadline, *, env=None, input_text=None):
    return subprocess.run(
        args, cwd=ROOT, input=input_text, stdin=subprocess.DEVNULL if input_text is None else None,
        stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, env=env,
        text=True, errors="replace", timeout=max(0.01, deadline - monotonic()), check=False,
    )


def mise_home(tool, deadline):
    # --no-config avoids mise's tracked-config writes. Explicit versions still
    # resolve from the same tool installation directory used by the tasks.
    try:
        result = command(["mise", "--no-config", "where", tool], deadline)
    except FileNotFoundError:
        fail("mise is not installed", "install mise and run mise install")
    if result.returncode or not result.stdout.strip():
        fail(f"{tool} is not installed", f"run mise install {tool}")
    return Path(result.stdout.strip())


def jdk(version, deadline):
    tool = f"java@{version}"
    path = mise_home(tool, deadline)
    compiler = path / "bin/javac"
    if not os.access(compiler, os.X_OK) or not os.access(path / "bin/java", os.X_OK):
        fail(f"{tool} is not a complete JDK", f"reinstall {tool} with mise")
    result = command([compiler, "-version"], deadline)
    if result.returncode or not re.search(rf"\bjavac {version}(?:\.|\b)", result.stdout):
        fail(f"JDK {version} cannot run", f"reinstall {tool} with mise")
    return path


def maven_home(deadline):
    path = mise_home("maven@3.9", deadline)
    # The mise Maven backend can keep the extracted Maven directory nested.
    for candidate in (path, *path.glob("apache-maven-3.9.*")):
        if os.access(candidate / "bin/mvn", os.X_OK) and any((candidate / "lib").glob("maven-core-3.9.*.jar")):
            return candidate
    fail("Maven 3.9 is not a complete installation", "reinstall maven@3.9 with mise")


def xml(path):
    root = ET.parse(path).getroot()
    for node in root.iter():
        node.tag = node.tag.rsplit("}", 1)[-1]
    return root


def interpolate(value):
    return re.sub(r"\$\{env\.([^}]+)\}", lambda match: os.getenv(match[1], ""), value.strip())


def setting(node, key):
    return interpolate(node.findtext(key, ""))


def settings_files(maven):
    user = HOME / ".m2/settings.xml"
    global_settings = maven / "conf/settings.xml"
    config = ROOT / ".mvn/maven.config"
    # Maven 3.9 uses one argument per line in .mvn/maven.config.
    args = (config.read_text().splitlines() if config.is_file() else []) + os.getenv("MAVEN_ARGS", "").split()
    for index, arg in enumerate(args):
        flag, equals, value = arg.strip().partition("=")
        if flag in ("-s", "--settings", "-gs", "--global-settings"):
            path = Path(value if equals else args[index + 1].strip())
            if flag in ("-s", "--settings"):
                user = ROOT / path
            else:
                global_settings = ROOT / path
    return user, global_settings


def maven_settings(maven):
    mirrors, servers = {}, {}
    for path in settings_files(maven):
        if not path.is_file():
            continue
        settings = xml(path)
        for group, tag, entries in (("mirrors", "mirror", mirrors), ("servers", "server", servers)):
            for entry in settings.findall(f"./{group}/{tag}"):
                entries.setdefault(setting(entry, "id"), entry)
    return mirrors, servers


def https_url(url):
    parsed = urlsplit(url)
    return parsed.scheme == "https" and parsed.hostname and not (
        parsed.username or parsed.password or parsed.query or parsed.fragment
    )


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        # Do not forward Authorization to a different endpoint or accept a login page.
        return None


def request_status(url, authorization, deadline, method="HEAD", accept=None):
    headers = {"Authorization": authorization, "User-Agent": "sonar-java-mise-doctor"}
    timeout = min(2.5, max(0.01, deadline - monotonic()))
    try:
        with build_opener(NoRedirect()).open(Request(url, headers=headers, method=method), timeout=timeout) as response:
            if accept is not None and not accept(response.read(256 * 1024)):
                return 404
            return response.status
    except HTTPError as error:
        error.close()
        return error.code
    except (OSError, ValueError):
        return None


def check_access(status, service, credential_fix, endpoint_fix):
    if status is None or status >= 500:
        fail(f"{service} is unreachable", endpoint_fix)
    if status in (401, 403):
        fail(f"{service} rejected the credentials or access", credential_fix)
    if not 200 <= status < 300:
        fail(f"{service} did not return the required resource", endpoint_fix)


# Use Maven's cipher/dispatcher rather than reimplementing its password format.
# The ephemeral Java source contains no credentials. Ciphertext goes in over a
# pipe, and decrypted credentials are captured in memory, never logged or saved.
DECRYPT_SOURCE = """
import java.nio.charset.StandardCharsets;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
class Decrypt {
    public static void main(String[] args) throws Exception {
        var dispatcher = new DefaultSecDispatcher(new DefaultPlexusCipher());
        dispatcher.setConfigurationFile(args[0]);
        System.out.print(dispatcher.decrypt(new String(System.in.readAllBytes(), StandardCharsets.UTF_8)));
    }
}
"""


def decipher(java, maven, ciphertext, deadline):
    with tempfile.TemporaryDirectory(prefix="sonarjava-doctor-") as scratch:
        source = Path(scratch) / "Decrypt.java"
        source.write_text(DECRYPT_SOURCE, encoding="utf-8")
        env = {key: value for key, value in os.environ.items()
               if key not in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")}
        result = command([java / "bin/java", "-cp", str(maven / "lib/*"), source,
                          HOME / ".m2/settings-security.xml"], deadline, env=env, input_text=ciphertext)
    if result.returncode or not result.stdout or result.stdout == ciphertext:
        fail("Maven could not decrypt the Repox password", "check Maven settings-security.xml and refresh the Repox password")
    return result.stdout


def repox(deadline, tools):
    maven = tools("maven")
    if not maven:
        fail("Repox credentials cannot be checked without Maven 3.9", "run mise install maven@3.9")
    try:
        mirrors, servers = maven_settings(maven)
        # Maven considers exact repository IDs before wildcard mirrors.
        mirror = next((entry for match in ("central", "*") for entry in mirrors.values()
                       if setting(entry, "mirrorOf") == match), None)
        url = setting(mirror, "url") if mirror is not None else ""
        if not https_url(url) or "repox" not in urlsplit(url).hostname.lower():
            fail("Maven has no HTTPS Repox mirror for central", MAVEN_FIX)
        server = servers.get(setting(mirror, "id"))
    except (OSError, ET.ParseError, IndexError, ValueError):
        fail("Maven settings could not be read", "correct Maven settings.xml and any settings overrides")
    username = setting(server, "username") if server is not None else ""
    password = setting(server, "password") if server is not None else ""
    if not username or not password or "${" in username or "${" in password:
        fail("Maven credentials for the configured Repox mirror are missing", MAVEN_FIX)
    if password.startswith("{") and password.endswith("}"):
        java = tools("java26") or tools("java21")
        if not java:
            fail("the encrypted Repox password could not be checked without Java", "run mise install")
        password = decipher(java, maven, password, deadline)
    auth = base64.b64encode(f"{username}:{password}".encode()).decode("ascii")
    parent = xml(ROOT / "pom.xml").find("parent")
    group, artifact, version = (parent.findtext(key) for key in ("groupId", "artifactId", "version"))
    path = f"{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}.pom"
    status = request_status(f"{url.rstrip('/')}/{path}", f"Basic {auth}", deadline)
    check_access(status, "Repox", "refresh the Maven Repox credentials and check read access", "check VPN/network and the Repox mirror URL")


def orchestrator_properties():
    try:
        source = urlsplit(os.getenv("ORCHESTRATOR_CONFIG_URL", ""))
        if source.scheme != "file" or source.netloc not in ("", "localhost") or not source.path.startswith("/"):
            fail("the Orchestrator properties file is not configured", ORCHESTRATOR_FIX)
        text = Path(unquote(source.path)).read_text(encoding="latin-1")
    except (OSError, ValueError):
        fail("the Orchestrator properties file is unreadable", ORCHESTRATOR_FIX)
    properties = {}
    for line in text.splitlines():
        match = re.match(r"\s*([^\s#!:=]+)(?:\s*[=:]\s*|\s+)(.*)", line)
        if match:
            key, value = match.groups()
            properties[key] = value.strip().replace(r"\:", ":").replace(r"\=", "=")
    return properties


def orchestrator(deadline, properties):
    props = properties
    if props is None:
        return
    url = props.get("orchestrator.artifactory.url", "")
    if not https_url(url):
        fail("the Orchestrator Artifactory URL is missing or not HTTPS", "set orchestrator.artifactory.url in the Orchestrator properties file")
    token = props.get("orchestrator.artifactory.accessToken", "")
    if not token:
        fail("the Orchestrator Artifactory token is missing", TOKEN_FIX)
    # Query the same commercial release coordinates that Orchestrator resolves.
    path = "api/search/versions?g=com.sonarsource.sonarqube&a=sonarqube-enterprise&remote=1&repos=sonarsource-releases"
    status = request_status(f"{url.rstrip('/')}/{path}", f"Bearer {token}", deadline, method="GET",
                            accept=lambda data: bool(json.loads(data).get("results")))
    check_access(status, "Orchestrator Artifactory", TOKEN_FIX, "check VPN/network, orchestrator.artifactory.url and read access to release artifacts")


def github(deadline, properties):
    props = properties or {}
    from_env = os.getenv("GITHUB_TOKEN", "").strip()
    token = from_env or props.get("github.token", "")
    if not token:
        fail("the GitHub QA license token is missing", GITHUB_FIX)
    fix = GITHUB_FIX if from_env else "refresh github.token in the Orchestrator properties file or export GITHUB_TOKEN"
    # Checking repository contents, rather than /user, also verifies its scope.
    status = request_status("https://api.github.com/repos/SonarSource/licenses/contents", f"Bearer {token}", deadline, method="GET")
    if status == 404:
        fail("the GitHub token cannot read the QA licenses", fix)
    check_access(status, "GitHub QA license repository", fix, "check network access to GitHub and the QA licenses")


def git(args, deadline):
    # Avoid optional locks or fetching a missing object from a partial clone.
    return command(["git", *args], deadline, env={**os.environ, "GIT_OPTIONAL_LOCKS": "0", "GIT_NO_LAZY_FETCH": "1"})


def sources(deadline):
    status = git(["submodule", "status", "--recursive", "--", "its/sources"], deadline)
    lines = status.stdout.splitlines()
    if status.returncode or not lines or any(not line.startswith(" ") for line in lines):
        fail("its/sources or a nested submodule is not at its recorded checkout", SUBMODULE_FIX)


def change_base(deadline):
    base = os.environ.get("MISE_CHANGE_BASE", "origin/master")
    commit = git(["rev-parse", "--verify", "--end-of-options", f"{base}^{{commit}}"], deadline)
    if commit.returncode or git(["merge-base", "HEAD", commit.stdout.strip()], deadline).returncode:
        fail("the change base does not resolve to a commit with a common ancestor", "set MISE_CHANGE_BASE to a local commit or fetch origin/master")


def disk(_deadline):
    for path in (ROOT, HOME / ".m2"):
        while not path.exists() and path != path.parent:
            path = path.parent
        if shutil.disk_usage(path).free < 10 * GIB:
            fail("less than 10 GiB of free space for Maven", "free space on the checkout and Maven cache filesystems")


def memory(deadline):
    if sys.platform == "darwin":
        result = command(["sysctl", "-n", "hw.memsize"], deadline)
        if result.returncode:
            return
        total = int(result.stdout.strip())
    else:
        total = os.sysconf("SC_PHYS_PAGES") * os.sysconf("SC_PAGE_SIZE")
    if 0 < total < 12 * GIB:
        return "WARNING: less than 12 GiB of RAM; Maven tasks use an 8 GiB Java heap."


def sonar(deadline):
    if not shutil.which("sonar"):
        fail("the sonar CLI is not installed", "install the SonarQube CLI and run sonar auth login")
    status = command(["sonar", "auth", "status", "--format", "json"], deadline,
                     env={**os.environ, "DO_NOT_TRACK": "1"})
    if status.returncode:
        fail("the sonar CLI could not verify its login", SONAR_FIX)
    try:
        if json.loads(status.stdout).get("status") == "connected":
            return
    except (ValueError, AttributeError):
        pass
    fail("the sonar CLI is not logged in", SONAR_FIX)


def require_jar(module, fix):
    jars = (ROOT / module / "target").glob(f"{Path(module).name}-*.jar")
    if not any(path.is_file() and not path.name.endswith(("-sources.jar", "-javadoc.jar", "-tests.jar")) for path in jars):
        fail(f"the {Path(module).name} JAR has not been built", fix)


# Share each prerequisite probe across tasks. QA only runs Maven on Java 21;
# the artifacts it consumes were built on Java 26 by separate tasks.
MAVEN_COMMON = ("maven", "repox", "disk")
MAVEN = ("java26",) + MAVEN_COMMON
QA = ("java21",) + MAVEN_COMMON + ("orchestrator_file", "orchestrator", "github", "plugin_jar")
RULING = QA + ("sources",)
TASKS = {
    "build": MAVEN,
    "test:unit": MAVEN,
    "test:sanity": MAVEN,
    "test:custom-rules-example": MAVEN,
    "lint:licenses": MAVEN,
    "its:plugin": QA + ("example_jar",),
    "ruling": RULING,
    "check": MAVEN,
    "ci": tuple(key for key in dict.fromkeys(MAVEN + RULING) if key != "plugin_jar"),
    "build:changed": MAVEN + ("change_base",),
    "test:changed": MAVEN + ("change_base",),
    "lint:licenses:changed": MAVEN + ("change_base",),
    "test:custom-rules-example:changed": MAVEN + ("change_base",),
    "check:changed": MAVEN + ("change_base",),
    "analyze:vortex": ("sonar",),
    "doctor": (),
}


def start_probe(check, deadline, label, fix):
    result = Future()

    def run():
        try:
            result.set_result((check(deadline), None))
        except ProbeFailure as failure:
            result.set_result((None, str(failure)))
        except subprocess.TimeoutExpired:
            result.set_result((None, f"the {label} check timed out — {fix}"))
        except Exception:
            # Exceptions from parsers and subprocesses may include secret values.
            result.set_result((None, f"the {label} check failed — {fix}"))

    # A stalled system DNS resolution must not extend the wall-clock deadline.
    Thread(target=run, daemon=True).start()
    return result, label, fix


def main():
    deadline = monotonic() + 4.5
    checks = {}

    def add(name, check, label, fix):
        checks[name] = start_probe(check, deadline, label, fix)

    def value(name):
        return checks[name][0].result()[0]

    add("java26", lambda end: jdk(26, end), "Java 26", "run mise install java@26")
    add("java21", lambda end: jdk(21, end), "Java 21", "run mise install java@21")
    add("maven", maven_home, "Maven", "run mise install maven@3.9")
    add("repox", lambda end: repox(end, value), "Repox", "check VPN/network and Maven settings")
    add("orchestrator_file", lambda _: orchestrator_properties(), "Orchestrator file", ORCHESTRATOR_FIX)
    add("orchestrator", lambda end: orchestrator(end, value("orchestrator_file")), "Orchestrator Artifactory", TOKEN_FIX)
    add("github", lambda end: github(end, value("orchestrator_file")), "GitHub", GITHUB_FIX)
    build_fix = "run mise run build"
    add("plugin_jar", lambda _: require_jar("sonar-java-plugin", build_fix), "analyzer JAR", build_fix)
    add("example_jar", lambda _: require_jar("docs/java-custom-rules-example", build_fix), "custom example JAR", build_fix)
    add("sources", sources, "recursive submodule", SUBMODULE_FIX)
    add("change_base", change_base, "change base", "set MISE_CHANGE_BASE to a local commit or fetch origin/master")
    add("disk", disk, "disk space", "check free space on the checkout and Maven cache filesystems")
    add("sonar", sonar, "SonarQube CLI login", SONAR_FIX)
    add("memory", memory, "memory", "check that the machine has at least 12 GiB of RAM")

    results = {}
    for key, (future, label, fix) in checks.items():
        try:
            results[key] = future.result(timeout=max(0, deadline - monotonic()))
        except FutureTimeout:
            results[key] = (None, f"the {label} check timed out — {fix}")
    if results["memory"][0]:
        print(results["memory"][0])

    unavailable = False
    for name, prerequisites in TASKS.items():
        errors = [results[check][1] for check in prerequisites if results[check][1]]
        if errors:
            unavailable = True
            print(f"{name}: CANNOT RUN: {'; '.join(errors)}")
        else:
            print(f"{name}: OK")
    return int(unavailable)


if __name__ == "__main__":
    sys.exit(main())
