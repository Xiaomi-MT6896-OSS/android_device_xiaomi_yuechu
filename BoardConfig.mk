#
# Copyright (C) 2025 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

DEVICE_PATH := device/xiaomi/yuechu

# Inherit from mt6895-common
include device/xiaomi/mt6895-common/BoardConfigCommon.mk

# Use yuechu's USB gadget configuration instead of the generic MediaTek rc.
SOONG_CONFIG_NAMESPACES += mediatek_gadget
SOONG_CONFIG_mediatek_gadget += use_custom_usb_gadget_rc
SOONG_CONFIG_mediatek_gadget_use_custom_usb_gadget_rc := true

# Bootloader
TARGET_BOOTLOADER_BOARD_NAME := yuechu

# UDFPS
TARGET_HAS_UDFPS := true

# Display
TARGET_SCREEN_DENSITY := 440

# Kernel
TARGET_KERNEL_CONFIG := \
	gki_defconfig \
	vendor/xiaomi_mt6895.config \
	vendor/yuechu.config

# Kernel modules
BOARD_VENDOR_KERNEL_MODULES_LOAD := $(strip $(shell cat $(DEVICE_PATH)/modules/modules.load))
BOARD_VENDOR_RAMDISK_RECOVERY_KERNEL_MODULES_LOAD := $(strip $(shell cat $(DEVICE_PATH)/modules/modules.load.recovery))
BOARD_VENDOR_RAMDISK_KERNEL_MODULES_LOAD := $(strip $(shell cat $(DEVICE_PATH)/modules/modules.load.vendor_boot))
BOOT_KERNEL_MODULES := $(BOARD_VENDOR_RAMDISK_RECOVERY_KERNEL_MODULES_LOAD) $(BOARD_VENDOR_RAMDISK_KERNEL_MODULES_LOAD)

# Properties
TARGET_SYSTEM_PROP += $(DEVICE_PATH)/system.prop
TARGET_VENDOR_PROP += $(DEVICE_PATH)/vendor.prop

# Sepolicy
BOARD_VENDOR_SEPOLICY_DIRS += $(DEVICE_PATH)/sepolicy/vendor

# VINTF
DEVICE_FRAMEWORK_COMPATIBILITY_MATRIX_FILE += \
    $(DEVICE_PATH)/vintf/dolby_framework_matrix.xml

# Inherit the proprietary files
include vendor/xiaomi/yuechu/BoardConfigVendor.mk
include vendor/xiaomi/miuicamera-yuechu/BoardConfig.mk
