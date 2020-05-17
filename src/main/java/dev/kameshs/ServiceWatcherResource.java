package dev.kameshs;

import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import dev.kameshs.watcher.ServiceWatcher;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.vertx.mutiny.core.eventbus.EventBus;


@ApplicationScoped
@Path("/service")
public class ServiceWatcherResource {

    private static final Logger LOGGER =
            Logger.getLogger(ServiceWatcherResource.class.getName());

    @Inject
    KubernetesClient client;

    @ConfigProperty(name = "dev.kameshs.watch-namespaces")
    List<String> watchNamespaces;

    @Inject
    EventBus bus;

    @Inject
    ServiceWatcher serviceWatcher;

    @PostConstruct
    void init() {
        if (watchNamespaces != null && !watchNamespaces.isEmpty()) {
            // for (String ns : watchNamespaces) {
            // client.services().inNamespace(ns).watch(serviceWatcher);
            // }
        }
    }

    @GET
    @Path("/expose/{namespace}")
    public Response expose(@PathParam("namespace") String namespace)
            throws Exception {
        LOGGER.info("Expose NS " + namespace);
        bus.sendAndForget("start-watching-namespace", namespace);
        return Response.noContent().build();
    }

}
