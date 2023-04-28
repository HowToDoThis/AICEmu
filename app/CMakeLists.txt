cmake_minimum_required(VERSION 3.10.2)


include_directories(
        src/main/jni/dobby
)
enable_language(C ASM)


add_library(
        pmm
        SHARED
        src/main/jni/main.cpp
)

include_directories(dobby)
add_library(local_dobby STATIC IMPORTED)
set_target_properties(local_dobby PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/libs/${ANDROID_ABI}/libdobby.a)


find_library(
        log-lib
        log
)

find_library(android-lib android)
target_link_libraries(
        pmm
        local_dobby
        ${log-lib}
        ${android-lib}
)
