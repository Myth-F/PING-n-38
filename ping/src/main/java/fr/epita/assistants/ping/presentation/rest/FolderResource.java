package fr.epita.assistants.ping.presentation.rest;

import fr.epita.assistants.ping.data.dto.*;
import fr.epita.assistants.ping.data.model.ProjectModel;
import fr.epita.assistants.ping.data.model.UserModel;
import fr.epita.assistants.ping.utils.ErrorInfo;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;                       // JAX-RS annotations (except we’ll qualify @Path)
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;                   // filesystem Path
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

/*
 * NB: we fully-qualify the JAX-RS @Path annotation below to avoid the
 *     name clash with java.nio.file.Path imported above.
 */
@jakarta.ws.rs.Path("/api/projects/{projectId}/folders")
@ApplicationScoped
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FolderResource {

    @ConfigProperty(name = "LOG_FILE")
    String logFile;

    @ConfigProperty(name = "ERROR_LOG_FILE")
    String errorLogFile;

    /* ───────────────────────────── LIST ───────────────────────────── */

    @GET
    public Response listFolder(@PathParam("projectId") UUID projectId,
                               @QueryParam("path") @DefaultValue("") String relativePath,
                               @Context SecurityContext ctx) {

        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel user     = UserModel.findById(userId);
        ProjectModel project = ProjectModel.findById(projectId);

        if (project == null) {
            logError("List folder failed: project not found - " + projectId);
            return Response.status(404).entity(new ErrorInfo("The project could not be found")).build();
        }
        if (!hasProjectAccess(project, user)) {
            logError("List folder failed: unauthorized access by " + userId + " to project " + projectId);
            return Response.status(403).entity(new ErrorInfo("The user is not allowed to access the project")).build();
        }

        try {
            Path projectPath = Paths.get(project.getPath());
            Path folderPath  = projectPath.resolve(relativePath).normalize();

            if (!folderPath.startsWith(projectPath)) {
                logError("List folder failed: path traversal attempt - " + relativePath);
                return Response.status(403).entity(new ErrorInfo("Path traversal detected")).build();
            }
            if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
                logError("List folder failed: folder not found - " + relativePath);
                return Response.status(404).entity(new ErrorInfo("Folder not found")).build();
            }

            List<FSEntryResponse> entries = new ArrayList<>();
            Files.list(folderPath).forEach(path -> {
                String name       = path.getFileName().toString();
                String entryPath  = projectPath.relativize(path).toString();
                boolean directory = Files.isDirectory(path);
                entries.add(new FSEntryResponse(name, entryPath, directory));
            });

            log("User " + userId + " listed folder " + relativePath + " in project " + projectId);
            return Response.ok(entries).build();

        } catch (IOException e) {
            logError("List folder failed: " + e.getMessage());
            return Response.status(500).entity(new ErrorInfo("Failed to list folder")).build();
        }
    }

    /* ─────────────────────────── CREATE ─────────────────────────── */

    @POST
    @Transactional
    public Response createFolder(@PathParam("projectId") UUID projectId,
                                 PathRequest request,
                                 @Context SecurityContext ctx) {

        if (request.getRelativePath() == null || request.getRelativePath().isBlank()) {
            logError("Create folder failed: invalid path");
            return Response.status(400).entity(new ErrorInfo("The relative path is invalid")).build();
        }

        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel user     = UserModel.findById(userId);
        ProjectModel project = ProjectModel.findById(projectId);

        if (project == null) {
            logError("Create folder failed: project not found - " + projectId);
            return Response.status(404).entity(new ErrorInfo("The project could not be found")).build();
        }
        if (!hasProjectAccess(project, user)) {
            logError("Create folder failed: unauthorized access by " + userId + " to project " + projectId);
            return Response.status(403).entity(new ErrorInfo("The user is not allowed to access the project")).build();
        }

        try {
            Path projectPath = Paths.get(project.getPath());
            Path folderPath  = projectPath.resolve(request.getRelativePath()).normalize();

            if (!folderPath.startsWith(projectPath)) {
                logError("Create folder failed: path traversal attempt - " + request.getRelativePath());
                return Response.status(403).entity(new ErrorInfo("Path traversal detected")).build();
            }
            if (Files.exists(folderPath)) {
                logError("Create folder failed: folder already exists - " + request.getRelativePath());
                return Response.status(409).entity(new ErrorInfo("The folder already exists")).build();
            }

            Files.createDirectories(folderPath);

            log("User " + userId + " created folder " + request.getRelativePath() + " in project " + projectId);
            return Response.status(201).build();

        } catch (IOException e) {
            logError("Create folder failed: " + e.getMessage());
            return Response.status(500).entity(new ErrorInfo("Failed to create folder")).build();
        }
    }

    /* ─────────────────────────── DELETE ─────────────────────────── */

    @DELETE
    @Transactional
    public Response deleteFolder(@PathParam("projectId") UUID projectId,
                                 PathRequest request,
                                 @Context SecurityContext ctx) {

        if (request.getRelativePath() == null || request.getRelativePath().isBlank()) {
            logError("Delete folder failed: invalid path");
            return Response.status(400).entity(new ErrorInfo("The relative path is invalid")).build();
        }

        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel user     = UserModel.findById(userId);
        ProjectModel project = ProjectModel.findById(projectId);

        if (project == null) {
            logError("Delete folder failed: project not found - " + projectId);
            return Response.status(404).entity(new ErrorInfo("The project could not be found")).build();
        }
        if (!hasProjectAccess(project, user)) {
            logError("Delete folder failed: unauthorized access by " + userId + " to project " + projectId);
            return Response.status(403).entity(new ErrorInfo("The user is not allowed to access the project")).build();
        }

        try {
            Path projectPath = Paths.get(project.getPath());
            Path folderPath  = projectPath.resolve(request.getRelativePath()).normalize();

            if (!folderPath.startsWith(projectPath)) {
                logError("Delete folder failed: path traversal attempt - " + request.getRelativePath());
                return Response.status(403).entity(new ErrorInfo("Path traversal detected")).build();
            }
            if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
                logError("Delete folder failed: folder not found - " + request.getRelativePath());
                return Response.status(404).entity(new ErrorInfo("The folder could not be found")).build();
            }

            if (folderPath.equals(projectPath)) {
                Files.list(folderPath).forEach(p -> {
                    try { deleteRecursively(p); } catch (IOException e) { throw new RuntimeException(e); }
                });
            } else {
                deleteRecursively(folderPath);
            }

            log("User " + userId + " deleted folder " + request.getRelativePath() + " in project " + projectId);
            return Response.noContent().build();

        } catch (IOException e) {
            logError("Delete folder failed: " + e.getMessage());
            return Response.status(500).entity(new ErrorInfo("Failed to delete folder")).build();
        }
    }

    /* ─────────────────────────── MOVE ─────────────────────────── */

    @PUT
    @jakarta.ws.rs.Path("/move")
    @Transactional
    public Response moveFolder(@PathParam("projectId") UUID projectId,
                               MoveRequest request,
                               @Context SecurityContext ctx) {

        if (request.getSrc() == null || request.getSrc().isBlank()
                || request.getDst() == null || request.getDst().isBlank()) {
            logError("Move folder failed: invalid paths");
            return Response.status(400).entity(new ErrorInfo("The source or destination path is invalid")).build();
        }

        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel user     = UserModel.findById(userId);
        ProjectModel project = ProjectModel.findById(projectId);

        if (project == null) {
            logError("Move folder failed: project not found - " + projectId);
            return Response.status(404).entity(new ErrorInfo("The project could not be found")).build();
        }
        if (!hasProjectAccess(project, user)) {
            logError("Move folder failed: unauthorized access by " + userId + " to project " + projectId);
            return Response.status(403).entity(new ErrorInfo("The user is not allowed to access the project")).build();
        }

        try {
            Path projectPath = Paths.get(project.getPath());
            Path srcPath     = projectPath.resolve(request.getSrc()).normalize();
            Path dstPath     = projectPath.resolve(request.getDst()).normalize();

            if (!srcPath.startsWith(projectPath) || !dstPath.startsWith(projectPath)) {
                logError("Move folder failed: path traversal attempt");
                return Response.status(403).entity(new ErrorInfo("Path traversal detected")).build();
            }
            if (!Files.exists(srcPath) || !Files.isDirectory(srcPath)) {
                logError("Move folder failed: source not found - " + request.getSrc());
                return Response.status(404).entity(new ErrorInfo("Source folder not found")).build();
            }
            if (Files.exists(dstPath)) {
                logError("Move folder failed: destination already exists - " + request.getDst());
                return Response.status(409).entity(new ErrorInfo("The folder already exists")).build();
            }

            Files.createDirectories(dstPath.getParent());
            Files.move(srcPath, dstPath);

            log("User " + userId + " moved folder from " + request.getSrc() + " to " + request.getDst() + " in project " + projectId);
            return Response.noContent().build();

        } catch (IOException e) {
            logError("Move folder failed: " + e.getMessage());
            return Response.status(500).entity(new ErrorInfo("Failed to move folder")).build();
        }
    }

    /* ─────────────────────────── Helpers ─────────────────────────── */

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
