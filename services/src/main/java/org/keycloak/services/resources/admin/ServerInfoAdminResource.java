package org.keycloak.services.resources.admin;

import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.social.SocialProvider;
import org.keycloak.spi.authentication.AuthenticationProvider;
import org.keycloak.spi.authentication.AuthenticationProviderManager;
import org.keycloak.util.ProviderLoader;

import javax.ws.rs.GET;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ServerInfoAdminResource {

    @GET
    public ServerInfoRepresentation getInfo() {
        ServerInfoRepresentation info = new ServerInfoRepresentation();
        setSocialProviders(info);
        setThemes(info);
        setAuthProviders(info);
        return info;
    }

    private void setThemes(ServerInfoRepresentation info) {
        Iterable<ThemeProvider> providers = ProviderLoader.load(ThemeProvider.class);
        info.themes = new HashMap<String, List<String>>();
        for (Theme.Type type : Theme.Type.values()) {
            List<String> themes = new LinkedList<String>();
            for (ThemeProvider p : providers) {
                themes.addAll(p.nameSet(type));
            }
            Collections.sort(themes);
            info.themes.put(type.toString().toLowerCase(), themes);
        }
    }

    private void setSocialProviders(ServerInfoRepresentation info) {
        info.socialProviders = new LinkedList<String>();
        for (SocialProvider p : ProviderLoader.load(SocialProvider.class)) {
            info.socialProviders.add(p.getId());
        }
        Collections.sort(info.socialProviders);
    }

    private void setAuthProviders(ServerInfoRepresentation info) {
        info.authProviders = new HashMap<String, List<String>>();
        Iterable<AuthenticationProvider> authProviders = AuthenticationProviderManager.load();
        for (AuthenticationProvider authProvider : authProviders) {
            info.authProviders.put(authProvider.getName(), authProvider.getAvailableOptions());
        }
    }

    public static class ServerInfoRepresentation {

        private Map<String, List<String>> themes;

        private List<String> socialProviders;

        private Map<String, List<String>> authProviders;

        public ServerInfoRepresentation() {
        }

        public Map<String, List<String>> getThemes() {
            return themes;
        }

        public List<String> getSocialProviders() {
            return socialProviders;
        }

        public Map<String, List<String>> getAuthProviders() {
            return authProviders;
        }
    }

}
