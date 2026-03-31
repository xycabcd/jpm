package org.codejive.jpm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.codejive.jpm.config.AppInfo;
import org.codejive.jpm.util.*;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.DependencyResolutionException;

/** The class implementing all the jpm command actions. */
public class Jpm {
    private final Path directory;
    private final boolean noLinks;
    private final Path appFile;
    private final Path cacheDir;
    private final boolean verbose;

    private Jpm(Path directory, boolean noLinks, Path appFile, Path cacheDir, boolean verbose) {
        this.directory = directory;
        this.noLinks = noLinks;
        this.appFile = appFile;
        this.cacheDir = cacheDir;
        this.verbose = verbose;
    }

    /**
     * Create a new {@link Builder} instance for the {@link Jpm} class.
     *
     * @return A new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder class for the {@link Jpm} class. */
    public static class Builder {
        private Path directory;
        private boolean noLinks;
        private Path appFile;
        private Path cacheDir;
        private boolean verbose;

        private Builder() {}

        /**
         * Set the target directory to use for the jpm commands.
         *
         * @param directory The target directory.
         * @return The builder instance for chaining.
         */
        public Builder directory(Path directory) {
            this.directory = directory;
            return this;
        }

        /**
         * Set whether to create symbolic links or not.
         *
         * @param noLinks Whether to create symbolic links or not.
         * @return The builder instance for chaining.
         */
        public Builder noLinks(boolean noLinks) {
            this.noLinks = noLinks;
            return this;
        }

        /**
         * Set the app.yml file to use for the jpm commands.
         *
         * @param appFile The app.yml file.
         * @return The builder instance for chaining.
         */
        public Builder appFile(Path appFile) {
            this.appFile = appFile;
            return this;
        }

        /**
         * Set the cache directory to use for downloaded artifacts.
         *
         * @param cacheDir The cache directory.
         * @return The builder instance for chaining.
         */
        public Builder cacheDir(Path cacheDir) {
            this.cacheDir = cacheDir;
            return this;
        }

        /**
         * Set whether to enable verbose output or not.
         *
         * @param verbose Whether to enable verbose output or not.
         * @return The builder instance for chaining.
         */
        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        /**
         * Builds the {@link Jpm} instance.
         *
         * @return A {@link Jpm} instance.
         */
        public Jpm build() {
            return new Jpm(directory, noLinks, appFile, cacheDir, verbose);
        }
    }

    /**
     * Copies the given artifacts to the target directory.
     *
     * @param artifactNames The artifacts to copy.
     * @param sync Whether to sync the target directory or not.
     * @return An instance of {@link SyncResult} containing the statistics of the copy operation.
     * @throws IOException If an error occurred during the copy operation.
     * @throws DependencyResolutionException If an error occurred during the dependency resolution.
     */
    public SyncResult copy(String[] artifactNames, boolean sync)
            throws IOException, DependencyResolutionException {
        return copy(artifactNames, Collections.emptyMap(), sync);
    }

    /**
     * Copies the given artifacts to the target directory.
     *
     * @param artifactNames The artifacts to copy.
     * @param repos A map of additional repository names to URLs where artifacts can be found.
     * @param sync Whether to sync the target directory or not.
     * @return An instance of {@link SyncResult} containing the statistics of the copy operation.
     * @throws IOException If an error occurred during the copy operation.
     * @throws DependencyResolutionException If an error occurred during the dependency resolution.
     */
    public SyncResult copy(String[] artifactNames, Map<String, String> extraRepos, boolean sync)
            throws IOException, DependencyResolutionException 
    {
        AppInfo appInfo = readAppInfo();
        String[] deps = getArtifacts(artifactNames, appInfo);
        Map<String, String> repos = getRepositories(extraRepos, appInfo);
        List<Path> files = Resolver.create(deps, repos, cacheDir).resolvePaths();
        return FileUtils.syncArtifacts(files, directory, noLinks, !sync);
    }

    private static String artifactGav(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }

    /**
     * Returns the paths of the given artifacts. If no artifacts are given, the paths for all
     * dependencies in the app.yml file will be returned instead.
     *
     * @param artifactNames The artifacts to get the paths for.
     * @return A list of paths.
     * @throws DependencyResolutionException If an error occurred during the dependency resolution.
     * @throws IOException If an error occurred during the operation.
     */
    public List<Path> path(String[] artifactNames)
            throws DependencyResolutionException, IOException {
        return path(artifactNames, Collections.emptyMap());
    }

    /**
     * Returns the paths of the given artifacts. If no artifacts are given, the paths for all
     * dependencies in the app.yml file will be returned instead.
     *
     * @param artifactNames The artifacts to get the paths for.
     * @param extraRepos A map of additional repository names to URLs where artifacts can be found.
     * @return A list of paths.
     * @throws DependencyResolutionException If an error occurred during the dependency resolution.
     * @throws IOException If an error occurred during the operation.
     */
    public List<Path> path(String[] artifactNames, Map<String, String> extraRepos)
            throws DependencyResolutionException, IOException {
        AppInfo appInfo = readAppInfo();
        String[] deps = getArtifacts(artifactNames, appInfo);
        Map<String, String> repos = getRepositories(extraRepos, appInfo);
        if (deps.length > 0) {
            List<Path> files = Resolver.create(deps, repos, cacheDir).resolvePaths();
            return files;
        } else {
            return Collections.emptyList();
        }
    }

    private static String[] getArtifacts(String[] artifactNames, AppInfo appInfo) {
        String[] deps;
        if (artifactNames.length > 0) {
            deps = artifactNames;
        } else {
            deps = appInfo.getDependencyGAVs();
        }
        return deps;
    }

    private Map<String, String> getRepositories(Map<String, String> extraRepos, AppInfo appInfo) {
        Map<String, String> repos = new HashMap<>(appInfo.repositories());
        repos.putAll(extraRepos);
        return repos;
    }

    private AppInfo readAppInfo() throws IOException {
        return AppInfo.read(appFile);
    }
}
