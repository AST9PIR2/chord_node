package REST.server.computation;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import javax.ws.rs.core.Application;

public class ChordNodeApp extends Application {

    final static int SERVER_PORT = 8991;
    final static String SERVER_PATH_PREFIX = "/api";

    public static void main(String args[]) {
        String chordAddress = System.getenv("CHORD_ADDRESS");
        if (chordAddress == null || chordAddress.isEmpty()) {
            System.err.println("Error: CHORD_ADDRESS environment variable not set.");
            System.exit(1);
        }

        String existingNodeAddress = System.getenv("CHORD_NODE");

        ChordNodeService chordNodeService = new ChordNodeService(existingNodeAddress);

        Server server = new Server(SERVER_PORT);

        final ServletContextHandler context = new ServletContextHandler(server, "/");
        final ServletHolder restEasyServlet = new ServletHolder(new HttpServletDispatcher());
        restEasyServlet.setInitParameter("resteasy.servlet.mapping.prefix", SERVER_PATH_PREFIX);
        restEasyServlet.setInitParameter("javax.ws.rs.Application", ChordNodeApp.class.getCanonicalName());
        context.addServlet(restEasyServlet, SERVER_PATH_PREFIX + "/*");

        final ServletHolder defaultServlet = new ServletHolder(new DefaultServlet());
        context.addServlet(defaultServlet, "/");

        server.setHandler(context);

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
