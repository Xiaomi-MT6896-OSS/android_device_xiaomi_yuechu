# Camera metadata inject (Y16 depth)

The MIUI camera asks camera 5 for the private Y16 depth format
`0x20363159` (540422489).  MTK only publishes it in the Xiaomi vendor
scaler key `com.xiaomi.scaler.availableStreamConfigurations`
(`0x9101001c`), while AOSP cameraserver validates against
`android.scaler.availableStreamConfigurations`.  Portrait mode therefore
fails with:

```
createConfiguredSurface: Camera 5: No supported stream configurations
with format 0x20363159
```

`libcammeta_inject.so` is loaded as a `DT_NEEDED` of
`libmtkcam_hal_android_device.so` and rewrites that library's GOT slots
for `mcam::android::convertMetadata` in its constructor.  The hook then
copies the vendor Y16 rows into the standard scaler tags (stream
configurations, min frame durations and stall durations) when the
converted buffer is the static camera capability.

A normal `DT_NEEDED`/`LD_PRELOAD` interposition is not enough here: the
MTK device library is loaded in its own linker namespace and directly
depends on the common library that defines the symbol, so symbol lookup
finds the common implementation first.  The GOT write bypasses lookup.

## Build integration

- This directory provides `libcammeta_inject` (vendor, 64-bit).
- `device/xiaomi/yuechu/device.mk` adds it to `PRODUCT_PACKAGES`.
- `vendor/xiaomi/yuechu/Android.bp` has a `genrule` that copies
  `libmtkcam_hal_android_device.so` and runs
  `vendor/xiaomi/yuechu/cammeta/patch_cammeta.sh` to add the
  `DT_NEEDED` entry.  The script is idempotent and checks the dynamic
  section before patching.
- The patched library keeps the same SONAME, so the generated prebuilt
  module installs it at `/vendor/lib64/mt6895/` as before.

If the vendor blob list is regenerated, re-apply the `genrule` and the
`libcammeta_inject` dependency in `vendor/xiaomi/yuechu/Android.bp`.

## Verification

```
adb shell dumpsys media.camera --camera-id 5 | grep -A... \
  'android.scaler.availableStreamConfigurations'
```

The standard key must contain rows starting with `540422489`.
`logcat -s CAMMETA:*` shows the GOT slots being patched and the Y16
rows being injected.

The hook depends on the mangled symbol
`_ZN4mcam7android15convertMetadataERKN5NSCam9IMetadataERPK15camera_metadata`
and its sibling.  If a future MTK blob renames them, the GOT patch
silently finds nothing; check the CAMMETA log after every blob update.
