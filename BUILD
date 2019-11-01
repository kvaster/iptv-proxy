# GSuite Sync

load("@rules_jvm_external//:defs.bzl", "artifact")

config_setting(
    name = "local",
    values = {"define": "profile=local"},
)

java_binary(
    name = "iptv-proxy",
    srcs = glob(["src/main/java/**/*.java"]),
    main_class = "com.kvaster.iptv.App",
    resources = select({
        ":local": glob(["src/test/resources/**"]),
        "//conditions:default": glob(["src/main/resources/**"]),
    }),
    deps = [
        # logging
        artifact("org.slf4j:slf4j-api"),
        artifact("org.slf4j:log4j-over-slf4j"),
        artifact("org.slf4j:jcl-over-slf4j"),
        artifact("org.slf4j:jul-to-slf4j"),
        artifact("ch.qos.logback:logback-classic"),

        # app specific
        artifact("org.yaml:snakeyaml"),
        artifact("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml"),
        artifact("com.fasterxml.jackson.core:jackson-databind"),
        artifact("com.fasterxml.jackson.core:jackson-core"),
        artifact("com.fasterxml.jackson.core:jackson-annotations"),
        artifact("io.undertow:undertow-core"),

        # test
        #artifact("org.junit.jupiter:junit-jupiter"),
    ],
)
