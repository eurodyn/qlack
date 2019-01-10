package com.eurodyn.qlack.fuse.security.ws;

import com.eurodyn.qlack.common.exceptions.QDoesNotExistException;
import com.eurodyn.qlack.fuse.aaa.dto.UserDTO;
import com.eurodyn.qlack.fuse.aaa.dto.UserDetailsDTO;
import com.eurodyn.qlack.fuse.aaa.service.UserService;
import com.eurodyn.qlack.fuse.security.cache.AAAUserCaching;
import com.eurodyn.qlack.fuse.security.service.AuthenticationService;
import com.eurodyn.qlack.util.jwt.JWTUtil;
import com.eurodyn.qlack.util.jwt.dto.JWTClaimsRequestDTO;
import com.eurodyn.qlack.util.jwt.dto.JWTGenerateRequestDTO;
import io.swagger.annotations.Api;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Path("/auth")
@Api("Authentication API")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Component
public class AuthWS {

    @Context
    private HttpHeaders headers;

    @Value("${security.jwt.secret:aqlacksecret}")
    private String jwtSecret;

    /**
     * Default expiration set at 24 hours.
     */
    @Value("${security.jwt.expiration:#{24*60*60}}")
    private int jwtExpiration;

    private final AuthenticationService authenticationService;

    private final UserService userService;

    private final AAAUserCaching userCaching;

    @Autowired
    public AuthWS(AuthenticationService authenticationService, UserService userService, AAAUserCaching userCaching) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.userCaching = userCaching;
    }

    @POST
    @Path("/login")
    public Response login(UserDetailsDTO dto) throws ServiceException {
        UserDetailsDTO user = dto != null ? dto : new UserDetailsDTO();

        UserDetailsDTO authenticatedUser = (UserDetailsDTO) authenticationService
            .authenticate(user.getUsername(), user.getPassword())
            .getPrincipal();

        String userId = authenticatedUser.getId();
        String sessionId = userService.login(userId, user.getSessionId(), true).getSessionId();
        authenticatedUser.setSessionId(sessionId);

        String jwt = JWTUtil.generateToken(new JWTGenerateRequestDTO(jwtSecret, authenticatedUser.getUsername(), jwtExpiration));

        return Response.ok(authenticatedUser)
            .header(HttpHeaders.AUTHORIZATION, jwt)
            .build();
    }

    @POST
    @Path("/logout")
    public Response logout() throws ServiceException {
        String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        String username = String.valueOf(JWTUtil.getSubject(new JWTClaimsRequestDTO(token, jwtSecret, jwtExpiration)));

        UserDTO user = userService.getUserByName(username);

        if (user == null) {
            throw new QDoesNotExistException("User doesn't exist.");
        }

        // Remove user from cache.
        userCaching.getUserCache().removeUserFromCache(user.getUsername());

        // Logout from session.
        if (user.getSessionId() != null) {
            userService.logout(user.getId(), user.getSessionId());
        }

        return Response.ok().build();
    }

}
