# clj-anti-z

A tiny cluster manager with async state saving and recover.

It's an anti zookeeper, just to synchronize migrate script in clojure across multiple nodes.

No ha, no quorum, just a basic state api.

## Installation

	git clone
	lein do clean, uberjar

## Usage

simply run :

    $ java -jar clj-anti-z-0.1.0-standalone.jar state-path port host


## License

Copyright Jean-Baptiste Besselat © 2016 Linkfluence SAS 
Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
