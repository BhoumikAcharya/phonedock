# CLAUDE.md

Start by reading [AGENTS.md](AGENTS.md). It is the handover document for this
project: phone facts, toolchain paths, build and install commands, code map,
the user's preferences, and the gotchas from previous sessions.

Rules that apply in every session:

- Never relock the bootloader or flash partitions on the phone.
- Before any adb or fastboot command that writes to or erases the phone beyond
  `adb install`, show the command and wait for the user's confirmation.
- Do not disable or uninstall system packages without asking.
- Work in this repo's `dock/` directory; it is the source of truth.
- Push to GitHub only when the user asks.
