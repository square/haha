Trove: High Performance Collections for Java
http://trove.starlight-systems.com/

perflib and Android Studio use trove4j "1.1 (with patches from JetBrains)" which isn't available on Maven Central.

The `trove4j-1.1-sources.jar-unzipped_flattened` directory was created the following way:

* Check out the Android platform/prebuilts/tools repository at commit 3fde540f482afc68b847f8e85672acc145606ffe
* `common/m2/repository/net/sf/trove4j/trove4j/1.1` contains `trove4j-1.1-sources.jar`
* Unzip `trove4j-1.1-sources.jar`
* Merge `core/src/gnu/trove` and `generated/src/gnu/trove` in one source folder.