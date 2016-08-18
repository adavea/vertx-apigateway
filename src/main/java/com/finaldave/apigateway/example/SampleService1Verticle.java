package com.finaldave.apigateway.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.core.Vertx;
import java.net.InetSocketAddress;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import com.finaldave.apigateway.*;

public class SampleService1Verticle extends AbstractServiceVerticle {

    public static void main(String[] args)
    {
        VertxOptions op = new VertxOptions();
        op.setClustered(true);
        Vertx.clusteredVertx(op, e -> {
            if (e.succeeded()) {
                //Deploying api gateway
                System.out.println("Deploying api gateway");
                e.result().deployVerticle("com.finaldave.apigateway.APIGateway");

                //Deploying sample services
                System.out.println("Deploying sample services");
                DeploymentOptions deploymentOptions = new DeploymentOptions();
                deploymentOptions.setInstances(3);
                e.result().deployVerticle("com.finaldave.apigateway.example.SampleService1Verticle", deploymentOptions);

            } else {
                e.cause().printStackTrace();
            }
        });
    }

    public void start() {
        //Create our server and routing objects
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);

        router.route().handler(CorsHandler.create("*")
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.POST)
            .allowedMethod(HttpMethod.OPTIONS)
            .allowedHeader("X-PINGARUNER")
            .allowedHeader("Content-Type"));

        //Handlers for game state
        router.get("/sampleService1").handler(this::handleServiceRequest);
        router.get("/sampleService1/:id").handler(this::handleServiceRequestWithId);

        //Start up our web services server and listen on the given port
        server.requestHandler(router::accept).listen(8089);

        //Add our services to our service registry so our api gateway
        //can pick them up
        addService(new ServiceEntry("/sampleService1", 8089));
        startAutoRegistry();
    }

    public void stop() 
    {
        deregisterServices();
    }

    protected void handleServiceRequest(RoutingContext routingContext) {
        //Delay here, so if you refresh your browser with ctrl+shift+R you
        //will actually get different instances of these services
        try {
            Thread.sleep(4000);
        } catch(Exception e) {}

        routingContext.response().end("Hello from the sample service (" + id + "), no id");
    }

    protected void handleServiceRequestWithId(RoutingContext routingContext) {
        //Delay here, so if you refresh your browser with ctrl+shift+R you
        //will actually get different instances of these services
        try {
            Thread.sleep(4000);
        } catch(Exception e) {}

        routingContext.response().end("Hello from the sample service (" + id + "), with id... can't really be bothered to fetch the id or anything... just saying there is one");
    }
}