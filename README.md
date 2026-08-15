# Device tree for Xiaomi Civi 3 (yuechu)
The Xiaomi Civi 3 (codenamed _"yuechu"_) is a high-end, mid-range smartphone from Xiaomi.

It was announced on 2023, May 29. Release date was 2023, June 01.

## Device specifications

Basic   | Spec Sheet
-------:|:-------------------------
CPU     | Octa-core (1x3.1 GHz Cortex-A78 & 3x3.0 GHz Cortex-A78 & 4x2.0 GHz Cortex-A55)
Chipset | Mediatek Dimensity 8200-Ultra (4 nm)
GPU     | Mali-G610 MC6
Memory  | 8/12 GB RAM
Shipped Android Version | Android 13, MIUI 14 up Android 15, Hyper OS
Storage | 128/256/512 (UFS 3.1)
Battery | Non-removable Li-Po 5080 mAh battery
Display | 1080 x 2460 pixels, 6.6 inches, Dobly Vision, IPS LCD

## Device picture
![Redmi Note 12T Pro](https://cdn.cnbj1.fds.api.mi-img.com/nr-pub/202305291422_e96776c7e1e35cebb454457c3344d3cd.png)

## Device tree structure

This tree inherits the MT6895 SoC-common layer:

- `device/xiaomi/mt6895-common` (board config + product makefile + shared configs/sepolicy/native sources)
- `vendor/xiaomi/mt6895-common` (SoC-common proprietary blobs, extracted from yuechu OS3.0.10.0)

This tree keeps the yuechu-specific parts: Dolby DAP engine (blobs, sepolicy,
`audio_effects.xml`, `media_codecs_dolby_audio.xml`, `XiaomiDolby`), NFC, IR,
Radio/IMS, keymint/MITEE/TEE stack, camera stack and touch firmware.
Run `./extract-files.py` inside `device/xiaomi/mt6895-common` first, then
`./extract-files.py` here, to regenerate the vendor trees.
