# abcd's JPM

Forked from codejive/java-jpm to make the tool as minimal and standalone as possible.

## Building

Use anything to get the dependencies specified in `deps.yml`. This file itself is compatible with both this project's definition file and the upstream project's app.yml file.

Use any compiler to build the source code with Java 17 and the classpath of the dependencies.

## Using JPM

`org.codejive.jpm.Main` is the main class. The dependencies are needed to be included in classpath when running.

Usage can be mostly seen in command line help. Please note that the commands have very different usage than the original ones. So please read the help carefully.

The `--verbose, -v` flag is the only flag that can be passed globally and affect subcommands. The `--version, -V` only works without subcommands. The `--help, -h` print global help when passed globally. The rest only work with subcommands. 

JPM supports a `@filename` argument file option, similar to what `java` command provide. For it's usage, please see https://picocli.info/#AtFiles

JPM will read some settings in the MavenUserHome directory (default of `~/.m2`), and download artifacts to `[MavenUserHome]/repository`. If you want to change where that directory is located, use the `-H` option or set `JPM_HOME` environment variable. Other than that, JPM will (probably?) not touch anything else on your system.

It says that using `JPM_REPO_[...]` environment variables can set the user name and password for repositories, but I'm not sure if it works and I can't test it. If you tried it, please post the result in discussions.

### Dependency Definition File

A Dependency Definition File is a yaml file. It can define repositories and dependencies. If there are dependencies specified on command line, the ones in the file will be ignore. If there are repositories specified on command line, the ones in the fill will ALSO BE USED. For exact format, please refer to the documentation about app.yml on the README of codejive/java-jpm.

## Credits

This is a fork of codejive/java-jpm. Most of the works are done by the original author. License of original work is in the `upstream-legal.txt` file.
