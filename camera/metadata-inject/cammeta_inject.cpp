// 相机元数据注入 shim（C1 / GOT 版）
//
// 背景：小米相机人像模式请求私有 Y16 深度格式（0x20363159 / 540422489），
// MTK HAL 只在 com.xiaomi.scaler.availableStreamConfigurations (0x9101001c)
// 里公布，标准 android.scaler.availableStreamConfigurations 里没有，
// 于是 cameraserver 的 roundBufferDimensionNearest() 找不到匹配项而拒绝建流。
//
// 做法：在 HAL 把 MTK 元数据转成 Android camera_metadata 时
// (mcam::android::convertMetadata)，如果这份 buffer 带有标准 scaler 流配置
// 和私有 Y16 条目（即静态能力），就把私有 key 里的 Y16 行原样补进标准 key。
// 只改静态能力，不碰每次请求/结果。
//
// 注入方式：本库被加为 libmtkcam_hal_android_device.so 的 DT_NEEDED。
// 该库的 GOT 里有两个 convertMetadata 的 JUMP_SLOT；由于 device.so 直接
// NEEDED common.so，符号预占（LD_PRELOAD / 普通 DT_NEEDED 插序）不可靠。
// 因此在 constructor 里解析 device.so 的 PT_DYNAMIC + .rela.plt，
// 找到对应 JUMP_SLOT 后直接改写 GOT 表项，绕过动态链接器的符号查找。
#include <android/log.h>
#include <dlfcn.h>
#include <elf.h>
#include <link.h>
#include <sys/mman.h>
#include <system/camera_metadata.h>
#include <unistd.h>

#include <cerrno>
#include <cstdint>
#include <cstring>
#include <vector>

#define LOG_TAG "CAMMETA"
#define LOGI(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// 小米私有 Y16 深度格式
static constexpr int32_t kY16VendorFormat = 540422489;  // 0x20363159
// com.xiaomi.scaler.availableStreamConfigurations
static constexpr uint32_t kXiaomiStreamConfigs = 0x9101001cu;

static constexpr const char* kDeviceLib = "libmtkcam_hal_android_device.so";
static constexpr const char* kCommonLib = "libmtkcam_hal_android_common.so";

// mcam::android::convertMetadata(NSCam::IMetadata const&, camera_metadata const*&)
static constexpr const char* kSymToAndroid =
    "_ZN4mcam7android15convertMetadataERKN5NSCam9IMetadataERPK15camera_metadata";
// mcam::android::convertMetadata(camera_metadata const* const&, NSCam::IMetadata&)
static constexpr const char* kSymFromAndroid =
    "_ZN4mcam7android15convertMetadataERKPK15camera_metadataRN5NSCam9IMetadataE";

static void* realSym(const char* lib, const char* sym) {
  void* h = dlopen(lib, RTLD_NOW | RTLD_NOLOAD);
  if (h == nullptr) h = dlopen(lib, RTLD_NOW);
  if (h == nullptr) {
    LOGI("dlopen(%s) failed: %s", lib, dlerror());
    return nullptr;
  }
  void* p = dlsym(h, sym);
  if (p == nullptr) LOGI("dlsym(%s) failed: %s", sym, dlerror());
  return p;
}

// 把 rows 追加到 int32 四元组 tag 上（流配置）
static bool appendInt32Rows(camera_metadata_t* dst, uint32_t tag,
                            const std::vector<int32_t>& rows) {
  camera_metadata_entry_t e;
  if (find_camera_metadata_entry(dst, tag, &e) != 0) return false;
  std::vector<int32_t> data(e.count + rows.size());
  if (e.count > 0) std::memcpy(data.data(), e.data.i32, e.count * sizeof(int32_t));
  std::memcpy(data.data() + e.count, rows.data(), rows.size() * sizeof(int32_t));
  const size_t idx = e.index;
  return update_camera_metadata_entry(dst, idx, data.data(), data.size(), nullptr) == 0;
}

