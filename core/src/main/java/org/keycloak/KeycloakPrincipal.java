package org.keycloak;

import java.security.Principal;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakPrincipal implements Principal {
    protected String name;
    protected String surrogate;
    protected Map<String, Object> attributes;

    public KeycloakPrincipal(String name, String surrogate, Map<String, Object> attributes) {
        this.name = name;
        this.surrogate = surrogate;
        this.attributes = attributes;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getSurrogate() {
        return surrogate;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeycloakPrincipal that = (KeycloakPrincipal) o;

        if (!name.equals(that.name)) return false;
        if (surrogate != null ? !surrogate.equals(that.surrogate) : that.surrogate != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (surrogate != null ? surrogate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return name;
    }
}
