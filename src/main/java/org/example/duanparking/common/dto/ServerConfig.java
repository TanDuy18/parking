package org.example.duanparking.common.dto;

import java.io.Serializable;
import java.util.Objects;

public class ServerConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String host;
    private int port;
    private String serviceName;

    public ServerConfig(String name, String host, int port, String serviceName) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.serviceName = serviceName;
    }

    // Getters
    public String getName() { return name; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getServiceName() { return serviceName; }

    public String getRmiUrl() {
        return "rmi://" + host + ":" + port + "/" + serviceName;
    }

    @Override
    public String toString() {
        return name + " (" + host + ":" + port + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ServerConfig that = (ServerConfig) obj;
        return port == that.port &&
                host.equals(that.host) &&
                serviceName.equals(that.serviceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, serviceName);
    }
}