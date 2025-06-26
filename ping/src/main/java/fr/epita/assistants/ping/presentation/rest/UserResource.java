package fr.epita.assistants.ping.presentation.rest;

import fr.epita.assistants.ping.data.dto.*;
import fr.epita.assistants.ping.data.model.ProjectModel;
import fr.epita.assistants.ping.data.model.UserModel;
import fr.epita.assistants.ping.service.JwtService;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/user")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    JwtService jwtService;

    @ConfigProperty(name = "LOG_FILE")
    String logFile;

    @ConfigProperty(name = "ERROR_LOG_FILE")
    String errorLogFile;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        if (request.getLogin() == null || request.getPassword() == null) {
            logError("Login failed: null login or password");
            return Response.status(400).entity(new ErrorInfo("The login or the password is null")).build();
        }

        UserModel user = UserModel.find("login = ?1 and password = ?2",
                request.getLogin(), request.getPassword()).firstResult();

        if (user == null) {
            logError("Login failed: invalid credentials for " + request.getLogin());
            return Response.status(401).entity(new ErrorInfo("The login/password combination is invalid")).build();
        }

        String token = jwtService.generateToken(user);
        log("User logged in: " + user.getId());

        return Response.ok(new LoginResponse(token)).build();
    }

    @POST
    @RolesAllowed("admin")
    @Transactional
    public Response createUser(NewUserRequest request, @Context SecurityContext ctx) {
        if (request.getLogin() == null || request.getPassword() == null ||
                !isValidLogin(request.getLogin())) {
            logError("Create user failed: invalid login or password");
            return Response.status(400).entity(new ErrorInfo("The login or the password is invalid")).build();
        }

        if (UserModel.find("login", request.getLogin()).count() > 0) {
            logError("Create user failed: login already exists - " + request.getLogin());
            return Response.status(409).entity(new ErrorInfo("The login is already taken")).build();
        }

        UserModel user = new UserModel();
        user.setLogin(request.getLogin());
        user.setPassword(request.getPassword());
        user.setDisplayName(generateDisplayName(request.getLogin()));
        user.setAvatar("");
        user.setIsAdmin(request.getIsAdmin() != null ? request.getIsAdmin() : false);

        user.persist();
        log("User created by admin " + ctx.getUserPrincipal().getName() + ": " + user.getId());

        return Response.ok(toUserResponse(user)).build();
    }

    @GET
    @Path("/all")
    @RolesAllowed("admin")
    public Response getAllUsers(@Context SecurityContext ctx) {
        List<UserModel> users = UserModel.listAll();
        log("Admin " + ctx.getUserPrincipal().getName() + " listed all users");

        return Response.ok(users.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList())).build();
    }

    @GET
    @Path("/refresh")
    @Authenticated
    public Response refresh(@Context SecurityContext ctx) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel user = UserModel.findById(userId);

        if (user == null) {
            logError("Refresh token failed: user not found - " + userId);
            return Response.status(404).entity(new ErrorInfo("The user could not be found")).build();
        }

        String token = jwtService.generateToken(user);
        log("Token refreshed for user: " + userId);

        return Response.ok(new LoginResponse(token)).build();
    }

    @GET
    @Path("/{id}")
    @Authenticated
    public Response getUser(@PathParam("id") UUID id, @Context SecurityContext ctx) {
        UUID requesterId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel requester = UserModel.findById(requesterId);

        if (!requesterId.equals(id) && !requester.getIsAdmin()) {
            logError("Get user failed: unauthorized access by " + requesterId + " to user " + id);
            return Response.status(403).entity(new ErrorInfo("The user is not allowed to access this user")).build();
        }

        UserModel user = UserModel.findById(id);
        if (user == null) {
            logError("Get user failed: user not found - " + id);
            return Response.status(404).entity(new ErrorInfo("User not found")).build();
        }

        log("User " + requesterId + " accessed user data: " + id);
        return Response.ok(toUserResponse(user)).build();
    }

    @PUT
    @Path("/{id}")
    @Authenticated
    @Transactional
    public Response updateUser(@PathParam("id") UUID id, UpdateUserRequest request, @Context SecurityContext ctx) {
        UUID requesterId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserModel requester = UserModel.findById(requesterId);

        if (!requesterId.equals(id) && !requester.getIsAdmin()) {
            logError("Update user failed: unauthorized access by " + requesterId + " to user " + id);
            return Response.status(403).entity(new ErrorInfo("The user is not allowed")).build();
        }

        UserModel user = UserModel.findById(id);
        if (user == null) {
            logError("Update user failed: user not found - " + id);
            return Response.status(404).entity(new ErrorInfo("The user could not be found")).build();
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(request.getPassword());
        }
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        user.persist();
        log("User " + requesterId + " updated user: " + id);

        return Response.ok(toUserResponse(user)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    @Transactional
    public Response deleteUser(@PathParam("id") UUID id, @Context SecurityContext ctx) {
        UserModel user = UserModel.findById(id);
        if (user == null) {
            logError("Delete user failed: user not found - " + id);
            return Response.status(404).entity(new ErrorInfo("The user could not be found")).build();
        }

        if (ProjectModel.find("owner", user).count() > 0) {
            logError("Delete user failed: user owns projects - " + id);
            return Response.status(403).entity(new ErrorInfo("The user owns projects")).build();
        }

        user.delete();
        log("Admin " + ctx.getUserPrincipal().getName() + " deleted user: " + id);

        return Response.noContent().build();
    }

    private boolean isValidLogin(String login) {
        if (login == null || login.isBlank()) return false;

        long separatorCount = login.chars().filter(ch -> ch == '.' || ch == '_').count();
        return separatorCount == 1;
    }

    private String generateDisplayName(String login) {
        String[] parts = login.split("[._]");
        return Arrays.stream(parts)
                .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1))
                .collect(Collectors.joining(" "));
    }

    private UserResponse toUserResponse(UserModel user) {
        return new UserResponse(
                user.getId(),
                user.getLogin(),
                user.getDisplayName(),
                user.getIsAdmin(),
                user.getAvatar()
        );
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