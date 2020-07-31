# Client Library 

Define new plugins & services for the Distributed Web Scraper!

# Installation

The library is written in **Kotlin**, but should be accessable from other JVM languages. However the API may be 
less friendly to use from java or other JVM languages. Particularly consuming and producing to topics relies 
heavily on Kotlin coroutines & Flows. see ![kotlinx-coroutines-rx2](https://github.com/Kotlin/kotlinx.coroutines/tree/master/reactive/kotlinx-coroutines-rx2) which
proides conversiosn between Kotlin coroutine constructs and equivilent RxJava constructs.

As of now the library needs to be included manually using the output jar file in ![clientlib/build/libs](/build/libs "libs").
In future we would like to have the library available via *Maven*.

In intellij you can add it by selecting `File > Project Structure > Modules > Dependencies` and clicking the `+` icon.

Or add this to the build.gradle for your project

```gradle
repositories {
    ...
    flatDir {
        dirs "path/to/dir/containing/library"
    }
    ...
}

dependencies {
    ...
    compile(name:"clientlib", ext:"jar")
    ...
}
```

You will also need to add in the dependencies for the library, as noted you may require additional dependencies in non-Kotlin projects

```gradle
dependencies {
    ...
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3"
    implementation 'org.apache.kafka:kafka-clients:2.0.0'
    implementation group: 'org.jsoup', name: 'jsoup', version: '1.11.3'
    implementation 'com.google.code.gson:gson:2.8.6'
    compile(name:"clientlib", ext:"jar")
    ...
}
```

# Plugin Development

Currently the plugin
