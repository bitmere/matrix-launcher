# Documentation index

Start with the root [README](../README.md), [house rules](../AGENTS.md),
[handover](HANDOVER.md) and [testing guide](TESTING.md).
Commands in documentation assume the repository root unless stated otherwise.

## Guides and architecture

- [Architecture](ARCHITECTURE.md) and [namespace compatibility](NAMESPACE.md)
- [Home setup](HOME-SETUP.md), [new TV installation](NEW-TV-INSTALL.md), [TV gotchas](tv-gotchas.md) and [migration](MIGRATION.md)
- [Search](SEARCH.md), [media apps](MEDIA-APPS.md), [Nuvio routing](NUVIO-ROUTING.md) and [add-on catalogs](ADDON-CATALOGS.md)
- [Wallpaper setup](WALLPAPERS.md) and [API text entry](API-ENTRY.md)
- [Row focus](ROW-FOCUS.md) and [development tools](../tools/README.md)

## Planning and AI references

- [OpenSpec changes](../openspec/changes/): proposals, tasks and specifications; preserved in their original locations.
- [Roadmap and TODO](TODO.md): preserved planning notes; reconcile with implemented features before starting work.
- [AI entry point](../llms.txt) and [detailed AI reference](llms-full.txt)

The detailed AI reference identifies 2.7.4/code 153. The roadmap separates implemented local tools from pending provider/device verification and stream checks, based on source review on 2026-11-16.
AGENTS.md takes precedence over conflicting historical workflow guidance.

## Release and validation records

Release files stay at their existing paths so changelog and historical links remain valid.
Dates, versions and scope inside each record identify the evidence it contains;
[VALIDATION.md](VALIDATION.md) contains current module-qualified test/lint commands and an unfilled results section for the overseer, followed by the historical 2.0.0 report (2026-11-04). Historical measurements do not establish current-release validation.

### Release notes

- [RELEASE-1.1.1-beta.7](archive/RELEASE-1.1.1-beta.7.md)
- [RELEASE-1.1](archive/RELEASE-1.1.md)
- [RELEASE-1.2](archive/RELEASE-1.2.md)
- [RELEASE-1.3](archive/RELEASE-1.3.md)
- [RELEASE-1.4](archive/RELEASE-1.4.md)
- [RELEASE-2.0](archive/RELEASE-2.0.md)
- [RELEASE-2.1](archive/RELEASE-2.1.md)
- [RELEASE-2.2](archive/RELEASE-2.2.md)
- [RELEASE-2.3](archive/RELEASE-2.3.md)
- [RELEASE-2.5](archive/RELEASE-2.5.md)

### Validation and device records

- [VALIDATION](VALIDATION.md)
- [VALIDATION-2.0.1](VALIDATION-2.0.1.md)
- [DEVICE-CHECK-2.1](DEVICE-CHECK-2.1.md)
- [TCL-SMART-TV-PRO-OPTIMIZATION](TCL-SMART-TV-PRO-OPTIMIZATION.md)

### Reviews and history

- [PROJECT-REVIEW](PROJECT-REVIEW.md)
- [CODE-REVIEW-53](CODE-REVIEW-53.md)
- [OPTIMIZATION-54](OPTIMIZATION-54.md)
- [AUDIT-PLATFORM](AUDIT-PLATFORM.md)
- [AUDIT-DATA](AUDIT-DATA.md)
- [UI-REVIEW](UI-REVIEW.md)
- [MOTION-REVIEW](MOTION-REVIEW.md)

See also the [changelog](../CHANGELOG.md), [cleanup scope](CLEANUP.md),
[source provenance](PROVENANCE.md) and [archived records](archive/).
