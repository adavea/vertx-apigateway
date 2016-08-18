package com.finaldave.apigateway;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

class Launcher
{
    public static void main(String[] args)
    {
        VertxOptions op = new VertxOptions();
        op.setClustered(true);
        Vertx.clusteredVertx(op, e -> {
            if (e.succeeded()) {
                DeploymentOptions deploymentOptions = new DeploymentOptions();
                deploymentOptions.setInstances(2);

                //Deploying api gateway
                System.out.println("Deploying api gateway");
                e.result().deployVerticle("com.finaldave.apigateway.APIGateway", deploymentOptions);
            } else {
                e.cause().printStackTrace();
            }
        });
    }
}
