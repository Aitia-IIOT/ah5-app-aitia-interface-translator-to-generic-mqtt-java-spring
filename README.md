# InterfaceTranslatorToGenericMQTT

This project provides an [interface translation provider](https://aitia-iiot.github.io/ah5-docs-java-spring/tools_assets/translation/translation_providers/#interface-translation-providers) for Eclipse Arrowhead 5th generation.

## Translation Capabilities

**From interfaces**

* generic_mqtt
* generic_mqtts
* generic_http
* generic_https

**to interfaces**

* generic_mqtt
* generic_mqtts

## How to Start the app

* Make sure you have the proper values in the configuration file before starting the application.

	> The configuration properties can be found in the application.properties file which is located next to the executable .jar file of the system. 

* Execute java -jar arrowhead-app-interface-translator-to-generic-mqtt-<version>.jar from the same folder.
* Note that the ServiceRegistry Core System has to be started first. The applications should be started only when ServiceRegistry is up and running.