// 把 rows 追加到 int64 四元组 tag 上（帧率/延迟时长）
static bool appendInt64Rows(camera_metadata_t* dst, uint32_t tag,
                            const std::vector<int64_t>& rows) {
  camera_metadata_entry_t e;
  if (find_camera_metadata_entry(dst, tag, &e) != 0) return false;
  std::vector<int64_t> data(e.count + rows.size());
  if (e.count > 0) std::memcpy(data.data(), e.data.i64, e.count * sizeof(int64_t));
  std::memcpy(data.data() + e.count, rows.data(), rows.size() * sizeof(int64_t));
  const size_t idx = e.index;
  return update_camera_metadata_entry(dst, idx, data.data(), data.size(), nullptr) == 0;
}

// 返回 true 表示替换了 buffer
static bool patchStaticMetadata(camera_metadata_t** pp) {
  camera_metadata_t* src = *pp;
  if (src == nullptr) return false;

  camera_metadata_ro_entry_t vend;
  if (find_camera_metadata_ro_entry(src, kXiaomiStreamConfigs, &vend) != 0) return false;
  if (vend.type != TYPE_INT32 || vend.count < 4) return false;

  std::vector<int32_t> rows;
  std::vector<int64_t> durRows;
  for (size_t i = 0; i + 3 < vend.count; i += 4) {
    if (vend.data.i32[i] != kY16VendorFormat) continue;
    rows.push_back(vend.data.i32[i]);
    rows.push_back(vend.data.i32[i + 1]);
    rows.push_back(vend.data.i32[i + 2]);
    rows.push_back(vend.data.i32[i + 3]);
    // 30fps 对应的帧时长（纳秒）
    durRows.push_back(static_cast<int64_t>(kY16VendorFormat));
    durRows.push_back(static_cast<int64_t>(vend.data.i32[i + 1]));
    durRows.push_back(static_cast<int64_t>(vend.data.i32[i + 2]));
    durRows.push_back(33333333);
  }
  if (rows.empty()) return false;

  // 必须是静态能力：带标准 scaler 流配置 tag
  camera_metadata_ro_entry_t sc;
  if (find_camera_metadata_ro_entry(src, ANDROID_SCALER_AVAILABLE_STREAM_CONFIGURATIONS,
                                    &sc) != 0) {
    return false;
  }
  for (size_t i = 0; i + 3 < sc.count; i += 4) {
    if (sc.data.i32[i] == kY16VendorFormat) return false;  // 已注入过
  }

  const size_t extraData = rows.size() * sizeof(int32_t) + durRows.size() * sizeof(int64_t) + 256;
  camera_metadata_t* dst = allocate_camera_metadata(get_camera_metadata_entry_capacity(src),
                                                    get_camera_metadata_data_capacity(src) + extraData);
  if (dst == nullptr) return false;
  if (append_camera_metadata(dst, src) != 0) {
    free_camera_metadata(dst);
    return false;
  }

  const bool ok =
      appendInt32Rows(dst, ANDROID_SCALER_AVAILABLE_STREAM_CONFIGURATIONS, rows) &&
      appendInt64Rows(dst, ANDROID_SCALER_AVAILABLE_MIN_FRAME_DURATIONS, durRows) &&
      appendInt64Rows(dst, ANDROID_SCALER_AVAILABLE_STALL_DURATIONS, durRows);
  if (!ok) {
    free_camera_metadata(dst);
    return false;
  }

  *pp = dst;
  LOGI("y16 injected into standard stream configurations (%zu rows)", rows.size() / 4);
  return true;
}

// ---------------------------------------------------------------- GOT patch

using ToAndroidFn = void* (*)(const void*, const void*&);
using FromAndroidFn = void* (*)(const void*&, void*);

