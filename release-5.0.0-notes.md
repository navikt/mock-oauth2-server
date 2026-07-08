## What's Changed
- ci: migrate release-drafter config away from deprecated fields (#962) @ybelMekk
- docs: add migration guidance for 5.0.0 claim precedence change  (#961) @ybelMekk

## 🚀 Features

- feat: support interactive login with requestMapping claim selection (#944) @ybelMekk

## ⚠️ Breaking Changes

- feat: support interactive login with requestMapping claim selection (#944) @ybelMekk

**Before:** claims submitted on the login page could overwrite claims from matching `requestMappings`.
**Now:** claims from matching `requestMappings` take precedence on key collisions.
Login-page claims still apply for keys not already set by the mapping.

👉 See full migration details: [MIGRATION.md](https://github.com/navikt/mock-oauth2-server/blob/master/MIGRATION.md#migrating-to-500)

## 🧰 Maintenance

- chore: apply formatting-only cleanup (#952) @ybelMekk

## ⬆️ Dependency upgrades

- test: fix WebTestClient setup in resource server example (#960) @ybelMekk
- build: align kotlin plugins to 2.4.0 without breaking consumer target (#959) @ybelMekk
- chore(deps): bump org.yaml:snakeyaml from 2.5 to 2.6 (#958) @[dependabot[bot]](https://github.com/apps/dependabot)
- chore(deps): bump ktorVersion from 3.4.3 to 3.5.0 (#957) @[dependabot[bot]](https://github.com/apps/dependabot)
- chore(deps): bump the github-actions group with 3 updates (#955) @[dependabot[bot]](https://github.com/apps/dependabot)
- chore(deps): bump com.fasterxml.woodstox:woodstox-core from 7.1.1 to 7.2.1 (#954) @[dependabot[bot]](https://github.com/apps/dependabot)

