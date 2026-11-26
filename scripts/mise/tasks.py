import os
from pathlib import Path
import re
import shlex
import subprocess
import sys


ROOT = Path(__file__).resolve().parents[2]
PLUGIN_TASK = "its:plugin"
SKIP_COVERAGE = "-Djacoco.skip=true"
ALLOW_UNMATCHED = "-Dsurefire.failIfNoSpecifiedTests=false"
IT_TASKS = {PLUGIN_TASK, "ruling"}


def run(command, **kwargs):
    subprocess.run(command, cwd=ROOT, check=True, **kwargs)


def maven(*args, task=None, github_token=False):
    env = os.environ.copy()
    ci = "CI" in env
    if not github_token:
        env.pop("GITHUB_TOKEN", None)
    env.setdefault("MAVEN_OPTS", "-Xmx8g")
    options = shlex.split(env.get("MAVEN_OUTPUT_ARGS", ""))
    if not ci:
        env["MAVEN_OPTS"] += " -Ddevelocity.scan.disabled=true"
        options.append("-Ddevelocity.scan.disabled=true")
    java = "21" if task in IT_TASKS else "26"
    run(["mise", "exec", f"java@{java}", "--", "mvn", "--batch-mode", *options, *args], env=env)


def unit(module=None, test=None, coverage=False, goal="install", extra_args=()):
    args = [goal, "-Dsonar.skip=true", *extra_args]
    if module:
        args += ["-pl", module, "-am"]
    if test:
        args += [f"-Dtest={test}", ALLOW_UNMATCHED]
    if not coverage:
        args.append(SKIP_COVERAGE)
    maven(*args)


def sanity():
    options = ["verify", "-Psanity", "-Dtest=SanityTest", SKIP_COVERAGE]
    sources = "java-checks-test-sources"
    if "CI" in os.environ and os.environ.get("BUILD_NUMBER"):
        maven("-f", f"{sources}/pom.xml", "clean", "compile")
        maven("-f", "sonar-java-plugin/pom.xml", *options)
    else:
        projects = f"sonar-java-plugin,{sources}/default,{sources}/aws"
        maven("-pl", projects, "-am", ALLOW_UNMATCHED, *options)


def custom_rules_example():
    maven("-f", "docs/java-custom-rules-example/pom_SQ_10_6_LATEST.xml", "clean", "package")


def licenses():
    maven("org.codehaus.mojo:license-maven-plugin:aggregate-add-third-party",
          f"-Dlicense.missingFile={ROOT / 'missing-dep-licenses.properties'}", "-DuseMissingFile")


def integration(task, profile, sq_version, selectors, github_token=True):
    directory = "plugin" if task == PLUGIN_TASK else "ruling"
    args = ["-f", f"its/{directory}/pom.xml", "package", f"-P{profile}",
            f"-Dsonar.runtimeVersion={sq_version}", "-Dmaven.test.redirectTestOutputToFile=false"]
    for variable, prop in (("ORCHESTRATOR_CONFIG_URL", "orchestrator.configUrl"),
                           ("ORCHESTRATOR_HOME", "orchestrator.home")):
        if value := os.environ.get(variable):
            args.append(f"-D{prop}={value}")
    if "CI" in os.environ:
        parallel = "classes" if task == PLUGIN_TASK else "methods"
        args += [f"-Dparallel={parallel}", "-DuseUnlimitedThreads=true"]
    maven(*args, *selectors, task=task, github_token=github_token)


def plugin(test, sq_version, community_only):
    selectors = ["-DcommunityEditionTestsOnly=true"] if community_only else []
    if test:
        selectors += [f"-Dtest={test}", ALLOW_UNMATCHED]
    selected_class = test.rsplit(".", 1)[-1] if test else None
    needs_license = selected_class != "SonarLintIntegrationTest" and not (community_only and selected_class == "JspTest")
    integration(PLUGIN_TASK, "it-plugin", sq_version, selectors, github_token=needs_license)


def ruling(profile, rules, project, sq_version):
    selectors = [f"-DsonarRules={rules}"] if rules else []
    if project:
        if (profile == "only-sonarqube-project") != (project == "sonarqube_server"):
            raise SystemExit("The selected method does not belong to the ruling profile.")
        selectors.append(f"-Druling.test=org.sonar.java.it.JavaRulingTest#{project}")
    integration("ruling", f"it-ruling,{profile}", sq_version, selectors)


def usage(name, pattern=None):
    value = os.environ.get(f"usage_{name}")
    if value and pattern and not re.fullmatch(pattern, value):
        sys.exit(f"Invalid {name.replace('_', '-')} selector.")
    return value


def vortex():
    env = os.environ.copy()
    env.pop("GITHUB_TOKEN", None)
    command = ["sonar", "analyze", "agentic", "--depth", "DEEP"]
    if base := env.get("MISE_CHANGE_BASE"):
        command += ["--base", base]
    run(command, env=env)


def main(task):
    if task == "test:unit":
        unit(usage("module"), usage("test"), usage("coverage") == "true")
    elif task == PLUGIN_TASK:
        plugin(usage("test", r"[\w.]+"), usage("sq_version") or "LATEST_RELEASE", usage("community_only") == "true")
    elif task == "ruling":
        ruling(usage("profile"), usage("rules", r"S\d+(,S\d+)*"), usage("project", r"\w+"),
               usage("sq_version") or "LATEST_RELEASE")
    else:
        actions = {
            "analyze:vortex": vortex,
            "build": lambda: maven("install", "-DskipTests", SKIP_COVERAGE, "-Dsonar.skip=true"),
            "test:sanity": sanity,
            "test:custom-rules-example": custom_rules_example,
            "lint:licenses": licenses,
        }
        actions[task]()


if __name__ == "__main__":
    try:
        main(sys.argv[1])
    except subprocess.CalledProcessError as error:
        sys.exit(error.returncode)
    except FileNotFoundError as error:
        sys.exit(f"Missing executable: {error.filename}")
