package fr.epita.assistants.ping.service;

import fr.epita.assistants.ping.data.dto.ExecFeatureRequest;
import fr.epita.assistants.ping.domain.executor.FeatureExecutor;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@ApplicationScoped
public class GitExecutor implements FeatureExecutor {

    @Override
    public String name() {
        return "git";
    }

    @Override
    public void execute(File projectRoot, Object request) {
        ExecFeatureRequest execRequest = (ExecFeatureRequest) request;

        try {
            switch (execRequest.getCommand()) {
                case "init":
                    executeInit(projectRoot);
                    break;
                case "add":
                    executeAdd(projectRoot, execRequest.getParams());
                    break;
                case "commit":
                    executeCommit(projectRoot, execRequest.getParams());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown git command: " + execRequest.getCommand());
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void executeInit(File projectRoot) throws GitAPIException {
        Git.init().setDirectory(projectRoot).call();
    }

    private void executeAdd(File projectRoot, java.util.List<String> params) throws GitAPIException, IOException {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("No files specified for git add");
        }

        try (Git git = Git.open(projectRoot)) {
            for (String param : params) {
                Path basePath = projectRoot.toPath();
                Path filePath = basePath.resolve(param).normalize();

                if (!filePath.startsWith(basePath)) {
                    throw new IllegalArgumentException("Invalid file path: " + param);
                }

                if (param.contains("*") || param.contains("?")) {
                    // tf did i do here, handles wildcards
                    String pattern = param.replace("*", ".*").replace("?", ".");
                    try (Stream<Path> paths = Files.walk(basePath)) {
                        paths.filter(p -> !Files.isDirectory(p))
                                .filter(p -> basePath.relativize(p).toString().matches(pattern))
                                .forEach(p -> {
                                    try {
                                        git.add().addFilepattern(basePath.relativize(p).toString()).call();
                                    } catch (GitAPIException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                    }
                } else {
                    if (!Files.exists(filePath)) {
                        throw new IllegalArgumentException("File does not exist: " + param);
                    }
                    git.add().addFilepattern(param).call();
                }
            }
        } catch (org.eclipse.jgit.errors.RepositoryNotFoundException e) {
            throw new IllegalArgumentException("Not a git repository");
        }
    }

    private void executeCommit(File projectRoot, java.util.List<String> params) throws GitAPIException, IOException {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("No commit message provided");
        }

        try (Git git = Git.open(projectRoot)) {
            git.commit().setMessage(params.get(0)).call();
        } catch (org.eclipse.jgit.errors.RepositoryNotFoundException e) {
            throw new IllegalArgumentException("Not a git repository");
        }
    }
}