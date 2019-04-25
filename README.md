[![No Maintenance Intended](http://unmaintained.tech/badge.svg)](http://unmaintained.tech/)


# Headless Android Heap Analyzer

*“Ha Ha!”* - Nelson

## This repository is DEPRECATED

* This project was created to provide a heap dump parser for [LeakCanary](https://github.com/square/leakcanary) by repackaging heap dump parsers from other repositories.
* LeakCanary now has its own [heap dump parser](https://github.com/square/leakcanary/tree/master/leakcanary-haha).
* That parser is available on Maven Central as `com.squareup.leakcanary:leakcanary-haha:2.0-alpha-1`.

## Introduction

HAHA is a Java library to automate the analysis of Android heap dumps.

This project is essentially a repackaging of the work of others to make it available as a small footprint Maven dependency.

## Usage

To learn how to dump the heap, read the [Android documentation](https://developer.android.com/tools/debugging/debugging-memory.html#HeapDump). Here's an example:

``` java
File heapDumpFile = ...
Debug.dumpHprofData(heapDumpFile.getAbsolutePath());
```

After dumping the heap, use HAHA to parse and analyze it.

``` java
DataBuffer buffer = new MemoryMappedFileBuffer(heapDumpFile);
Snapshot snapshot = Snapshot.createSnapshot(buffer);

// The rest is up to you.
ClassObj someClass = snapshot.findClass("com.example.SomeClass");
```

## Gradle config

``` groovy
dependencies {
  compile 'com.squareup.haha:haha:2.1'
}
```

## Versions

### HAHA 2.1

* This release updates perflib to the `bc5c698efaf970f599e877a8b26de16736db4272` SHA of the android_platform_tool_base repo.
* It also overlays a patched version of `HprofParser.java` to fix https://issuetracker.google.com/issues/122713143.

### HAHA 2.0.4

This release separates out Trove4j from HAHA since Trove4j is available under LGPL 2.1 and HAHA is available under Apache 2.

* This library contains parts of [perflib](https://android.googlesource.com/platform/tools/base/+/studio-master-dev/perflib) and is available under the same license, Apache v2.
* It contains a repackaged version of Guava 
* It has an artifact dependency on a [fork of Trove4j](https://github.com/JetBrains/intellij-deps-trove4j) ("1.1 with patches from JetBrains") which is [available in jcenter](https://bintray.com/jetbrains/trove4j/trove4j) under the LGPL 2.1 license.
* The result is merged in an uber-jar (perflib + guava) proguarded to remove unused code and reduce the footprint.

### HAHA 2.0, 2.0.1, 2.0.2, 2.0.3

* This library contains parts of [perflib](https://android.googlesource.com/platform/tools/base/+/studio-master-dev/perflib) and is available under the same license, Apache v2.
* It contains a repackaged version of Guava and Trove4j "1.1 with patches from JetBrains"
* Trove4j is available under the LGPL 2.1 license.
* The result is merged in an uber-jar proguarded to remove unused code and reduce the footprint.

### HAHA 1.1, 1.2 and 1.3

* The first versions of HAHA were the result of a series of forks:
* [Eclipse Memory Analyzer](https://eclipse.org/mat) (MAT) is a Java heap analyzer. It's a standalone GUI built with Eclipse RCP that embeds an engine to parse and analyze heap dumps.
* [vshor/mat](https://bitbucket.org/vshor/mat) is a fork of *Eclipse Memory Analyzer*. It removed a lot of code and changed some of it to make a headless version (no GUI), as well as ignore weak references when finding paths to GC Roots.
* [AndroMAT](https://bitbucket.org/joebowbeer/andromat/overview) is a fork of *vshor/mat* and changed the heap dump parsing to support Android specific heap dump format.
* *HAHA* was originally a fork of *AndroMAT*. We recreated the lost Git history, kept the bare minimum needed code and packaged it to be releasable on Maven Central.
* MAT is available under the Eclipse Public License v1.0 so HAHA was initially released under that same license.

## Releasing

```
mvn release:prepare && mvn release:perform

```
