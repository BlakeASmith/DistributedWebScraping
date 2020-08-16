# Client Library 

Define new plugins & services for the Distributed Web Scraper!

# Installation

The library is written in **Kotlin**, but should be accessable from other JVM languages. However the API may be 
less friendly to use from java or other JVM languages. Particularly consuming and producing to topics relies 
heavily on Kotlin coroutines & Flows. see ![kotlinx-coroutines-rx2](https://github.com/Kotlin/kotlinx.coroutines/tree/master/reactive/kotlinx-coroutines-rx2) which
proides conversiosn between Kotlin coroutine constructs and equivilent RxJava constructs.

As of now the library needs to be included manually using the output jar files in ![clientlib/build/libs](/build/libs "libs") and ![clientlib/definitions/build/libs](/definitions/build/libs).
In future we would like to have the library available via *Maven*. It is packaged into two separate artifacts. *clientlib.definitions* contains
the type definitions, as well as any code relating to Plugin development. The *clientlib* package contains any code which communicates 
with the Kafka cluster. 

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
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3"
    implementation 'org.apache.kafka:kafka-clients:2.0.0'
    implementation group: 'org.jsoup', name: 'jsoup', version: '1.11.3'
    implementation 'com.google.code.gson:gson:2.8.6'
    compile(name:"clientlib", ext:"jar")
    compile(name:"clientlib.definitions", ext:"jar")
    ...
}
```

# Plugin Development

To create a new plugin, simply create a class implementing the Plugin interface.

```kotlin
	import org.jsoup.nodes.Document

	interface Plugin {
		fun scrape(doc: Document): String
	}

	class MyPlugin: Plugin {
		fun scrape(doc: Document) = doc.location()
	}
```

The `Document` passed to the scrape function provides a Jquery-like interface to the *DOM*. Here we're just returning the URL.

Produce a Jar file containing your Plugin class and any required dependencies (Jsoup can be exluded as it is already present at the client).

# Defining Services

A **Service** is just a set of domains which you'd like to have scraped, along 
with a set of rules for how to scrape them. You create one via the data class Service.

```kotlin
data class Service(
    val name: String, // name of the service
    val rootDomains: List<String>, // domains to crawl, only pages with these domains will be crawled
    val filters: List<String>, // substrings used to filter out URLs. URLs will only be scraped if they do not
    			       // contain these
    val plugins: List<String> // the list of plugins which will be run on each page
    			      // the results of every plugin are pushed to the results channel, but the 
			      // name of the plugin used is indicated with each entry
)
```

and create one as follows:

```kotlin
import csc.distributed.webscraper.services.Service

val myService = Service(
	name = "example",
	rootDomains = listOf("https://www.scrapethissite.com", "http://www.important-cia-documents.com"),
	filters = listOf("#", "/dontlook", "/garbage path"),
	plugins = listOf("wordcount", "find-replace")
)
```

## Pushing a plugin & service to Kafka

Once you have defined your Plugin and Service, you can submit it and collect the
results as follows;

```kotlin
import csc.distributed.webscraper.kafka.Kafka
import csc.distributed.webscraper.plugins.webscraper
...
...
import csc.distributed.webscraper.Scraper
import ca.blakeasmith.kKafka.jvm.*

@Serializable
data class MyPluginResult(...)

fun main() {
    val myService = Service(
	    name = "example",
	    rootDomains = listOf("https://www.scrapethissite.com"),
	    filters = listOf("#", "/dontlook", "/garbage path"),
	    plugins = listOf("myPlugin")
    )
   
    val myPlugin = File("path/to/myPlugin.jar")

    val config = KafkaConfig(listof("<ip>:<addr>"))
    Scraper.Services(config)
             .write(myService.name, myService)
             .close()

    Scraper.Plugins(config)
             .write("myPlugin", myPlugin.readBytes())
             .close()

    Scraper.Output(myService.name, MyPluginResult.serializer().list)
           .read("my_Kafka_group_id") // returns a Flow
           .forEach { (url, pluginName, pluginResult)
               println(pluginResult)  
           }.collect()            
}

```












