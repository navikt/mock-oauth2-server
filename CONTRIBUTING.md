# Contributing

This project is open to feature requests and contributions from the open source community.
Please fork the repo and start a new branch for your changes.

## Prerequisites

This project requires **Java 21** (Temurin recommended). If you use [mise](https://mise.jdm.run/), the correct version is declared in `.mise.toml` and will be activated automatically:

```
mise install
```

Otherwise install Java 21 manually and make sure `JAVA_HOME` points to it.

## Building locally

This project uses [Gradle](https://gradle.org/). A Gradle wrapper is included so you do not need a local Gradle installation.

Build the project:

```shell
./gradlew build
```

Run only the tests:

```shell
./gradlew test
```

Build the Docker image to your local Docker daemon:

```shell
./gradlew -Djib.from.platforms=linux/amd64 jibDockerBuild
```

## Code style

The project uses the [Kotlin official code style](https://kotlinlang.org/docs/coding-conventions.html), enforced by [kotlinter](https://github.com/jeremymailen/kotlinter-gradle). An `.editorconfig` file is included and most IDEs will pick it up automatically.

Check formatting:

```shell
./gradlew lintKotlin
```

Auto-fix formatting:

```shell
./gradlew formatKotlin
```

The build will fail if there are formatting violations.

## Testing

Tests are written using [JUnit 5](https://junit.org/junit5/) and [Kotest](https://kotest.io/). If you are adding a new feature or fixing a bug, please include tests that cover the change.

Run the full test suite:

```shell
./gradlew test
```

## Upgrading the Gradle wrapper

Find the latest version at https://gradle.org/releases/ then run:

```shell
./gradlew wrapper --gradle-version $gradleVersion
```

Also update the version in `build.gradle.kts`:

```
gradleVersion = "$gradleVersion"
```

## Pull Request Review

If you have a branch on your fork that is ready to be merged, please create a new pull request. The maintainers will review to make sure the above guidelines have been followed and if the changes are helpful to all library users, they will be merged.

## Releasing

The release process has been automated in GitHub Actions. Every merge into master is automatically added to the
[draft release notes](https://github.com/navikt/mock-oauth2-server/releases) of the next version. Once the next
version is ready to be released, simply publish the release with the version name as the title and tag and this
will trigger the publishing process.

This project uses [semantic versioning](https://semver.org/) and does NOT prefix tags or release titles with `v` i.e. use `1.2.3` instead of `v1.2.3`.

