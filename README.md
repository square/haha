# Headless Android Heap Analyzer

*“Ha Ha!”* - Nelson


## Introduction

HAHA is a Java library to automate the analysis of Android heap dumps.

This project is the result of a series of forks, and we have done very little work apart from making it available as a Maven dependency. Here's the fork history:

* [Eclipse Memory Analyzer](https://eclipse.org/mat) (MAT) is a Java heap analyzer. It's a standalone GUI built with Eclipse RCP that embeds an engine to parse and analyze heap dumps.
* [vshor/mat](https://bitbucket.org/vshor/mat) is a fork of *Eclipse Memory Analyzer*. It removed a lot of code and changed some of it to make a headless version (no GUI), as well as ignore weak references when finding paths to GC Roots.
* [AndroMAT](https://bitbucket.org/joebowbeer/andromat/overview) is a fork of *vshor/mat* and changed the heap dump parsing to support Android specific heap dump format.
* *HAHA* is a fork of *AndroMAT*. We recreated the lost Git history, kept the bare minimum needed code and packaged it to be releasable on Maven Central.

## Usage

To learn how to dump the heap, read the [Android documentation](https://developer.android.com/tools/debugging/debugging-memory.html#HeapDump). Here's an example:

```
File heapDumpFile = ...
Debug.dumpHprofData(heapDumpFile.getAbsolutePath());
```

After dumping the heap, use HAHA to parse and analyze it. The closest thing to documentation would probably be the [Eclipse Memory Analyzer API](http://help.eclipse.org/juno/index.jsp?topic=%2Forg.eclipse.mat.ui.help%2Fdoc%2Findex.html&overview-summary.html), or simply reading the sources.

```
SnapshotFactory factory = new SnapshotFactory();
Map<String, String> args = Collections.emptyMap();
VoidProgressListener listener = new VoidProgressListener();
ISnapshot snapshot = factory.openSnapshot(heapDumpFile, args, listener);

// The rest is up to you.
Collection<IClass> refClasses = snapshot.getClassesByName("com.example.SomeClass", false);
```

## Gradle config

```
dependencies {
  compile 'com.squareup.haha:haha:1.2'
}
```

## Contributing

We aren't accepting external contributions at this time.