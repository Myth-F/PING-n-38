package fr.epita.assistants.ping.presentation.rest;

import fr.epita.assistants.ping.data.dto.*;
import fr.epita.assistants.ping.data.model.ProjectModel;
import fr.epita.assistants.ping.data.model.UserModel;
import fr.epita.assistants.ping.domain.executor.FeatureExecutor;
import fr.epita.assistants.ping.service.GitExecutor;
import fr.epita.assistants.ping.utils.ErrorInfo;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Path("/api/projects")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProjectResource {

    @ConfigProperty(name = "PROJECT_DEFAULT_PATH")
    String projectDefaultPath;

    @ConfigProperty(name = "LOG_FILE")
    String logFile;

    @ConfigProperty(name = "ERROR_LOG_FILE")
    String errorLogFile;

    @Inject
    GitExecutor gitExecutor;

    @GET
    @Authenticated
    public Response listProjects(@QueryParam("onlyOwned") Boolean onlyOwned, @Context SecurityContext ctx) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel user = UserModel.findById(userId);

        List<ProjectModel> projects;
        if (Boolean.TRUE.equals(onlyOwned)) {
            projects = ProjectModel.find("owner", user).list();
        } else {
            projects = ProjectModel.find("owner = ?1 or ?2 member of members", user, user).list();
        }

        log("User " + userId + " listed projects (onlyOwned=" + onlyOwned + ")");

        return Response.ok(projects.stream()
                .map(this::toProjectResponse)
                .collect(Collectors.toList())).build();
    }

    @POST
    @Authenticated
    @Transactional
    public Response createProject(NewProjectRequest request,
                                  @Context SecurityContext ctx) {

        if (request.getName() == null || request.getName().isBlank()) {
            logError("Create project failed: invalid name");
            return Response.status(400)
                    .entity(new ErrorInfo("The project name is invalid"))
                    .build();
        }

        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel user = UserModel.findById(userId);

        ProjectModel project = new ProjectModel();
        project.setName(request.getName());
        project.setOwner(user);
        project.getMembers().clear();
        project.getMembers().add(user);

        UUID tempId = UUID.randomUUID();
        String projectPath = projectDefaultPath + tempId;
        project.setPath(projectPath);

        project.persist();
        project.flush();

        projectPath = projectDefaultPath + project.getId();
        project.setPath(projectPath);
        project.persist();

        try {
            Files.createDirectories(Paths.get(projectPath));
            log("User " + userId + " created project: " + project.getId());
        } catch (IOException e) {
            logError("Failed to create project directory: " + e.getMessage());
            project.delete();   
            return Response.status(500)
                    .entity(new ErrorInfo("Failed to create project directory"))
                    .build();
        }

        return Response.ok(toProjectResponse(project)).build();
    }

    @GET
    @Path("/all")
    @RolesAllowed("admin")
    public Response listAllProjects(@Context SecurityContext ctx) {
        List<ProjectModel> projects = ProjectModel.listAll();
        log("Admin " + ctx.getUserPrincipal().getName() + " listed all projects");

        return Response.ok(projects.stream()
                .map(this::toProjectResponse)
                .collect(Collectors.toList())).build();
    }

    @GET
    @Path("/{id}")
    @Authenticated
    public Response getProject(@PathParam("id") UUID id, @Context SecurityContext ctx) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel user = UserModel.findById(userId);
        ProjectModel project = ProjectModel.findById(id);

        if (project == null) {
            logError("Get project failed: project not found - " + id);
            return Response.status(404).entity(new ErrorInfo("Project not found")).build();
        }

        if (!hasProjectAccess(project, user)) {
            logError("Get project failed: unauthorized access by " + userId + " to project " + id);
            return Response.status(403).entity(new ErrorInfo("The user is not allowed to access the project")).build();
        }

        log("User " + userId + " accessed project: " + id);
        return Response.ok(toProjectResponse(project)).build();
    }

    @PUT
    @Path("/{id}")
    @Authenticated
    @Transactional
    public Response updateProject(@PathParam("id") UUID id, UpdateProjectRequest request, @Context SecurityContext ctx) {
        if (request.getName() == null && request.getNewOwnerId() == null) {
            logError("Update project failed: all fields null");
            return Response.status(400).entity(new ErrorInfo("Both the name and the new owner are null")).build();
        }

        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel user = UserModel.findById(userId);
        ProjectModel project = ProjectModel.findById(id);

        if (project == null) {
            logError("Update project failed: project not found - " + id);
            return Response.status(404).entity(new ErrorInfo("The project could not be found")).build();
        }

        if (!project.getOwner().getId().equals(userId) && !user.getIsAdmin()) {
            logError("Update project failed: unauthorized update by " + userId + " to project " + id);
            return Response.status(403).entity(new ErrorInfo("The user is not allowed to access the project")).build();
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            project.setName(request.getName());
        }

        if (request.getNewOwnerId() != null) {
            UserModel newOwner = UserModel.findById(request.getNewOwnerId());
            boolean isMember = newOwner != null &&
                    project.getMembers().stream().anyMatch(m -> m.getId().equals(newOwner.getId()));

            if (newOwner == null || !isMember) {
                logError("Update project failed: new owner not a member - " + request.getNewOwnerId());
                return Response.status(404).entity(new ErrorInfo("The new owner is not a member of the project")).build();
            }
            project.setOwner(newOwner);
        }

        log("User " + userId + " updated project: " + id);
        return Response.ok(toProjectResponse(project)).build();
    }

    @DELETE
    @Path("/{id}")
    @Authenticated
    @Transactional
    public Response deleteProject(@PathParam("id") UUID id, @Context SecurityContext ctx) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel user = UserModel.findById(userId);
        ProjectModel project = ProjectModel.findById(id);

        if (project == null) {
            logError("Delete project failed: project not found - " + id);
            return Response.status(404).entity(new ErrorInfo("The project could not be found")).build();
        }

        if (!project.getOwner().getId().equals(userId) && !user.getIsAdmin()) {
            logError("Delete project failed: unauthorized delete by " + userId + " to project " + id);
            return Response.status(403).entity(new ErrorInfo("The user is not allowed to access this project")).build();
        }

        // Delete project directory
        try {
            deleteDirectory(new File(project.getPath()));
        } catch (Exception e) {
            logError("Failed to delete project directory: " + e.getMessage());
        }

        project.delete();
        log("User " + userId + " deleted project: " + id);

        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/add-user")
    @Authenticated
    @Transactional
    public Response addMember(@PathParam("id") UUID id, UserProjectRequest request, @Context SecurityContext ctx) {
        if (request.getUserId() == null) {
            logError("Add member failed: invalid userId");
            return Response.status(400).entity(new ErrorInfo("The userId is invalid")).build();
        }

        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel user = UserModel.findById(userId);
        ProjectModel project = ProjectModel.findById(id);

        if (project == null) {
            logError("Add member failed: project not found - " + id);
            return Response.status(404).entity(new ErrorInfo("The project could not be found")).build();
        }

        if (!hasProjectAccess(project, user)) {
            logError("Add member failed: unauthorized access by " + userId + " to project " + id);
            return Response.status(403).entity(new ErrorInfo("The user is not allowed to access the project")).build();
        }

        UserModel newMember = UserModel.findById(request.getUserId());
        if (newMember == null) {
            logError("Add member failed: user not found - " + request.getUserId());
            return Response.status(404).entity(new ErrorInfo("The user could not be found")).build();
        }

        if (project.getMembers().stream().anyMatch(m -> m.getId().equals(newMember.getId()))) {
            logError("Add member failed: user already a member - " + request.getUserId());
            return Response.status(409).entity(new ErrorInfo("The user is already a member of the project")).build();
        }

        project.getMembers().add(newMember);
        log("User " + userId + " added member " + request.getUserId() + " to project " + id);

        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/remove-user")
    @Authenticated
    @Transactional
    public Response removeMember(@PathParam("id") UUID id, UserProjectRequest request, @Context SecurityContext ctx) {
        if (request.getUserId() == null) {
            logError("Remove member failed: invalid userId");
            return Response.status(400).entity(new ErrorInfo("The userId is invalid")).build();
        }

        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel user = UserModel.findById(userId);
        ProjectModel project = ProjectModel.findById(id);

        if (project == null) {
            logError("Remove member failed: project not found - " + id);
            return Response.status(404).entity(new ErrorInfo("The project could not be found")).build();
        }

        if (!project.getOwner().getId().equals(userId) && !user.getIsAdmin()) {
            logError("Remove member failed: unauthorized access by " + userId + " to project " + id);
            return Response.status(403).entity(new ErrorInfo("The user is not allowed to access the project")).build();
        }

        UserModel memberToRemove = UserModel.findById(request.getUserId());
        boolean isMember = memberToRemove != null &&
                project.getMembers().stream().anyMatch(m -> m.getId().equals(memberToRemove.getId()));

        if (memberToRemove == null || !isMember) {
            logError("Remove member failed: user not a member - " + request.getUserId());
            return Response.status(404).entity(new ErrorInfo("The user is not a member of the project")).build();
        }

        if (project.getOwner().getId().equals(request.getUserId())) {
            logError("Remove member failed: cannot remove owner - " + request.getUserId());
            return Response.status(403).entity(new ErrorInfo("Cannot remove the owner of the project")).build();
        }

        project.getMembers().removeIf(m -> m.getId().equals(memberToRemove.getId()));
        log("User " + userId + " removed member " + request.getUserId() + " from project " + id);

        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/exec")
    @Authenticated
    public Response executeFeature(@PathParam("id") UUID id, ExecFeatureRequest request, @Context SecurityContext ctx) {
        if (request.getFeature() == null || request.getCommand() == null) {
            logError("Execute feature failed: invalid parameters");
            return Response.status(400).entity(new ErrorInfo("Feature or command is null")).build();
        }

        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel user = UserModel.findById(userId);
        ProjectModel project = ProjectModel.findById(id);

        if (project == null) {
            logError("Execute feature failed: project not found - " + id);
            return Response.status(404).entity(new ErrorInfo("The project could not be found")).build();
        }

        if (!hasProjectAccess(project, user)) {
            logError("Execute feature failed: unauthorized access by " + userId + " to project " + id);
            return Response.status(403).entity(new ErrorInfo("The user is not allowed to access the project")).build();
        }

        if (!"git".equals(request.getFeature())) {
            logError("Execute feature failed: unknown feature - " + request.getFeature());
            return Response.status(400).entity(new ErrorInfo("Unknown feature")).build();
        }

        try {
            gitExecutor.execute(new File(project.getPath()), request);
            log("User " + userId + " executed " + request.getFeature() + " " + request.getCommand() + " on project " + id);
            return Response.noContent().build();
        } catch (Exception e) {
            logError("Execute feature failed: " + e.getMessage());
            return Response.status(e.getMessage().contains("not a git repository") ? 400 : 500)
                    .entity(new ErrorInfo(e.getMessage())).build();
        }
    }

    private boolean hasProjectAccess(ProjectModel project, UserModel user) {
        return project.getOwner().getId().equals(user.getId()) ||
                project.getMembers().stream().anyMatch(m -> m.getId().equals(user.getId())) ||
                user.getIsAdmin();
    }

    private ProjectResponse toProjectResponse(ProjectModel project) {
        List<UserSummaryResponse> members = project.getMembers().stream()
                .map(this::toUserSummary)
                .collect(Collectors.toList());

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                members,
                toUserSummary(project.getOwner())
        );
    }

    private UserSummaryResponse toUserSummary(UserModel user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getDisplayName(),
                user.getAvatar()
        );
    }

    private void deleteDirectory(File dir) throws IOException {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        Files.delete(dir.toPath());
    }

    private void log(String message) {
        String timestamp = new SimpleDateFormat("dd/MM/yy - HH:mm:ss")
                .format(Calendar.getInstance().getTime());
        String logMessage = String.format("[%s] %s\n", timestamp, message);

        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(logMessage);
        } catch (IOException e) {
            System.out.println(logMessage);
        }
    }

    private void logError(String message) {
        String timestamp = new SimpleDateFormat("dd/MM/yy - HH:mm:ss")
                .format(Calendar.getInstance().getTime());
        String logMessage = String.format("[%s] ERROR: %s\n", timestamp, message);

        try (FileWriter writer = new FileWriter(errorLogFile, true)) {
            writer.write(logMessage);
        } catch (IOException e) {
            System.err.println(logMessage);
        }
    }
}