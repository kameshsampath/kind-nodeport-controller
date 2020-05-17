package dev.kameshs.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.core.DockerClientBuilder;

@ApplicationScoped
public class ContainerUtility {

	private static final String KIND = "kind";

	private static final Logger LOGGER =
			Logger.getLogger(ContainerUtility.class.getName());

	String SOCAT_OPTION_LISTEN_FORMAT = "tcp-listen:%d,fork,reuseaddr";
	String SOCAT_OPTION_CONNECT_FORMAT = "tcp-connect:%s:%d";

	DockerClient client;

	@PostConstruct
	void init() {
		client = DockerClientBuilder.getInstance().build();
	}

	public String run(String controlPlaneName, String containerName,
			int nodePort,
			String protocol,
			String portSpec) {
		LOGGER.info("Starting Container");
		var socatListen = String.format(
				SOCAT_OPTION_LISTEN_FORMAT,
				nodePort);
		var socatConnect = String.format(
				SOCAT_OPTION_CONNECT_FORMAT,
				controlPlaneName,
				nodePort);
		ExposedPort exposedPort = new ExposedPort(nodePort,
				InternetProtocol.parse(protocol));
		Binding binding = new Binding("127.0.0.1", String.valueOf(nodePort));
		PortBinding portBinding = new PortBinding(binding, exposedPort);
		CreateContainerResponse containerResponse =
				client.createContainerCmd("alpine/socat")
						.withNetworkMode(KIND)
						.withName(containerName)
						.withExposedPorts(exposedPort)
						.withPortSpecs(portSpec)
						.withPortBindings(portBinding)
						.withCmd("-dd", socatListen, socatConnect)
						.exec();
		String containerId = containerResponse.getId();
		client.startContainerCmd(containerId).exec();

		return containerId;
	}

	public void stop(String containerId) {
		LOGGER.log(Level.INFO, "Stopping Container {0} ", containerId);
		client.stopContainerCmd(containerId).exec();
	}

	public void delete(String containerId) {
		LOGGER.log(Level.INFO, "Kill and Remove Container {0} ",
				containerId);
		client.removeContainerCmd(containerId).exec();
	}
}
