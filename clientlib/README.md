# Client Library 

Define new plugins & services for the Distributed Web Scraper!

# Installation

As of now the library needs to be included manually using the output jar file in ![clientlib/build/libs](/build/libs "libs").
In future we would like to have the library available via *Maven*

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


# Plugin Development

Currently the plugin
