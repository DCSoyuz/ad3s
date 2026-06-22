# The following five lines of boilerplate have to be in your project's
# CMakeLists in this exact order for cmake to work correctly
cmake_minimum_required(VERSION 3.16)
set(ENV{IDF_COMPONENT_MANAGER} "1")
include($ENV{IDF_PATH}/tools/cmake/project.cmake)
set(EXTRA_COMPONENT_DIRS
  "$ENV{IDF_PATH}/components/"
)
set(
  COMPONENTS
  # NOTE: esp_vfs_console is added to see log output on the console (USB-CDC)
  "main esptool_py logger esp_vfs_console"
  CACHE STRING
  "List of components to include"
  )

idf_build_set_property(MINIMAL_BUILD ON)
project(ad3s_prog_esp32 CXX)  # C++ support