extern "C" __attribute__((visibility("hidden"), noinline))
void* c1_meta_to_android(const void* in, const void*& out) {
  static ToAndroidFn fn;
  static int calls;
  if (fn == nullptr) fn = (ToAndroidFn)realSym(kCommonLib, kSymToAndroid);
  void* r = fn ? fn(in, out) : nullptr;
  if (calls < 4) {
    ++calls;
    LOGI("HOOK convertMetadata(IMetadata -> camera_metadata) out=%p", out);
  }
  camera_metadata_t* m = const_cast<camera_metadata_t*>(
      reinterpret_cast<const camera_metadata_t*>(out));
  if (m != nullptr && patchStaticMetadata(&m)) {
    out = reinterpret_cast<const void*>(m);
  }
  return r;
}

extern "C" __attribute__((visibility("hidden"), noinline))
void* c1_meta_from_android(const void*& in, void* out) {
  static FromAndroidFn fn;
  static int calls;
  if (fn == nullptr) fn = (FromAndroidFn)realSym(kCommonLib, kSymFromAndroid);
  if (calls < 2) {
    ++calls;
    LOGI("HOOK convertMetadata(camera_metadata -> IMetadata) in=%p", in);
  }
  return fn ? fn(in, out) : nullptr;
}

// 兼容旧的符号预占方案：保留同名导出符号。GOT patch 仍使用 hidden 实现，
// 避免取地址时被动态链接器预占成 common.so 里的真函数。
extern "C" void* shim_convert_metadata(const void*, const void*&)
    __asm__("_ZN4mcam7android15convertMetadataERKN5NSCam9IMetadataERPK15camera_metadata");
extern "C" void* shim_convert_metadata(const void* in, const void*& out) {
  return c1_meta_to_android(in, out);
}

