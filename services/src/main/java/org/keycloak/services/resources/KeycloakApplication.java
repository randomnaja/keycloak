package org.keycloak.services.resources;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.SkeletonKeyContextResolver;
import org.keycloak.audit.AuditListener;
import org.keycloak.audit.AuditListenerFactory;
import org.keycloak.audit.AuditProvider;
import org.keycloak.audit.AuditProviderFactory;
import org.keycloak.models.Config;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelProvider;
import org.keycloak.provider.ProviderFactoryLoader;
import org.keycloak.services.DefaultProviderSessionFactory;
import org.keycloak.services.ProviderSessionFactory;
import org.keycloak.util.KeycloakRegistry;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.SocialRequestManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.resources.admin.AdminService;
import org.keycloak.models.utils.ModelProviderUtils;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakApplication extends Application {

    private static final Logger log = Logger.getLogger(KeycloakApplication.class);

    protected Set<Object> singletons = new HashSet<Object>();
    protected Set<Class<?>> classes = new HashSet<Class<?>>();

    protected KeycloakSessionFactory factory;
    protected String contextPath;

    public KeycloakApplication(@Context ServletContext context) {
        this.factory = createSessionFactory();
        this.contextPath = context.getContextPath();
        KeycloakRegistry registry = new KeycloakRegistry();
        registry.putService(KeycloakSessionFactory.class, factory);
        context.setAttribute(KeycloakRegistry.class.getName(), registry);
        //classes.add(KeycloakSessionCleanupFilter.class);

        context.setAttribute(ProviderSessionFactory.class.getName(), createProviderSessionFactory());

        TokenManager tokenManager = new TokenManager();
        SocialRequestManager socialRequestManager = new SocialRequestManager();

        singletons.add(new RealmsResource(tokenManager, socialRequestManager));
        singletons.add(new AdminService(tokenManager));
        singletons.add(new SocialResource(tokenManager, socialRequestManager));
        classes.add(SkeletonKeyContextResolver.class);
        classes.add(QRCodeResource.class);
        classes.add(ThemeResource.class);

        setupDefaultRealm();
    }

    public String getContextPath() {
        return contextPath;
    }

    /**
     * Get base URI of WAR distribution, not JAX-RS
     *
     * @param uriInfo
     * @return
     */
    public URI getBaseUri(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().replacePath(getContextPath()).build();
    }

    protected void setupDefaultRealm() {
        new ApplianceBootstrap().bootstrap(factory);
    }


    public static KeycloakSessionFactory createSessionFactory() {
        ModelProvider provider = ModelProviderUtils.getConfiguredModelProvider();

        if (provider != null) {
            log.debug("Model provider: " + provider.getId());
            return provider.createFactory();
        }

        throw new RuntimeException("Model provider not found");
    }

    public static DefaultProviderSessionFactory createProviderSessionFactory() {
        DefaultProviderSessionFactory factory = new DefaultProviderSessionFactory();

        factory.registerLoader(AuditProvider.class, ProviderFactoryLoader.create(AuditProviderFactory.class), Config.getAuditProvider());
        factory.registerLoader(AuditListener.class, ProviderFactoryLoader.create(AuditListenerFactory.class));

        return factory;
    }

    public KeycloakSessionFactory getFactory() {
        return factory;
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

}
