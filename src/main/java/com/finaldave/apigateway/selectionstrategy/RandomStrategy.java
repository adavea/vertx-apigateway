package com.finaldave.apigateway.selectionstrategy;

import java.util.Random;

public class RandomStrategy<T> implements SelectionStrategy<T>
{
	Random rand = new Random();

	public T selectService(T[] services)
	{
		return services[rand.nextInt(services.length)];
	}
}