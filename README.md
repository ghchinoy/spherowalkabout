spherowalkabout
===============

Sphero Walkabout
----------------

An app created at the [Boulder Sphero Hack Tour 2012](http://www.gosphero.com/dev)

See [app webpage](http://goo.gl/sFtVb)  for more info!

Use your Sphero to wander around the map!

* Use the Sphero as a controller
* Roll right to go east, left to go west, forward to go north, and backwards to go south
* Lift up to zoom out, drop down to zoom in

Using the source
----------------

To use this project please note that in order to show map tiles you'll need to configure the **Google Maps API**.

Please follow the instructions to [obtain a Google Maps API key](https://developers.google.com/maps/documentation/android/mapkey) that is specific to your machine's debug signing certificate.

When you've obtained the key, replace the `maps_debug` value in `res/strings.xml` 
There's also a `maps_release` value if you choose to create an apk with a production certificate (or export via Android Tools > Export Signed Application Package ... in Eclipse).