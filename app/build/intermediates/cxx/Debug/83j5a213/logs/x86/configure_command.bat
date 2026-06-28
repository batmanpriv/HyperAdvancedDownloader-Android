@echo off
"C:\\Program Files\\CMake\\bin\\cmake.exe" ^
  "-HC:\\Users\\007\\Desktop\\HADAndroid\\app\\src\\main\\cpp" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=26" ^
  "-DANDROID_PLATFORM=android-26" ^
  "-DANDROID_ABI=x86" ^
  "-DCMAKE_ANDROID_ARCH_ABI=x86" ^
  "-DANDROID_NDK=C:\\AndroidSDK\\ndk\\27.3.13750724" ^
  "-DCMAKE_ANDROID_NDK=C:\\AndroidSDK\\ndk\\27.3.13750724" ^
  "-DCMAKE_TOOLCHAIN_FILE=C:\\AndroidSDK\\ndk\\27.3.13750724\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=" ^
  "-DCMAKE_CXX_FLAGS=-std=c++17" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=C:\\Users\\007\\Desktop\\HADAndroid\\app\\build\\intermediates\\cxx\\Debug\\83j5a213\\obj\\x86" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=C:\\Users\\007\\Desktop\\HADAndroid\\app\\build\\intermediates\\cxx\\Debug\\83j5a213\\obj\\x86" ^
  "-DCMAKE_BUILD_TYPE=Debug" ^
  "-BC:\\Users\\007\\Desktop\\HADAndroid\\app\\.cxx\\Debug\\83j5a213\\x86" ^
  -GNinja
