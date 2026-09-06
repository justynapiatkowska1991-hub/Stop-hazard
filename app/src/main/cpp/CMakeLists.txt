cmake_minimum_required(VERSION 3.22.1)
project("stophazard")

add_library(native-lib SHARED native-lib.cpp)

find_library(log-lib log)

target_include_directories(native-lib PRIVATE
    ${CMAKE_SOURCE_DIR}/prebuilt/include)

# tun2socks native library will be supplied per Android ABI by the build step.
# Expected layout:
# app/src/main/cpp/prebuilt/lib/<ABI>/libtun2socks.a

target_link_libraries(native-lib
    ${CMAKE_SOURCE_DIR}/prebuilt/lib/${ANDROID_ABI}/libtun2socks.a
    ${log-lib})
