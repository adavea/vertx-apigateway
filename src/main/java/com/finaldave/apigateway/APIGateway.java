package com.finaldave.apigateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import com.hazelcast.core.IMap;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Hazelcast;
import com.finaldave.apigateway.selectionstrategy.*;

public class APIGateway extends AbstractServiceVerticle 
{
	IMap<String,ServiceEntry> availableServices;
    HazelcastInstance hazelcastInstance;
    int proxyPort = 8084;
    SelectionStrategy<String> selectionStrategy = new RoundRobinStrategy<String>();
    
    public void start() {
    	//Convenience variables
        hazelcastInstance = Hazelcast.getAllHazelcastInstances().iterator().next();
        availableServices = hazelcastInstance.getMap(AbstractServiceVerticle.servicesMapName);

        //Go ahead and periodically print out all available services
		long timerID = vertx.setPeriodic(30000, id -> {
			System.out.println("Printing out all available service keys");
            for(String key : availableServices.keySet())
            {
                System.out.println(key);
            }
	    });

		//Register ourself so other services know they can
		//proxy via this gateway to get to get to other registered
		//services hosted anywhere
        addService(new ServiceEntry("apigateway", proxyPort));
        startAutoRegistry();

		//Start our http server that will receive incoming requests then route
		//them to our services.  This code is mostly based on the vertx
		//example Proxy code, especially the part that isn't commented very well ;p
        HttpClient client = vertx.createHttpClient(new HttpClientOptions());
	    vertx.createHttpServer().requestHandler(req -> {
			System.out.println("Proxying request: " + req.uri());

			//Select a registered service that can handle requests
			//to the given uri.  note that requests to /actions/blah/123
			//can be routed to any service registered as an /actions
			//service
			ServiceEntry serviceInfo = getServiceInfo(req.uri(),
				availableServices, selectionStrategy);

			//Proxy example code for the most part here down
			HttpClientRequest c_req = client.request(
				req.method(), 
				serviceInfo.port, 
				serviceInfo.host, 
				req.uri(), 
				c_res -> {
			        System.out.println("Proxying response: " + c_res.statusCode());
			        req.response().setChunked(true);
			        req.response().setStatusCode(c_res.statusCode());
			        req.response().headers().setAll(c_res.headers());
			        c_res.handler(data -> {
			          System.out.println("Proxying response body: " + data.toString("ISO-8859-1"));
			          req.response().write(data);
			        });
			        c_res.endHandler((v) -> req.response().end());
				}
			);
			c_req.setChunked(true);
			c_req.headers().setAll(req.headers());
			req.handler(data -> {
				System.out.println("Proxying request body " + data.toString("ISO-8859-1"));
				c_req.write(data);
			});
			req.endHandler((v) -> c_req.end());
		}).listen(proxyPort);
    }

    public void stop() 
    {
        deregisterServices();
    }
}