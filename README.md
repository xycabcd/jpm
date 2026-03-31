# xycabcd's JPM

Forked from codejive/java-jpm to make the tool as minimal and standalone as possible.

## Building

Use anything to get the dependencies specified in `jpm.yml`. 

Use any compiler to build the source code with Java 17 and the classpath of the dependencies.

## Using JPM

Note that JPM will read some settings in the MavenUserHome directory (default of `~/.m2`), and download artifacts in `[MavenUserHome]/repository`. If you want to change where that directory is located, use the `-H` option or set `JPM_HOME` environment variable. Other than that, JPM will (probably?) not touch anything else on your system.

The usage can be mostly seen in command line help.

### Dependency Definition File

A Dependency Definition File is a yaml file. It can define repositories and dependencies. If there are dependencies specified on command line, the ones in the file will be ignore. If there are repositories specified on command line, the ones in the fill will ALSO BE USED. For exact format, please refer to the documentation about app.yml on the README of codejive/java-jpm.

## Credits

This is a fork of codejive/java-jpm. Most of the works are done the the original author.
