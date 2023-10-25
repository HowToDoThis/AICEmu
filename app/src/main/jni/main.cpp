
#include "main.h"
#include <dobby.h>
#include <sys/mman.h>

#define _uintval(p)               reinterpret_cast<uintptr_t>(p)
#define _ptr(p)                   reinterpret_cast<void *>(p)
#define _align_up(x, n)           (((x) + ((n) - 1)) & ~((n) - 1))
#define _align_down(x, n)         ((x) & -(n))
#define _page_size                4096
#define _page_align(n)            _align_up(static_cast<uintptr_t>(n), _page_size)
#define _ptr_align(x)             _ptr(_align_down(reinterpret_cast<uintptr_t>(x), _page_size))
#define _make_rwx(p, n)           ::mprotect(_ptr_align(p), \
                                              _page_align(_uintval(p) + n) != _page_align(_uintval(p)) ? _page_align(n) + _page_size : _page_align(n), \
                                              PROT_READ | PROT_WRITE | PROT_EXEC)

char buff[30];
char pmm_str[30];
char target_pmm[8] = {0x00, 0xf1, 0x00, 0x00, 0x00, 0x01, 0x43, 0x00};

void *new_func(u_int8_t tlv_list_len, u_int8_t *p_tlv_list, int app_init) {

    // lovely android 10 arm64 :3
    // read more: https://source.android.com/docs/core/tests/debug/native-crash?hl=zh-cn#xom
    _make_rwx(p_tlv_list, _page_size);

    bool found = false;

    // if (tlv_list_len == 0x1d) { // hardcoded arg pattern
    // 40 0a [syscode] [IDm] 53 02 01 00 55 01 01 51 08 [PMm]
    // if (tlv_list_len == 0x1b) { // another type hardcoded arg pattern
    // 40 12 [syscode] [IDm] [PMm] 53 02 01 00 55 01 01

        // handmade hexdump
        //for (int i = 0x0; i < 0x10; ++i)
        //    sprintf(buff + i * 3, "%02x ", *(char *)(p_tlv_list + i));
        //__android_log_print(6, "AICEmu-pmmtool", "[%x]: %s", p_tlv_list, buff);
        //for (int i = 0x0; i < 0x10; ++i)
        //    sprintf(buff + i * 3, "%02x ", *(char *)(p_tlv_list + 0x10 + i));
        //__android_log_print(6, "AICEmu-pmmtool", "[%x]: %s", p_tlv_list + 0x10, buff);

        for (int i = 0x0; i < 0x20; ++i) {
            // i know kinda stupid, but easier to read :(
            auto type = *(p_tlv_list + i);
            auto len = *(p_tlv_list + i + 1);
            auto p_value = p_tlv_list + i + 2;

            // 51 = NFC_PMID_LF_T3T_PMM
            // 08 = NCI_PARAM_LEN_LF_T3T_PMM
            // look for 51 08 (set pmm command) for type 0x1d
            if (type == 0x51 && len == 0x08) {
                __android_log_print(6, "AICEmu-pmmtool", "hook _Z23nfa_dm_check_set_confighPhb arg0->%x arg1->%x", tlv_list_len, p_tlv_list);

                __android_log_print(6, "AICEmu-pmmtool", "Set Pmm Found... hooking", pmm_str);

                found = true;

                for (int j = 0; j < 8; ++j)
                    sprintf(pmm_str + j * 3, "%02x ", *(char *)(p_value + j));
                __android_log_print(6, "AICEmu-pmmtool", "[1] old PMm: %s", pmm_str);

                // set
                for (int j = 0; j < 8; ++j)
                    *(char *)(p_value + j) = target_pmm[j];

                for (int j = 0; j < 8; ++j)
                    sprintf(pmm_str + j * 3, "%02x ", *(char *)(p_value + j));
                __android_log_print(6, "AICEmu-pmmtool", "[1] new PMm: %s", pmm_str);
            }

            // look for FF FF FF FF FF FF FF FF (pmm itself)
            if (*(char *)(p_tlv_list + i) == 0xff && *(char *)(p_tlv_list + i + 1) == 0xff
                && *(char *)(p_tlv_list + i + 2) == 0xff && *(char *)(p_tlv_list + i + 3) == 0xff
                && *(char *)(p_tlv_list + i + 4) == 0xff && *(char *)(p_tlv_list + i + 5) == 0xff
                && *(char *)(p_tlv_list + i + 6) == 0xff && *(char *)(p_tlv_list + i + 7) == 0xff) {

                found = true;

                for (int j = 0; j < 8; ++j)
                    sprintf(pmm_str + j * 3, "%02x ", *(char *)(p_tlv_list + i  + j));
                __android_log_print(6, "AICEmu-pmmtool", "[2] old PMm: %s", pmm_str);

                // set
                for (int j = 0; j < 8; ++j)
                    *(char *)(p_tlv_list + i + j) = target_pmm[j];

                for (int j = 0; j < 8; ++j)
                    sprintf(pmm_str + j * 3, "%02x ", *(char *)(p_tlv_list + i + j));
                __android_log_print(6, "AICEmu-pmmtool", "[2] new PMm: %s", pmm_str);
            }
        }

    void *result = old_func(tlv_list_len, p_tlv_list, app_init);

    if (found) {
        __android_log_print(6, "AICEmu-pmmtool", "hook result -> %x", result);

        // double check?
        for (int i = 0x0; i < 0x20; ++i) {
            // i know kinda stupid, but easier to read :(
            auto type = *(p_tlv_list + i);
            auto len = *(p_tlv_list + i + 1);
            auto p_value = p_tlv_list + i + 2;

            // 51 = NFC_PMID_LF_T3T_PMM
            // 08 = NCI_PARAM_LEN_LF_T3T_PMM
            // look for 51 08 (set pmm command) for type 0x1d
            if (type == 0x51 && len == 0x08) {
                for (int j = 0; j < 8; ++j)
                    sprintf(pmm_str + j * 3, "%02x ", *(char *)(p_value + j));
                __android_log_print(6, "AICEmu-pmmtool", "[1] returned Pmm: %s", pmm_str);
            }
        }

        if (result != 0)
            __system_property_set("tmp.AICEmu.pmmtool", "0");
        else
            __system_property_set("tmp.AICEmu.pmmtool", "1");
    }

    return result;
}

jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    __android_log_print(6, "AICEmu-pmmtool", "Inside JNI_OnLoad");
    __system_property_set("tmp.AICEmu.pmmtool", "0");

    JNIEnv *env = nullptr;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) == JNI_OK) {
        //void *func_addr = DobbySymbolResolver("libnfc-nci.so", "_Z23nfa_dm_check_set_confighPhb");
        void *func_addr = DobbySymbolResolver(NULL, "_Z23nfa_dm_check_set_confighPhb");
        __android_log_print(6, "AICEmu-pmmtool", "_Z23nfa_dm_check_set_confighPhb addr->%x", func_addr);
        DobbyHook(func_addr, (void *) new_func, (void **) &old_func);
        __android_log_print(6, "AICEmu-pmmtool", "Dobby hooked");
        return JNI_VERSION_1_6;
    }
    return 0;
}
