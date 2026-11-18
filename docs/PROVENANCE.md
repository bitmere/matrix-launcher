# Source provenance

Independent Compose repository, initialized from the Matrix Compose experiment at commit 980a6e4 (v1.0.1-beta.8), plus the in-progress modular migration on 2026-11-03.

Java baseline remains in personal-home-tv at 55a853a (v1.0.1-beta.4). The original repository and Compose worktree are preserved. No Git object store or remote is shared with this repository.

Based in part on Gathin23/cleanyourtv under the MIT license. Retained backend/device policies preserve their upstream attribution. See LICENSE and the font/icon licenses.

## Matrix implementation identity

The active app and test code now uses com.matrix.launcher paths and namespace; reusable core stays com.matrix.launcher.core. This naming change does not remove upstream attribution or change ownership of retained upstream code. Four minimal legacy service bridges and a Home activity-alias preserve Android selections across installed updates. The installed app ID/private settings are stable data identities. See [namespace compatibility](NAMESPACE.md).
