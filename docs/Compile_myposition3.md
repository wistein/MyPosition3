# Compile Guide for MyPosition3

## Environment for the Project
Windows 64-bit with adb driver
or
Linux 64-bit

Android Smartphone with high resolution screen (Android 7.1 or higher)

## Dependencies
- Android Studio (current version)

## Android Studio Components
Android SDK with
- Android Platforms: 9, 10, 11, 12, 13, 14, 15
- SDK Tools: Android Emulator, Android SDK Platform-Tools, Android SDK Tools, Android Support Library, Google USB Driver, Intel x86 Emulator Accelerator, Android Support Repository, Google Repository
- Plugins: .ignore, Android Support, CVS Integration, EditorConfig, Git Integration, GitHub, Gradle, Groovy, hg4idea, I18n for Java, IntelliLang, Java Bytecode Decompiler, JUnit, Properties Support, SDK Updater, Subversion Integration, Task Management, Terminal 

### Java SE 64-bit (current version)

### build.gradle (MyPosition3)
- buildscript:
  ext:
    kotlin_version = '2.0.20' (or higher)
  repositories:
    mavenCentral()
    google()
  dependencies:
    classpath 'com.android.tools.build:gradle:8.7.3' (or higher)
    classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"

### build.gradle (mypostion3)
- Compiled with SDK Ver. 35 for targetSdk 34 and minSdk 25.
- JavaVersion.VERSION_17 (or current version)
- External Libraries:
  implementation 'androidx.legacy:legacy-support-v4:1.0.0'
  implementation platform('org.jetbrains.kotlin:kotlin-bom:1.8.22')
  implementation 'androidx.work:work-runtime:2.10.0'
  implementation 'androidx.preference:preference-ktx:1.2.1'
  implementation 'androidx.core:core-ktx:1.15.0'

## Start the Project
Get the project source by downloading the master.zip.

Extract it to a directory "MyPosition3".

Load the directory as a project in Android Studio.

Set up your Android Studio environment regarding compiling key, apk directory and GitHub destination.
