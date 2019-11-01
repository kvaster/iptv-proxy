load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JVM_EXTERNAL_TAG = "2.9"

RULES_JVM_EXTERNAL_SHA = "e5b97a31a3e8feed91636f42e19b11c49487b85e5de2f387c999ea14d77c7f45"

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("@rules_jvm_external//:defs.bzl", "maven_install")

# common libs
JUNIT_VERSION = "5.5.1"

JACKSON_VERSION = "2.9.9"

JACKSON_DATABIND_VERSION = "2.9.9.3"

LOGBACK_VERSION = "1.2.3"

SLF4J_VERSION = "1.7.26"

SNAKE_YAML_VERSION = "1.25"

UNDERTOW_VERSION = "2.0.27.Final"

maven_install(
    artifacts = [
        # logging
        "org.slf4j:slf4j-api:%s" % SLF4J_VERSION,
        "org.slf4j:log4j-over-slf4j:%s" % SLF4J_VERSION,
        "org.slf4j:jcl-over-slf4j:%s" % SLF4J_VERSION,
        "org.slf4j:jul-to-slf4j:%s" % SLF4J_VERSION,
        "ch.qos.logback:logback-classic:%s" % LOGBACK_VERSION,

        # app specific
        "org.yaml:snakeyaml:%s" % SNAKE_YAML_VERSION,
        "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:%s" % JACKSON_VERSION,
        "com.fasterxml.jackson.core:jackson-databind:%s" % JACKSON_DATABIND_VERSION,
        "io.undertow:undertow-core:%s" % UNDERTOW_VERSION,

        # tests
        "org.junit.jupiter:junit-jupiter:%s" % JUNIT_VERSION,
    ],
    excluded_artifacts = [
        "org.apache.httpcomponents:httpclient",
        "org.apache.httpcomponents:httpcore",
        "com.sun.mail:javax.mail",
        "javax.activation:activation",
    ],
    fetch_sources = True,
    maven_install_json = "//:maven_install.json",
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
)

load("@maven//:defs.bzl", "pinned_maven_install")

pinned_maven_install()
