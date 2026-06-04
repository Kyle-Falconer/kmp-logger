# Releasing

## Production Releases

1. Checkout `origin/main`.
2. Update the `CHANGELOG.md` file with the changes of this release (the format is based on
   [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)).
   * Copy the template for the next unreleased version at the top.
   * Delete unused sections in the new release.
3. Update the version in `gradle.properties` and remove the `-SNAPSHOT` suffix.
4. Commit the changes and create a tag:
   ```
   git commit -am "Releasing 0.1.0."
   git tag 0.1.0
   ```
5. Publish the artifacts:
   ```
   ./gradlew publishAllPublicationsToMavenCentralRepository
   ```
6. Update the version in `gradle.properties` and add the `-SNAPSHOT` suffix.
7. Commit the change:
   ```
   git commit -am "Prepare next development version."
   ```
8. Push the two commits:
   ```
   git push && git push --tags
   ```

## Snapshot Releases

1. Verify in `gradle.properties` that the version has a `-SNAPSHOT` suffix.
2. Publish the artifacts:
   ```
   ./gradlew publishAllPublicationsToMavenCentralRepository
   ```

## Installing in Maven Local

```
./gradlew publishToMavenLocal
```
