package com.budgetwise;

import com.budgetwise.controller.ApiServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.resource.Resource;
import java.nio.file.Path;

/** Starts a small Jetty server and serves the static frontend alongside the JSON API. */
public class BudgetWiseApplication {
    public static void main(String[] args) throws Exception {
        int port=Integer.parseInt(System.getenv().getOrDefault("PORT","8080"));
        Server server=new Server(port);
        ServletContextHandler api=new ServletContextHandler(ServletContextHandler.SESSIONS);
        api.setContextPath("/api"); api.addServlet(new ServletHolder(new ApiServlet()),"/*");
        ResourceHandler files=new ResourceHandler(); files.setDirectoriesListed(false); files.setWelcomeFiles(new String[]{"index.html"});
        files.setBaseResource(Resource.newResource(Path.of("frontend").toAbsolutePath()));
        ContextHandler staticContext=new ContextHandler("/"); staticContext.setHandler(files);
        HandlerList handlers=new HandlerList(); handlers.addHandler(api); handlers.addHandler(staticContext); server.setHandler(handlers);
        server.start(); System.out.println("BudgetWise is running at http://localhost:"+port); server.join();
    }
}
