@echo off
"C:\\Enviroment\\Android\\Sdk\\cmake\\3.18.1\\bin\\cmake.exe" ^
  "-HC:\\Users\\Michael Ho\\yukkei-workspace\\LooMotion\\LoomoVital\\openCVLib\\libcxx_helper" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=22" ^
  "-DANDROID_PLATFORM=android-22" ^
  "-DANDROID_ABI=armeabi-v7a" ^
  "-DCMAKE_ANDROID_ARCH_ABI=armeabi-v7a" ^
  "-DANDROID_NDK=C:\\Enviroment\\Android\\Sdk\\ndk\\23.1.7779620" ^
  "-DCMAKE_ANDROID_NDK=C:\\Enviroment\\Android\\Sdk\\ndk\\23.1.7779620" ^
  "-DCMAKE_TOOLCHAIN_FILE=C:\\Enviroment\\Android\\Sdk\\ndk\\23.1.7779620\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=C:\\Enviroment\\Android\\Sdk\\cmake\\3.18.1\\bin\\ninja.exe" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=C:\\Users\\Michael Ho\\yukkei-workspace\\LooMotion\\LoomoVital\\openCVLib\\build\\intermediates\\cxx\\Debug\\1k706ym1\\obj\\armeabi-v7a" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=C:\\Users\\Michael Ho\\yukkei-workspace\\LooMotion\\LoomoVital\\openCVLib\\build\\intermediates\\cxx\\Debug\\1k706ym1\\obj\\armeabi-v7a" ^
  "-DCMAKE_BUILD_TYPE=Debug" ^
  "-BC:\\Users\\Michael Ho\\yukkei-workspace\\LooMotion\\LoomoVital\\openCVLib\\.cxx\\Debug\\1k706ym1\\armeabi-v7a" ^
  -GNinja ^
  "-DANDROID_STL=c++_shared"
