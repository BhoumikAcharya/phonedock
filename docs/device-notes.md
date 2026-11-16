# Device notes: Xiaomi Redmi Note 5 (India) / Redmi 5 Plus, codename `vince`

These are the exact values recorded while following the guide. Yours will
differ slightly; the important thing is that `vince` shows up everywhere.

| Item | Value |
| --- | --- |
| Marketing name | Redmi Note 5 (India) / Redmi 5 Plus (global) |
| Codename | `vince` (must NOT be `whyred`, that is the Note 5 Pro) |
| SoC | Snapdragon 625 (msm8953) |
| RAM | 4 GB (3,663,972 kB reported) |
| Stock firmware at start | MIUI V11.0.2.0.OEGMIXM, Android 8.1 |
| Baseband at start | 953_GEN_PACK-1.174284.1.194887.2 |
| Bootloader | Unlocked with the official Mi Unlock tool, never relocked |
| Fingerprint sensor | Goodix (`goodix_fp`) |

## ROM used

Evolution X 10.9, Android 15, official build with Google apps included.

| File | Value |
| --- | --- |
| ROM zip | `EvolutionX-15.0-20251001-vince-10.9-Official.zip` (1,686,334,000 bytes) |
| MD5 | `69efdc32395b1e23966ed4e7b666c255` |
| SHA-256 | `278be67bcac471a4a8bb2210aac7f7537b6c45063c2a81edda39f72e213212a9` |
| Recovery | `recovery.img` from the Evolution X CDN, 27,013,120 bytes |
| Device page | https://evolution-x.org/device/vince |
| Build metadata | https://raw.githubusercontent.com/Evolution-X/OTA/vic/builds/vince.json |

The build is marked "no longer maintained". It works fine for a dock, but do
not expect updates.

## After flashing

| Item | Value |
| --- | --- |
| Build | `lineage_vince-userdebug 15 BP1A.250505.005` |
| Screen | 1080 x 2160, density 400 |
| Known broken | Fingerprint HAL crashes (see README troubleshooting) |
