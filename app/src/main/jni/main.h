#ifndef MAIN_H
#define MAIN_H

#include <jni.h>
#include <android/log.h>
#include <string.h>
#include <malloc.h>
#include <stdbool.h>
#include <stdlib.h>
#include <stdio.h>
#include <dlfcn.h>
#include <link.h>
#include <inttypes.h>
#include <unistd.h>

void *(*old_func)(u_int8_t, u_int8_t *, int) = nullptr;
#endif //MAIN_H

