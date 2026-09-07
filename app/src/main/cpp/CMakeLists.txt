cmake_minimum_required(VERSION 3.22.1)

project("myapplication")

# Include libADLMIDI
add_subdirectory(libADLMIDI)

add_library(
        myapplication
        SHARED
        native-lib.cpp)

target_link_libraries(
        myapplication
        ADLMIDI
        log)
