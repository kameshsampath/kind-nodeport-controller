package dev.kameshs.data;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "docker_containers")
// TODO add indexes
public class DockerContainer extends PanacheEntity {

	@Column(name = "cluster_name", unique = true, nullable = false)
	public String clusterName;

	@Column(name = "container_id", unique = true, nullable = false)
	@NotNull
	public String containerId;

	@Column(name = "service_name", nullable = false)
	@NotNull
	public String serviceName;

	@Column(name = "service_namespace", nullable = false)
	@NotNull
	public String serviceNamespace;

	@Column(name = "service_port", nullable = false)
	@NotNull
	public int servicePort;

	public static List<DockerContainer> findContainers(String clusterName,
			String serviceName,
			String serviceNamespace, int port) {
		return find(
				"clusterName=?1 and serviceName=?2 and serviceNamespace=?3 and servicePort=?4",
				clusterName, serviceName, serviceNamespace, port).list();
	}

	public static List<DockerContainer> findContainers(String clusterName,
			String serviceName,
			String serviceNamespace) {
		return find(
				"clusterName=?1 and serviceName=?2 and serviceNamespace=?3",
				clusterName, serviceName, serviceNamespace).list();
	}

	public static List<DockerContainer> findByContainerId(String containerId) {
		return find("containerId", containerId).list();
	}
}
