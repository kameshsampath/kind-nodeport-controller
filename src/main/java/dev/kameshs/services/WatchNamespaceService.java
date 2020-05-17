package dev.kameshs.services;

import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import dev.kameshs.watcher.ServiceWatcher;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.vertx.ConsumeEvent;

@ApplicationScoped
public class WatchNamespaceService {

	private static final Logger LOGGER =
			Logger.getLogger(WatchNamespaceService.class.getName());

	@Inject
	KubernetesClient client;

	@Inject
	ServiceWatcher serviceWatcher;

	@ConsumeEvent("start-watching-namespace")
	public void startWatch(String namespace) {
		LOGGER.info("Got to watch service in namespace " + namespace);
		client.services().inNamespace(namespace).watch(serviceWatcher);
	}
}
