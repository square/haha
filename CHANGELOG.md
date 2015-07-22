# Change Log

## Version 2.0.2 *(2015-07-20)*

Switched hprof parsing from AndroMat to [perflib](https://android.googlesource.com/platform/tools/base/+/studio-master-dev/perflib)

Althought they are similar in spirit, all the APIs have changed.

## Version 1.3 *(2015-05-12)*

No caching of the result of parsing the heap dump. This could lead to stack overflow errors, and we don't need it anyway since our use case is to always open a heap dump, parse it, compute a result and throw away the index files.

## Version 1.2 *(2015-05-11)*

Replaces `messages.properties` resource with an enum, which should solve some Gradle configuration for projects that somehow can't see Java resource files.

## Version 1.1 *(2015-04-30)*

Initial release.
