package iothub.pubsub;

import io.vertx.core.Launcher;

public class PubSubLauncher {

    public static void main(String[] args) {
        Launcher launcher = new Launcher();
        String[] newArgs = new String[args.length + 1];
        newArgs[0] = PubSubVerticle.class.getName();
        System.arraycopy(args, 0, newArgs, 1, args.length);
        launcher.execute("run", newArgs);
    }

}
