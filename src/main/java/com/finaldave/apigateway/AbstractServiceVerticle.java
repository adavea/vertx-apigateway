package com.finaldave.apigateway;

import io.vertx.core.AbstractVerticle;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import io.vertx.core.json.Json;
import java.util.HashMap;
import java.util.ArrayList;
import com.finaldave.apigateway.selectionstrategy.*;

public abstract class AbstractServiceVerticle extends AbstractVerticle
{
    private static int lastId = 0;

	public static String servicesMapName = "availableServices";
    private int registrationHeartbeat = 15000; //in milliseconds
	private long registrationTimerId = -1;
    protected int id = -1;
    protected HazelcastInstance hazelcastInstance;
    private HashMap<String, ServiceEntry> services = new HashMap<String, ServiceEntry>();

    public AbstractServiceVerticle()
    {
    	//We need a unique id for each service on this machine
    	lastId++;
        id = lastId;

        //Convenience variable for this hazelcast instance
        hazelcastInstance = Hazelcast.getAllHazelcastInstances().iterator().next();
    }

    protected String getServiceKey(String endPoint)
    {
    	return endPoint + "|" + id + "|" + deploymentID();
    }

    protected ServiceEntry addService(ServiceEntry entry)
    {
	    String key = getServiceKey(entry.endPoint);
    	entry.setHost(hazelcastInstance.getCluster().getLocalMember().getSocketAddress().getHostString());

	    return services.put(key, entry);
    }
    protected ServiceEntry removeService(ServiceEntry entry)
    {
	    String key = getServiceKey(entry.endPoint);

    	return services.remove(key);
    }

    /**
     * Starts a timer that will automatically register this verticles
     * services every so often
     */
    protected long startAutoRegistry()
    {
    	return startAutoRegistry(registrationHeartbeat);
    }

    protected long startAutoRegistry(int registrationHeartbeat)
    {
    	this.registrationHeartbeat = registrationHeartbeat;

        //Every so many seconds, make sure we are registered in the cluster's
        //shared memory as a service
        registerServices();
        registrationTimerId = vertx.setPeriodic(registrationHeartbeat, id -> {
            registerServices();
        });

        return registrationTimerId;
    }

    /**
     *  Stops the services in this verticle from periodically
     *  writing themselves to the registry for the api gateway
     */
    protected boolean stopAutoRegistry()
    {
    	if(registrationTimerId != -1)
    	{
    		vertx.cancelTimer(registrationTimerId);
    		registrationTimerId = -1;
    		return true;
    	}

    	return false;
    }


    /**
     *  Deregisters all of our services from the cluster/gateway
     */
    protected void deregisterServices()
    {
        //Just exit if hazelcast hasn't finished initializing yet
        if(hazelcastInstance == null)
        {
            System.out.println("Ignoring attempt to deregister service--hazelcast isn't instantiated yet");
            return;
        }

        //Remove all our services
        IMap<String, ServiceEntry> registeredServices = hazelcastInstance.getMap(servicesMapName);
        for(String key : services.keySet())
        {
	        registeredServices.remove(key);
        }

        System.out.println(services.size() + " services deregistered");
    }

    /**
     * Registers all of our services with the cluster/gateway
     */
    protected void registerServices()
    {
        //Just exit if hazelcast hasn't finished initializing yet
        if(hazelcastInstance == null)
        {
            System.out.println("Ignoring attempt to register service--hazelcast isn't instantiated yet");
            return;
        }

        IMap<String, ServiceEntry> registeredServices = hazelcastInstance.getMap(servicesMapName);
        registeredServices.putAll(services);

        System.out.println(services.size() + " services registered");
    }

    protected ServiceEntry getServiceInfo(String uri, IMap<String, ServiceEntry> availableServices, SelectionStrategy<String> selectionStrategy)
    {
		//Try to find a service that can handle this request.
		//It is assumed that all microservices that handle requests under
		//the same root (e.g. /actions, /users) live in the same places.
		//For instance if a server can handle actions/1 it can also
		//handle actions/someSubsetofActions and actions/theCoolAction.
        int slashIndex = uri.indexOf('/');
        String serviceRoot = (slashIndex != -1) ? uri.substring(0, slashIndex) : uri;
        ArrayList<String> serviceKeys = new ArrayList<String>();
        for(String key : availableServices.keySet())
        {
        	if(key.startsWith(serviceRoot))
        	{
        		serviceKeys.add(key);
        	}
        }

        //If we couldn't find a relevant service just return
        if(serviceKeys.size() == 0)
        {
        	System.out.println("Could not find a service for request to " + serviceRoot);
        	return null;
        }

        //Apply our selection strategy to our set (aka select a service address to query)
        String[] serviceKeysArray = serviceKeys.toArray(new String[serviceKeys.size()]);
        String selectedServiceKey = selectionStrategy.selectService(serviceKeysArray);
		ServiceEntry serviceInfo = availableServices.get(selectedServiceKey);
		System.out.println("Selecting service with key " + selectedServiceKey);

		return serviceInfo;
    }
}