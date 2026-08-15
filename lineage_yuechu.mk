#
# Copyright (C) 2025 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit some common Lineage stuff.
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

# Inherit from yuechu device
$(call inherit-product, device/xiaomi/yuechu/device.mk)

# Keep the unrelated Pearl device tree and its vendor tree out of this product's Soong graph.
PRODUCT_SOURCE_ROOT_DIRS += -device/xiaomi/pearl -vendor/xiaomi/pearl

PRODUCT_DEVICE := yuechu
PRODUCT_NAME := lineage_yuechu
PRODUCT_BRAND := Xiaomi
PRODUCT_MODEL := 23046PNC9C
PRODUCT_MANUFACTURER := Xiaomi

PRODUCT_SYSTEM_NAME := yuechu
PRODUCT_SYSTEM_DEVICE := yuechu
PRODUCT_CHARACTERISTICS := nosdcard

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

# ROM Flags
WITH_GMS := false

# Animation
PERF_ANIM_OVERRIDE := true

# Bootanimation Resolution.
TARGET_BOOT_ANIMATION_RES := 1920

# Enable DM file pre-opting to reduce first boot time
# Note that this may significantly increase your compilation time!
PRODUCT_DEX_PREOPT_GENERATE_DM_FILES := true

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildFingerprint="Redmi/yuechu/yuechu:15/AP3A.240905.015.A2/OS3.0.10.0.VMICNXM:user/release-keys"

# Set BUILD_FINGERPRINT variable to be picked up by both system and vendor build.prop
BUILD_FINGERPRINT := Redmi/yuechu/yuechu:15/AP3A.240905.015.A2/OS3.0.10.0.VMICNXM:user/release-keys