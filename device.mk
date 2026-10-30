#
# Copyright (C) 2025 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from mt6895-common
$(call inherit-product, device/xiaomi/mt6895-common/mt6895.mk)

# Display
$(call soong_config_set_bool,surfaceflinger,register_displayservice,true)

# MIUI Camera
$(call inherit-product, vendor/xiaomi/miuicamera-yuechu/device.mk)

# Audio (device-specific Dolby effect stack)
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/audio/audio_effects.xml:$(TARGET_COPY_OUT_VENDOR)/etc/audio_effects.xml

# Dolby (DAP engine). Controller app; effect libs (libswdap/libswvqe/libswgamedap)
# and the dms@2.0 service come from proprietary-files.txt, registered in audio_effects.xml.
# TODO: The XiaomiDolby controller app is not provided by any repository yet, so
# referencing it here fails the build. Commented out to keep the build complete;
# restore once the Dolby integration is finished.
# PRODUCT_PACKAGES += \
#     XiaomiDolby

# libdlbdsservice.so (in the dms daemon) pulls in libsqlite -> libandroidicu ->
# libicuuc/libicui18n. libsqlite is built from source (external/sqlite) and
# installed to /vendor automatically (it is a shared_libs dep of libdlbdsservice).
# But libandroidicu/libicuuc/libicui18n ship only in the com.android.i18n APEX,
# which the A16 vendor linker namespace cannot see, so the dms vendor daemon
# can't resolve them and won't start. Vendor these three as prebuilt shared
# libraries (prebuilts/dolby_icu/Android.bp); they reuse the external/icu module
# names via stem. ICU data (icudt76l.dat) is still read from the APEX at runtime.
PRODUCT_PACKAGES += \
    libandroidicu-dolby-vendor \
    libicuuc-dolby-vendor \
    libicui18n-dolby-vendor

# ConsumerIR
PRODUCT_PACKAGES += \
    android.hardware.ir-service.example

# Dynamic Partitions (device fastboot package)
PRODUCT_FASTBOOT_TEMPLATE_ZIP := $(LOCAL_PATH)/prebuilts/fastboot.zip

# Media (device-specific Dolby codec config)
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/media/media_codecs_dolby_audio.xml:$(TARGET_COPY_OUT_VENDOR)/etc/media_codecs_dolby_audio.xml

# NFC
PRODUCT_PACKAGES += \
    android.hardware.nfc-service.nxp \
    com.android.nfc_extras \
    Tag

PRODUCT_COPY_FILES += \
        frameworks/native/data/etc/android.hardware.nfc.ese.xml:$(TARGET_COPY_OUT_ODM)/etc/permissions/android.hardware.nfc.ese.xml \
        frameworks/native/data/etc/android.hardware.nfc.hce.xml:$(TARGET_COPY_OUT_ODM)/etc/permissions/android.hardware.nfc.hce.xml \
        frameworks/native/data/etc/android.hardware.nfc.hcef.xml:$(TARGET_COPY_OUT_ODM)/etc/permissions/android.hardware.nfc.hcef.xml \
        frameworks/native/data/etc/android.hardware.nfc.uicc.xml:$(TARGET_COPY_OUT_ODM)/etc/permissions/android.hardware.nfc.uicc.xml \
        frameworks/native/data/etc/android.hardware.nfc.xml:$(TARGET_COPY_OUT_ODM)/etc/permissions/android.hardware.nfc.xml \
        frameworks/native/data/etc/android.hardware.se.omapi.ese.xml:$(TARGET_COPY_OUT_ODM)/etc/permissions/android.hardware.se.omapi.ese.xml \
        frameworks/native/data/etc/android.hardware.se.omapi.uicc.xml:$(TARGET_COPY_OUT_ODM)/etc/permissions/android.hardware.se.omapi.uicc.xml \
        frameworks/native/data/etc/com.android.nfc_extras.xml:$(TARGET_COPY_OUT_ODM)/etc/permissions/com.android.nfc_extras.xml \
        frameworks/native/data/etc/com.nxp.mifare.xml:$(TARGET_COPY_OUT_ODM)/etc/permissions/com.nxp.mifare.xml

# Overlays
PRODUCT_PACKAGES += \
    FrameworksResOverlayYuechu \
    NfcOverlayYuechu \
    SettingsProviderOverlayYuechu \
    SettingsOverlayYuechu \
    SystemUIResOverlayYuechu \
    WifiResOverlayYuechu

