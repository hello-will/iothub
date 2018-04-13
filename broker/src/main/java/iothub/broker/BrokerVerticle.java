package iothub.broker;

import io.vertx.core.AbstractVerticle;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;

import java.util.Map;

public class BrokerVerticle extends AbstractVerticle {

    private Server server;

    public void start() throws Exception {
        super.start();

        server = new Server();
        server.start(vertx);
    }

    public void stop() throws Exception {
        server.stop();

        super.stop();
    }
}
