cmake_minimum_required(VERSION 3.4.1)

include_directories(src/main/cpp/ARToolkitHeaders/)

add_library( ARWrapper SHARED IMPORTED)

add_library( c++_shared SHARED IMPORTED)

add_library(sciteexar SHARED
	cpp/ARWrapperSciTeeXObjects.cpp)

add_library(opcuaclient SHARED
	cpp/opcuaclient.c
	cpp/Open62541/open62541.h
	cpp/Open62541/open62541.c)

set_target_properties( # Specifies the target library.
                    ARWrapper
                    PROPERTIES IMPORTED_LOCATION
                    libs/${ANDROID_ABI}/libARWrapper.so )

set_target_properties( # Specifies the target library.
                    c++_shared
                    PROPERTIES IMPORTED_LOCATION
                    libs/${ANDROID_ABI}/libc++_shared.so )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib
              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
              opcuaclient
              ARWrapper
              c++_shared
              sciteexar
              # Links the target library to the log library
              # included in the NDK.
              ${log-lib} )