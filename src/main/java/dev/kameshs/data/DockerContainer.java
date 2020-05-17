package dev.kameshs.data;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "docker_containers")
// TODO add indexes
public class DockerContainer extends PanacheEntity {

	@Column(name = "container_id", unique = true)
	public String containerId;
	@Column(name = "service_name")
	public String serviceName;
	@Column(name = "service_namespace")
	public String serviceNamespace;
	@Column(name = "service_port")
	public int servicePort;

	public static List<DockerContainer> findContainer(String serviceName,
			String serviceNamespace, int port) {
		return find("serviceName=?1 and serviceNamespace=?2 and servicePort=?3",
				serviceName, serviceNamespace, port).list();
	}

	public static List<DockerContainer> findByContainerId(String containerId) {
		return find("containerId", containerId).list();
	}
}
