cmake_minimum_required(VERSION 3.4.1)

message(${ANDROID_ABI})


 #Sets lib_src_DIR to the path of the target CMake project.
set( lib_src_DIR src/main/cpp/include/ )

 #Sets lib_build_DIR to the path of the desired output directory.
set( lib_build_DIR ../aRSciTeeXObjects/build/intermediates/ndkBuild/universal/debug/obj/local )


#add_library( arwrapper SHARED IMPORTED )
#set_target_properties( arwrapper PROPERTIES IMPORTED_LOCATION
#                       ${lib_build_DIR}/${ANDROID_ABI}/libARWrapper.so )

#add_library( c++_shared SHARED IMPORTED )
#set_target_properties(  c++_shared PROPERTIES IMPORTED_LOCATION
#                       ${lib_build_DIR}/${ANDROID_ABI}/libc++_shared.so )


#add_library( sciteexar SHARED IMPORTED )
#set_target_properties(  sciteexar PROPERTIES IMPORTED_LOCATION
#                       ${lib_build_DIR}/${ANDROID_ABI}/libARSciTeeXObjects.so )

#include_directories( ${lib_src_DIR} )


add_library(opcuaclient SHARED
	src/main/cpp/opcuaclient.c
	src/main/cpp/Open62541/open62541.h
	src/main/cpp/Open62541/open62541.c)


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
              #arwrapper
              #sciteexar
              opcuaclient
              # Links the target library to the log library
              # included in the NDK.
              ${log-lib})