# Parts
PRODUCT_PACKAGES += \
    XiaomiParts \

# Remove unwanted packages
PRODUCT_PACKAGES += \
    RemovePkgs

# Rootdir
PRODUCT_PACKAGES += \
    init.project.rc \
    init.yuechu.rc

# Soong namespaces
PRODUCT_SOONG_NAMESPACES += \
    $(LOCAL_PATH)

# Touchscreen firmware, also needed in recovery where /vendor is not mounted
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/../../../vendor/xiaomi/yuechu/proprietary/vendor/firmware/s3907_xiaomi_m9_spi.img:$(TARGET_COPY_OUT_VENDOR_RAMDISK)/vendor/firmware/s3907_xiaomi_m9_spi.img

# Inherit the proprietary files
$(call inherit-product, vendor/xiaomi/yuechu/yuechu-vendor.mk)

# Camera decision configs (read by libcom.xiaomi.mizone; missing from the blob set)
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/camera/xiaomi/MiCamCPUControl.xml:$(TARGET_COPY_OUT_VENDOR)/etc/camera/xiaomi/MiCamCPUControl.xml \
    $(LOCAL_PATH)/camera/xiaomi/dualbokehjpegsnapshot.json:$(TARGET_COPY_OUT_VENDOR)/etc/camera/xiaomi/dualbokehjpegsnapshot.json \
    $(LOCAL_PATH)/camera/xiaomi/ecocloudinfo.json:$(TARGET_COPY_OUT_VENDOR)/etc/camera/xiaomi/ecocloudinfo.json \
    $(LOCAL_PATH)/camera/xiaomi/ecoplatforminfo.json:$(TARGET_COPY_OUT_VENDOR)/etc/camera/xiaomi/ecoplatforminfo.json \
    $(LOCAL_PATH)/camera/xiaomi/frontsupernightsnapshotjpeg.json:$(TARGET_COPY_OUT_VENDOR)/etc/camera/xiaomi/frontsupernightsnapshotjpeg.json \
    $(LOCAL_PATH)/camera/xiaomi/mivisettings.json:$(TARGET_COPY_OUT_VENDOR)/etc/camera/xiaomi/mivisettings.json \
    $(LOCAL_PATH)/camera/xiaomi/overridesettings.json:$(TARGET_COPY_OUT_VENDOR)/etc/camera/xiaomi/overridesettings.json \
    $(LOCAL_PATH)/camera/xiaomi/rearsupernightsnapshot.json:$(TARGET_COPY_OUT_VENDOR)/etc/camera/xiaomi/rearsupernightsnapshot.json \
    $(LOCAL_PATH)/camera/xiaomi/settings.json:$(TARGET_COPY_OUT_VENDOR)/etc/camera/xiaomi/settings.json \
    $(LOCAL_PATH)/camera/xiaomi/thirdpartydualbokehyuvsnapshot.json:$(TARGET_COPY_OUT_VENDOR)/etc/camera/xiaomi/thirdpartydualbokehyuvsnapshot.json \
    $(LOCAL_PATH)/camera/xiaomi/thirdpartyjpegsnapshot.json:$(TARGET_COPY_OUT_VENDOR)/etc/camera/xiaomi/thirdpartyjpegsnapshot.json \
    $(LOCAL_PATH)/camera/xiaomi/thirdpartyrawsupernightjpeg.json:$(TARGET_COPY_OUT_VENDOR)/etc/camera/xiaomi/thirdpartyrawsupernightjpeg.json \
    $(LOCAL_PATH)/camera/xiaomi/thirdpartyrawsupernightyuv.json:$(TARGET_COPY_OUT_VENDOR)/etc/camera/xiaomi/thirdpartyrawsupernightyuv.json

# Camera resources (AI models, filter LUTs, VIDHANCE license, ...) that the
# generated vendor blob list does not carry.
$(call inherit-product, vendor/xiaomi/yuechu/camera-resources.mk)

# Camera metadata inject: publish the vendor Y16 depth format in the
# standard scaler keys so cameraserver accepts the portrait stream.
PRODUCT_PACKAGES += \
    libcammeta_inject

# Lineage Gallery2 + a thin com.miui.gallery wrapper so the camera thumbnail
# opens the system gallery instead of the missing MIUI Gallery.
PRODUCT_PACKAGES += \
    Gallery2Patched \
    MiuiGalleryStub

# Not well-formed XML, so installed via prebuilt_etc instead of a copy rule.
PRODUCT_PACKAGES += \
    gma_custom_xml