namespace {

struct ModuleInfo {
  uintptr_t base = 0;
  const ElfW(Phdr)* phdr = nullptr;
  size_t phnum = 0;
  const char* name = nullptr;
};

struct FindCtx {
  const char* soname;
  ModuleInfo out;
};

int findModuleCallback(struct dl_phdr_info* info, size_t, void* data) {
  auto* ctx = static_cast<FindCtx*>(data);
  if (info->dlpi_name == nullptr || info->dlpi_name[0] == '\0') return 0;
  const char* slash = std::strrchr(info->dlpi_name, '/');
  const char* base = slash ? slash + 1 : info->dlpi_name;
  if (std::strcmp(base, ctx->soname) != 0) return 0;
  ctx->out.base = static_cast<uintptr_t>(info->dlpi_addr);
  ctx->out.phdr = info->dlpi_phdr;
  ctx->out.phnum = info->dlpi_phnum;
  ctx->out.name = info->dlpi_name;
  return 1;
}

bool findModule(const char* soname, ModuleInfo* out) {
  FindCtx ctx{soname, {}};
  dl_iterate_phdr(findModuleCallback, &ctx);
  if (ctx.out.base == 0 || ctx.out.phdr == nullptr) return false;
  *out = ctx.out;
  return true;
}

// DT_* 里的指针在 bionic 里通常已重定位成绝对地址；兼容仍是相对偏移的情况。
uintptr_t fixPtr(uintptr_t base, ElfW(Addr) v) {
  if (v >= base && v < base + (1ULL << 32)) return static_cast<uintptr_t>(v);
  return base + static_cast<uintptr_t>(v);
}

bool patchSlot(uintptr_t slot, void* target, const char* sym) {
  const long pageSize = sysconf(_SC_PAGESIZE);
  if (pageSize <= 0) return false;
  const uintptr_t page = slot & ~static_cast<uintptr_t>(pageSize - 1);
  if (mprotect(reinterpret_cast<void*>(page), static_cast<size_t>(pageSize),
               PROT_READ | PROT_WRITE) != 0) {
    LOGI("mprotect(RW) %p failed: %s", reinterpret_cast<void*>(page), std::strerror(errno));
    return false;
  }
  void** p = reinterpret_cast<void**>(slot);
  void* old = *p;
  *p = target;
  __builtin___clear_cache(reinterpret_cast<char*>(slot),
                          reinterpret_cast<char*>(slot) + sizeof(void*));
  if (mprotect(reinterpret_cast<void*>(page), static_cast<size_t>(pageSize), PROT_READ) != 0) {
    LOGI("mprotect(R) %p failed: %s", reinterpret_cast<void*>(page), std::strerror(errno));
  }
  LOGI("GOT patch %s slot=%p old=%p new=%p", sym, reinterpret_cast<void*>(slot), old, target);
  return true;
}

struct PatchTarget {
  const char* sym;
  void* fn;
};

int patchModuleGots(const char* soname, const PatchTarget* targets, size_t targetCount) {
  ModuleInfo mod;
  if (!findModule(soname, &mod)) {
    LOGI("module %s not found", soname);
    return -1;
  }
  LOGI("module %s base=%p phnum=%zu", soname, reinterpret_cast<void*>(mod.base), mod.phnum);

  const ElfW(Dyn)* dyn = nullptr;
  for (size_t i = 0; i < mod.phnum; ++i) {
    if (mod.phdr[i].p_type == PT_DYNAMIC) {
      dyn = reinterpret_cast<const ElfW(Dyn)*>(mod.base + mod.phdr[i].p_vaddr);
      break;
    }
  }
  if (dyn == nullptr) {
    LOGI("PT_DYNAMIC not found in %s", soname);
    return -1;
  }

  ElfW(Addr) symtab = 0;
  ElfW(Addr) strtab = 0;
  ElfW(Addr) jmprel = 0;
  size_t pltrelsz = 0;
  ElfW(Sxword) pltrel = DT_RELA;
  for (const ElfW(Dyn)* d = dyn; d->d_tag != DT_NULL; ++d) {
    switch (d->d_tag) {
      case DT_SYMTAB: symtab = d->d_un.d_ptr; break;
      case DT_STRTAB: strtab = d->d_un.d_ptr; break;
      case DT_JMPREL: jmprel = d->d_un.d_ptr; break;
      case DT_PLTRELSZ: pltrelsz = d->d_un.d_val; break;
      case DT_PLTREL: pltrel = d->d_un.d_val; break;
      default: break;
    }
  }
  if (symtab == 0 || strtab == 0 || jmprel == 0 || pltrelsz == 0) {
    LOGI("dynamic reloc tags missing: symtab=%llx strtab=%llx jmprel=%llx size=%zu",
         static_cast<unsigned long long>(symtab), static_cast<unsigned long long>(strtab),
         static_cast<unsigned long long>(jmprel), pltrelsz);
    return -1;
  }
  if (pltrel != DT_RELA) {
    LOGI("unexpected DT_PLTREL=%lld", static_cast<long long>(pltrel));
    return -1;
  }

  const auto* rela = reinterpret_cast<const ElfW(Rela)*>(fixPtr(mod.base, jmprel));
  const auto* syms = reinterpret_cast<const ElfW(Sym)*>(fixPtr(mod.base, symtab));
  const char* strs = reinterpret_cast<const char*>(fixPtr(mod.base, strtab));
  const size_t count = pltrelsz / sizeof(ElfW(Rela));

  int patched = 0;
  for (size_t i = 0; i < count; ++i) {
    if (ELF64_R_TYPE(rela[i].r_info) != R_AARCH64_JUMP_SLOT) continue;
    const size_t symIndex = static_cast<size_t>(ELF64_R_SYM(rela[i].r_info));
    const char* name = strs + syms[symIndex].st_name;
    for (size_t t = 0; t < targetCount; ++t) {
      if (std::strcmp(name, targets[t].sym) != 0) continue;
      if (patchSlot(mod.base + rela[i].r_offset, targets[t].fn, targets[t].sym)) ++patched;
    }
  }
  LOGI("%s: patched %d GOT slot(s)", soname, patched);
  return patched;
}

__attribute__((constructor)) void onLoad() {
  LOGI("shim loaded into pid %d", getpid());
  const PatchTarget targets[] = {
      {kSymToAndroid, reinterpret_cast<void*>(&c1_meta_to_android)},
      {kSymFromAndroid, reinterpret_cast<void*>(&c1_meta_from_android)},
  };
  patchModuleGots(kDeviceLib, targets, sizeof(targets) / sizeof(targets[0]));
}

}  // namespace
