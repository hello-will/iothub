package iothub.auth;

import io.vertx.core.Launcher;

public class AuthLauncher {

    public static void main(String[] args) {
        Launcher launcher = new Launcher();
        String[] newArgs = new String[args.length + 1];
        newArgs[0] = AuthVerticle.class.getName();
        System.arraycopy(args, 0, newArgs, 1, args.length);
        launcher.execute("run", newArgs);
    }

}
