# Contributing
This project is open to accept feature requests and contributions from the open source community. 
Please fork the repo and start a new branch to work on.


## Building locally
This project is using [Gradle](https://gradle.org/) for its build tool.
 A Gradle Wrapper is included in the code though so you do not have to manage your own installation.

To run a build simply exucute the following:

```shell script
./gradlew build
```

This will run all the steps defined in the `build.gradle.kts` file.


## Testing
If you are adding a new feature or bug fix please ensure there is proper test coverage.

## Pull Request Review
If you have a branch on your fork that is ready to be merged, please create a new pull request. The maintainers will review to make sure the above guidelines have been followed and if the changes are helpful to all library users, they will be merged.

## Releasing
The release process has been automated in Github Actions. Every merge into master is automatically added to the 
[draft release notes](https://github.com/navikt/mock-oauth2-server/releases) of the next version. Once the next 
version is ready to be released, simply publish the release with the version name as the title and tag and this 
will trigger to publishing process.

This project uses [semantic versioning](https://semver.org/) and does NOT prefix tags or release titles with `v` i.e. use `1.2.3` instead of `v1.2.3` 
