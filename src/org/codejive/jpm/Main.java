package org.codejive.jpm;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.codejive.jpm.util.SyncResult;
import org.codejive.jpm.util.Version;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Unmatched;

/** Main class for the jpm command line tool. */
@Command(
        name = "jpm",
        mixinStandardHelpOptions = true,
        versionProvider = Version.class,
        description = "Simple command line tool for managing Maven artifacts",
        subcommands = {
            Main.Copy.class,
            Main.PrintPath.class,
        })
public class Main {

    static boolean verbose = false;

    @Mixin VerboseMixin verboseMixin;

    @Command(
            name = "copy",
            aliases = {"c"},
            description =
                    "Resolves artifacts and copies them and all their dependencies to a target directory. "
                            + "If no artifacts are passed the dependencies defined in the definition file will be used.\n\n"
                            + "Example:\n  jpm copy org.apache.httpcomponents:httpclient:4.5.14\n")
    static class Copy implements Callable<Integer> {
        @Mixin VerboseMixin verboseMixin;
        @Mixin QuietMixin quietMixin;
        @Mixin AppInfoFileMixin appInfoFileMixin;
        @Mixin DepsMixin depsMixin;
        
        @Parameters(
                paramLabel = "<target>",
                description = "The directory to copy files to",
                index = "0"
        )
        private Path directory;
        
        @Parameters(
                paramLabel = "artifacts",
                description =
                        "One or more artifacts to resolve. Artifacts have the format <group>:<artifact>[:<extension>[:<classifier>]]:<version>",
                arity = "0..*",
                index="1..*"
        )
        private String[] artifactNames = {};

        @Option(
                names = {"-c", "--clear"},
                description =
                        "Makes sure the target directory will only contain the mentioned artifacts and their dependencies, possibly removing other files present in the directory",
                defaultValue = "false")
        private boolean sync;
        
                @Option(
                names = {"-s", "--symblink"},
                description = "Create symlinks for artifacts",
                defaultValue = "false")
        boolean symlink;

        @Override
        public Integer call() throws Exception {
            SyncResult stats =
                    Jpm.builder()
                            .directory(directory)
                            .noLinks(!symlink)
                            .cacheDir(depsMixin.getCacheDir())
                            .appFile(appInfoFileMixin.file)
                            .build()
                            .copy(
                                    artifactNames,
                                    depsMixin.getRepositoryMap(),
                                    sync);
            if (!quietMixin.quiet) {
                printStats(stats);
            }
            return (Integer) 0;
        }
    }
    

    @Command(
            name = "path",
            aliases = {"p"},
            description =
                    "Resolves artifacts and prints the full classpath to standard output. "
                            + "If no artifacts are passed the dependencies defined in the definition file will be used.\n\n"
                            + "Example:\n  jpm path org.apache.httpcomponents:httpclient:4.5.14\n")
    static class PrintPath implements Callable<Integer> {
        @Mixin VerboseMixin verboseMixin;
        @Mixin DepsMixin depsMixin;
        
        @Parameters(
                paramLabel = "artifacts",
                description =
                        "One or more artifacts to resolve. Artifacts have the format <group>:<artifact>[:<extension>[:<classifier>]]:<version>",
                arity = "0..*")
        private String[] artifactNames = {};
        
        @Mixin AppInfoFileMixin appInfoFileMixin;

        @Override
        public Integer call() throws Exception {
            List<Path> files =
                    Jpm.builder()
                            .cacheDir(depsMixin.getCacheDir())
                            .appFile(appInfoFileMixin.file)
                            .build()
                            .path(
                                    artifactNames,
                                    depsMixin.getRepositoryMap());
            if (!files.isEmpty()) {
                String classpath =
                        files.stream()
                                .map(Path::toString)
                                .collect(Collectors.joining(File.pathSeparator));
                System.out.print(classpath);
            }
            return (Integer) 0;
        }
    }

    

    

    static class DepsMixin {
        
        // TODO: 这环境变量根本就没读啊
        @Option(
                names = {"-r", "--repo"},
                description =
                        "URL to additional repository to use when resolving artifacts. Can be preceded by a name and an equals sign, e.g. -r myrepo=https://my.repo.com/maven2. When needing to pass user and password you can set JPM_REPO_<name>_USER and JPM_REPO_<name>_PASSWORD environment variables.")
        List<String> repositories = new ArrayList<>();

        @Option(
                names = {"-H", "--maven-home"},
                description =
                        "Directory that is the Maven User Home. Defaults to ~/.m2")
        Path homeDir;

        Path getCacheDir() {
            Path cacheDir = homeDir;
            
            if (cacheDir != null) {
                return cacheDir;
            }
            String envCache = System.getenv("JPM_HOME");
            if (envCache != null && !envCache.isEmpty()) {
                try {
                    return Path.of(envCache);
                } catch (InvalidPathException e) {
                    System.err.println(
                            "Warning: Invalid path in JPM_HOME environment variable, ignoring: "
                                    + envCache);
                }
            }
            return null;
        }

        Map<String, String> getRepositoryMap() {
            Map<String, String> repoMap = new HashMap<>();
            for (String repo : repositories) {
                String name;
                String url;
                int eq = repo.indexOf('=');
                if (eq >= 0) {
                    name = repo.substring(0, eq);
                    url = repo.substring(eq + 1);
                } else {
                    name = "";
                    url = repo;
                }
                if (name.isEmpty()) {
                    try {
                        URL x = new URL(repo);
                        name = x.getHost();
                    } catch (MalformedURLException e) {
                        name = "repo" + (repoMap.size() + 1);
                    }
                }
                if (!url.isEmpty()) {
                    repoMap.put(name, url);
                }
            }
            return repoMap;
        }
    }

    static class AppInfoFileMixin {
        @Option(
                names = {"-f", "--file"},
                description = "Dependency decleration file to use")
        Path file;
    }

    static class VerboseMixin {
        @Option(
                names = {"-v", "--verbose"},
                description = "Enable verbose output for debugging")
        public void setVerbose(boolean verbose) {
            Main.verbose = verbose;
        }
    }

    static class QuietMixin {
        @Option(
                names = {"-q", "--quiet"},
                description = "Don't output non-essential information",
                defaultValue = "false")
        private boolean quiet;
    }

    private static void printStats(SyncResult stats) {
        System.err.printf(
                "Artifacts new: %d, updated: %d, deleted: %d%n",
                (Integer) stats.copied, (Integer) stats.updated, (Integer) stats.deleted);
    }

    static CommandLine.IExecutionExceptionHandler errorHandler =
            (ex, commandLine, parseResult) -> {
                System.err.println("Error: " + ex.getMessage());
                if (verbose) {
                    ex.printStackTrace();
                } else {
                    System.err.println(
                            "Run with --verbose for stacktrace and more details.");
                }
                return commandLine.getCommandSpec().exitCodeOnExecutionException();
            };

    public static CommandLine getCommandLine() {
        return new CommandLine(new Main())
                .setStopAtPositional(true)
                .setAllowOptionsAsOptionParameters(true)
                .setAllowSubcommandsAsOptionParameters(true)
                .setExecutionExceptionHandler(errorHandler);
    }

    /**
     * Main entry point for the jpm command line tool.
     *
     * @param args The command line arguments.
     */
    public static void main(String... args) {
        getCommandLine().execute(args);
    }
}
