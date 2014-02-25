package org.keycloak.jaxrs;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.KeycloakAuthenticatedSession;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.RSATokenVerifier;
import org.keycloak.adapters.ResourceMetadata;
import org.keycloak.VerificationException;
import org.keycloak.representations.AccessToken;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Priority(Priorities.AUTHENTICATION)
public class JaxrsBearerTokenFilter implements ContainerRequestFilter {
    protected ResourceMetadata resourceMetadata;
    private static Logger log = Logger.getLogger(JaxrsBearerTokenFilter.class);

    public JaxrsBearerTokenFilter(ResourceMetadata resourceMetadata) {
        this.resourceMetadata = resourceMetadata;
    }

    protected void challengeResponse(ContainerRequestContext request, String error, String description) {
        StringBuilder header = new StringBuilder("Bearer realm=\"");
        header.append(resourceMetadata.getRealm()).append("\"");
        if (error != null) {
            header.append(", error=\"").append(error).append("\"");
        }
        if (description != null) {
            header.append(", error_description=\"").append(description).append("\"");
        }
        request.abortWith(Response.status(Response.Status.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE, header.toString()).build());
        return;
    }

    @Context
    protected SecurityContext securityContext;

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        String authHeader = request.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null) {
            challengeResponse(request, null, null);
            return;
        }

        String[] split = authHeader.trim().split("\\s+");
        if (split == null || split.length != 2) challengeResponse(request, null, null);
        if (!split[0].equalsIgnoreCase("Bearer")) challengeResponse(request, null, null);


        String tokenString = split[1];


        try {
            AccessToken token = RSATokenVerifier.verifyToken(tokenString, resourceMetadata.getRealmKey(), resourceMetadata.getRealm());
            KeycloakAuthenticatedSession skSession = new KeycloakAuthenticatedSession(tokenString, token, resourceMetadata);
            ResteasyProviderFactory.pushContext(KeycloakAuthenticatedSession.class, skSession);
            String callerPrincipal = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : null;

            final KeycloakPrincipal principal = new KeycloakPrincipal(
                    token.getSubject(), callerPrincipal, token.getAttributes());
            final boolean isSecure = securityContext.isSecure();
            final AccessToken.Access access;
            if (resourceMetadata.getResourceName() != null) {
                access = token.getResourceAccess(resourceMetadata.getResourceName());
            } else {
                access = token.getRealmAccess();
            }
            SecurityContext ctx = new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return principal;
                }

                @Override
                public boolean isUserInRole(String role) {
                    if (access.getRoles() == null) return false;
                    return access.getRoles().contains(role);
                }

                @Override
                public boolean isSecure() {
                    return isSecure;
                }

                @Override
                public String getAuthenticationScheme() {
                    return "OAUTH_BEARER";
                }
            };
            request.setSecurityContext(ctx);
        } catch (VerificationException e) {
            log.error("Failed to verify token", e);
            challengeResponse(request, "invalid_token", e.getMessage());
        }
    }

}
