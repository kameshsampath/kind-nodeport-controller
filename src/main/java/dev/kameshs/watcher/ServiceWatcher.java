package dev.kameshs.watcher;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import dev.kameshs.data.DockerContainer;
import dev.kameshs.util.ContainerUtility;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;

@ApplicationScoped
public class ServiceWatcher implements Watcher<Service> {

	private static final Logger LOGGER =
			Logger.getLogger(ServiceWatcher.class.getName());
	private static final String SERVICE_TYPE_NODE_PORT = "NodePort";
	private static final String DOCKER_CONTAINER_KEY_FORMAT =
			"%s-%s-%s-%s-%d-%d-proxy";

	@ConfigProperty(name = "dev.kameshs.watcher.kind-name",
			defaultValue = "kind")
	String kindName;

	@Inject
	KubernetesClient kubernetesClient;

	@Inject
	ContainerUtility containerUtil;

	@Override
	@Transactional
	public void eventReceived(Action action, Service service) {
		var controlPlaneName = kindName + "-control-plane";
		var serviceType = service.getSpec().getType();
		var serviceName = service.getMetadata().getName();
		var serviceNamespace = service.getMetadata().getNamespace();
		LOGGER.log(Level.INFO, "Service Event {0} Received for {1} ",
				new Object[] {action, serviceName});

		LOGGER.log(Level.FINE, "Service Type {0}", serviceType);

		if (SERVICE_TYPE_NODE_PORT.equals(serviceType)) {
			List<ServicePort> servicePorts = service.getSpec().getPorts();
			var containers = DockerContainer
					.findContainers(kindName, serviceName,
							serviceNamespace);
			// Remove all cotainers for this service
			if (Action.MODIFIED == action) {
				LOGGER.log(Level.INFO,
						"Removing containers for service {0} ",
						serviceName);
				deleteContainers(containers);
			} else if (Action.DELETED == action) {
				LOGGER.log(Level.INFO,
						"Removing containers for service {0} ",
						serviceName);
				deleteContainers(containers);
				return;
			}

			for (ServicePort servicePort : servicePorts) {
				int port = servicePort.getPort();
				int nodePort = servicePort.getNodePort();
				String protocol = servicePort.getProtocol();
				if (nodePort != 0) {
					String containerName =
							String.format(DOCKER_CONTAINER_KEY_FORMAT,
									kindName,
									serviceName, serviceNamespace,
									protocol, port, nodePort);
					LOGGER.log(Level.FINE, "NodePort proxy {0} ",
							containerName);
					String portSpec =
							"127.0.0.1:" + nodePort + ":" + nodePort;

					// Start new container
					if (Action.ADDED == action
							|| Action.MODIFIED == action) {
						LOGGER.log(Level.INFO, "Run new container",
								containerName);
						addNew(controlPlaneName, serviceName,
								serviceNamespace, port, nodePort, protocol,
								containerName, portSpec);
					}
				}
			}
		}

	}

	@Override
	public void onClose(KubernetesClientException cause) {
		// TODO stop and kill the containers

	}

	private void addNew(String controlPlaneName, String serviceName,
			String serviceNamespace, int port, int nodePort, String protocol,
			String containerName, String portSpec) {
		String containerId = containerUtil.run(
				controlPlaneName, containerName,
				nodePort, protocol, portSpec);
		List<DockerContainer> containers =
				DockerContainer.findByContainerId(containerId);
		DockerContainer dockerContainer = null;
		if (containers.isEmpty()) { // INSERT
			dockerContainer =
					new DockerContainer();
			dockerContainer.clusterName = kindName;
			dockerContainer.containerId = containerId;
			dockerContainer.serviceName = serviceName;
			dockerContainer.serviceNamespace = serviceNamespace;
			dockerContainer.servicePort = port;
			dockerContainer.id = null;
		} else { // UPDATE
			dockerContainer = containers.get(0);
			dockerContainer.clusterName = kindName;
			dockerContainer.containerId = containerId;
			dockerContainer.serviceName = serviceName;
			dockerContainer.serviceNamespace = serviceNamespace;
			dockerContainer.servicePort = port;
		}

		dockerContainer.persist();
	}

	private void deleteContainers(List<DockerContainer> containers) {
		if (!containers.isEmpty()) {
			containers.stream().forEach(e -> {
				LOGGER.log(Level.INFO,
						"Deleting container with Id {0} ",
						e.containerId);
				containerUtil.stop(e.containerId);
				containerUtil.delete(e.containerId);
				e.delete();
			});
		}
	}

}
