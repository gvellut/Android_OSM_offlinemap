Simple  Offline Maps with the MapsForge library

Map Data: OpenStreetMap (exported from http://download.bbbike.org/osm/bbbike/Tokyo/)

License: GPLv3

Although it was originally focused on Tokyo, this project now includes a way to generate an offline map application for basically any city (as long as an OSM export in PBF format is available, either created by yourself or accessible through an URL). It can be done by configuring a few values (see the configs folder at the root of the project).

Currently there are configurations for:
- Tokyo 
- Geneva
- Tokyo Ramen (pre-loaded data of ramen restaurants, provided by the Ramen DB)

Check out the apps on Google Play Store:
- Tokyo: https://play.google.com/store/apps/details?id=com.vellut.offlinemap.tokyo
- Geneva: https://play.google.com/store/apps/details?id=com.vellut.offlinemap.geneva
- Tokyo Ramen: https://play.google.com/store/apps/details?id=com.vellut.offlinemap.tokyoramen

## Configuration

- Download Osmosis and configure the mapsforge-writer plugin. Osmosis (http://wiki.openstreetmap.org/wiki/Osmosis) is a tool for processing OSM files. It is used to convert OSM PBF files to the MAP format used by Mapsforge, through the mapsforge-writer plugin. The plugin must be installed using the following instructions: https://code.google.com/p/mapsforge/wiki/GettingStartedMapWriter

- Clone the project or download a source zip.

- Create a local.properties file at the root of the project. In it, define properties sdk.dir and a osmosis.dir. For example:

```    
sdk.dir=/Applications/adt-bundle-mac-x86_64-20131030/sdk
osmosis.dir=/Users/guilhem/Documents/libraries/osmosis
```

- Create a sign.properties file at the root of the project. It is used to generate a release apk. It must look like this:

```
storeFile=../../android_keystore
storePassword=xxxxxxxx	
keyAlias=android_key
keyPassword=xxxxxxx
```

## Build

- To udpate the cartographic data for a configuration (downloaded from a preconfigured URL), open a terminal and got to the project directory. Do:

```
./gradew updateMapData -PmapConfiguration=<config>
```

(where config is one of the subfolders in the "configs" folder, e.g. "tokyo")

- To configure the source for one of the configs, do:

```
 ./gradlew configureAndroidProject -PmapConfiguration=<config>
```

- To build a release, do:

```
./gradlew aR
```

A release apk will be creating in the build/apk folder.

## Customize

Import the project into the Eclipse ADT using the "Android > Import existing code into workspace" tool.


