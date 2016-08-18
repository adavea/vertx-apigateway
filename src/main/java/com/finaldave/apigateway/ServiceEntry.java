package com.finaldave.apigateway;

import java.io.Serializable;

public class ServiceEntry implements Serializable
{
	public String host = null;
	public String endPoint = null;
	public int port;

	public ServiceEntry(String endPoint, int port)
	{
		this.endPoint = endPoint;
		this.port = port;
	}

	public void setHost(String host)
	{
		this.host = host;
	}
}