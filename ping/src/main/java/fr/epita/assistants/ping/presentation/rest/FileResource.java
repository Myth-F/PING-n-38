package fr.epita.assistants.ping.presentation.rest;

import fr.epita.assistants.ping.data.dto.MoveRequest;
import fr.epita.assistants.ping.data.dto.PathRequest;
import fr.epita.assistants.ping.data.model.ProjectModel;
import fr.epita.assistants.ping.data.model.UserModel;
import fr.epita.assistants.ping.utils.ErrorInfo;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;              // JDK Path (for file-system use)
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;


@jakarta.ws.rs.Path("/api/projects/{projectId}/files")
@ApplicationScoped
@Authenticated
public class FileResource {

    @ConfigProperty(name = "LOG_FILE")
    String logFile;

    @ConfigProperty(name = "ERROR_LOG_FILE")
    String errorLogFile;

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getFile(@PathParam("projectId") UUID projectId,
                            @QueryParam("path") @DefaultValue("") String relativePath,
                            @Context SecurityContext ctx) {

        if (relativePath == null) {
            logError("Get file failed: null path");
            return Response.status(400).entity("The relative path is invalid").build();
        }

        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel user   = UserModel.findById(userId);
        ProjectModel project = ProjectModel.findById(projectId);

        if (project == null) {
            logError("Get file failed: project not found - " + projectId);
            return Response.status(404).entity("The project could not be found").build();
        }
        if (!hasProjectAccess(project, user)) {
            logError("Get file failed: unauthorized access by " + userId + " to project " + projectId);
            return Response.status(403).entity("The user is not allowed to access the project").build();
        }

        try {
            Path projectPath = Paths.get(project.getPath());
            Path filePath    = projectPath.resolve(relativePath).normalize();

            if (!filePath.startsWith(projectPath)) {
                logError("Get file failed: path traversal attempt - " + relativePath);
                return Response.status(403).entity("Path traversal detected").build();
            }
            if (!Files.exists(filePath)) {
                logError("Get file failed: file not found - " + relativePath);
                return Response.status(404).entity("File not found").build();
            }

            byte[] content = Files.readAllBytes(filePath);
            log("User " + userId + " accessed file " + relativePath + " in project " + projectId);
            return Response.ok(content).build();

        } catch (IOException e) {
            logError("Get file failed: " + e.getMessage());
            return Response.status(500).entity("Failed to read file").build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response createFile(@PathParam("projectId") UUID projectId,
                               PathRequest request,
                               @Context SecurityContext ctx) {

        if (request.getRelativePath() == null || request.getRelativePath().isBlank()) {
            logError("Create file failed: invalid path");
            return Response.status(400).entity(new ErrorInfo("The relative path is invalid")).build();
        }

        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel user   = UserModel.findById(userId);
        ProjectModel project = ProjectModel.findById(projectId);

        if (project == null) {
            logError("Create file failed: project not found - " + projectId);
            return Response.status(404).entity(new ErrorInfo("The project could not be found")).build();
        }
        if (!hasProjectAccess(project, user)) {
            logError("Create file failed: unauthorized access by " + userId + " to project " + projectId);
            return Response.status(403).entity(new ErrorInfo("The user is not allowed to access the project")).build();
        }

        try {
            Path projectPath = Paths.get(project.getPath());
            Path filePath    = projectPath.resolve(request.getRelativePath()).normalize();

            if (!filePath.startsWith(projectPath)) {
                logError("Create file failed: path traversal attempt - " + request.getRelativePath());
                return Response.status(403).entity(new ErrorInfo("Path traversal detected")).build();
            }
            if (Files.exists(filePath)) {
                logError("Create file failed: file already exists - " + request.getRelativePath());
                return Response.status(409).entity(new ErrorInfo("The file already exists")).build();
            }

            Files.createDirectories(filePath.getParent());
            Files.createFile(filePath);

            log("User " + userId + " created file " + request.getRelativePath() + " in project " + projectId);
            return Response.status(201).build();

        } catch (IOException e) {
            logError("Create file failed: " + e.getMessage());
            return Response.status(500).entity(new ErrorInfo("Failed to create file")).build();
        }
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response deleteFile(@PathParam("projectId") UUID projectId,
                               PathRequest request,
                               @Context SecurityContext ctx) {

        if (request.getRelativePath() == null || request.getRelativePath().isBlank()) {
            logError("Delete file failed: invalid path");
            return Response.status(400).entity(new ErrorInfo("The relative path is invalid")).build();
        }

        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel user   = UserModel.findById(userId);
        ProjectModel project = ProjectModel.findById(projectId);

        if (project == null) {
            logError("Delete file failed: project not found - " + projectId);
            return Response.status(404).entity(new ErrorInfo("The project could not be found")).build();
        }
        if (!hasProjectAccess(project, user)) {
            logError("Delete file failed: unauthorized access by " + userId + " to project " + projectId);
            return Response.status(403).entity(new ErrorInfo("The user is not allowed to access the project")).build();
        }

        try {
            Path projectPath = Paths.get(project.getPath());
            Path filePath    = projectPath.resolve(request.getRelativePath()).normalize();

            if (!filePath.startsWith(projectPath)) {
                logError("Delete file failed: path traversal attempt - " + request.getRelativePath());
                return Response.status(403).entity(new ErrorInfo("Path traversal detected")).build();
            }
            if (!Files.exists(filePath)) {
                logError("Delete file failed: file not found - " + request.getRelativePath());
                return Response.status(404).entity(new ErrorInfo("The file could not be found")).build();
            }

            if (filePath.equals(projectPath)) {
                Files.list(filePath).forEach(p -> {
                    try { deleteRecursively(p); } catch (IOException e) { throw new RuntimeException(e); }
                });
            } else {
                deleteRecursively(filePath);
            }

            log("User " + userId + " deleted file " + request.getRelativePath() + " in project " + projectId);
            return Response.noContent().build();

        } catch (IOException e) {
            logError("Delete file failed: " + e.getMessage());
            return Response.status(500).entity(new ErrorInfo("Failed to delete file")).build();
        }
    }


    @POST
    @jakarta.ws.rs.Path("/upload")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response uploadFile(@PathParam("projectId") UUID projectId,
                               @QueryParam("path") String relativePath,
                               InputStream content,
                               @Context SecurityContext ctx) {

        if (relativePath == null || relativePath.isBlank()) {
            logError("Upload file failed: invalid path");
            return Response.status(400).entity(new ErrorInfo("The relative path is invalid")).build();
        }

        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel user   = UserModel.findById(userId);
        ProjectModel project = ProjectModel.findById(projectId);

        if (project == null) {
            logError("Upload file failed: project not found - " + projectId);
            return Response.status(404).entity(new ErrorInfo("The project could not be found")).build();
        }
        if (!hasProjectAccess(project, user)) {
            logError("Upload file failed: unauthorized access by " + userId + " to project " + projectId);
            return Response.status(403).entity(new ErrorInfo("The user is not allowed to access the project")).build();
        }

        try {
            Path projectPath = Paths.get(project.getPath());
            Path filePath    = projectPath.resolve(relativePath).normalize();

            if (!filePath.startsWith(projectPath)) {
                logError("Upload file failed: path traversal attempt - " + relativePath);
                return Response.status(403).entity(new ErrorInfo("Path traversal detected")).build();
            }

            Files.createDirectories(filePath.getParent());
            Files.copy(content, filePath, StandardCopyOption.REPLACE_EXISTING);

            log("User " + userId + " uploaded file " + relativePath + " in project " + projectId);
            return Response.status(201).build();

        } catch (IOException e) {
            logError("Upload file failed: " + e.getMessage());
            return Response.status(500).entity(new ErrorInfo("Failed to upload file")).build();
        }
    }

    @PUT
    @jakarta.ws.rs.Path("/move")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response moveFile(@PathParam("projectId") UUID projectId,
                             MoveRequest request,
                             @Context SecurityContext ctx) {

        if (request.getSrc() == null || request.getSrc().isBlank()
                || request.getDst() == null || request.getDst().isBlank()) {
            logError("Move file failed: invalid paths");
            return Response.status(400).entity(new ErrorInfo("The source or destination path is invalid")).build();
        }

        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel user   = UserModel.findById(userId);
        ProjectModel project = ProjectModel.findById(projectId);

        if (project == null) {
            logError("Move file failed: project not found - " + projectId);
            return Response.status(404).entity(new ErrorInfo("The project could not be found")).build();
        }
        if (!hasProjectAccess(project, user)) {
            logError("Move file failed: unauthorized access by " + userId + " to project " + projectId);
            return Response.status(403).entity(new ErrorInfo("The user is not allowed to access the project")).build();
        }

        try {
            Path projectPath = Paths.get(project.getPath());
            Path srcPath     = projectPath.resolve(request.getSrc()).normalize();
            Path dstPath     = projectPath.resolve(request.getDst()).normalize();

            if (!srcPath.startsWith(projectPath) || !dstPath.startsWith(projectPath)) {
                logError("Move file failed: path traversal attempt");
                return Response.status(403).entity(new ErrorInfo("Path traversal detected")).build();
            }
            if (!Files.exists(srcPath)) {
                logError("Move file failed: source not found - " + request.getSrc());
                return Response.status(404).entity(new ErrorInfo("Source file not found")).build();
            }
            if (Files.exists(dstPath)) {
                logError("Move file failed: destination already exists - " + request.getDst());
                return Response.status(409).entity(new ErrorInfo("The file already exists")).build();
            }

            Files.createDirectories(dstPath.getParent());
            Files.move(srcPath, dstPath);

            log("User " + userId + " moved file from " + request.getSrc() + " to " + request.getDst() + " in project " + projectId);
            return Response.noContent().build();

        } catch (IOException e) {
            logError("Move file failed: " + e.getMessage());
            return Response.status(500).entity(new ErrorInfo("Failed to move file")).build();
        }
    }


    private boolean hasProjectAccess(ProjectModel project, UserModel user) {
        return project.getOwner().getId().equals(user.getId())
                || project.getMembers().stream().anyMatch(m -> m.getId().equals(user.getId()))
                || user.getIsAdmin();
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.list(path).forEach(p -> {
                try { deleteRecursively(p); } catch (IOException e) { throw new RuntimeException(e); }
            });
        }
        Files.delete(path);
    }

    private void log(String message) {
        String timestamp = new SimpleDateFormat("dd/MM/yy - HH:mm:ss")
                .format(Calendar.getInstance().getTime());
        String logMessage = String.format("[%s] %s%n", timestamp, message);
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(logMessage);
        } catch (IOException e) { System.out.println(logMessage); }
    }

    private void logError(String message) {
        String timestamp = new SimpleDateFormat("dd/MM/yy - HH:mm:ss")
                .format(Calendar.getInstance().getTime());
        String logMessage = String.format("[%s] ERROR: %s%n", timestamp, message);
        try (FileWriter writer = new FileWriter(errorLogFile, true)) {
            writer.write(logMessage);
        } catch (IOException e) { System.err.println(logMessage); }
    }
}
