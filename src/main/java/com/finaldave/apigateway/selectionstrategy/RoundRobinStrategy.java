package com.finaldave.apigateway.selectionstrategy;

import java.util.Random;
import java.util.Arrays;

public class RoundRobinStrategy<T> implements SelectionStrategy<T>
{
	int lastSelectedIndex = -1;

	public T selectService(T[] services)
	{
        Arrays.sort(services);

		lastSelectedIndex++;		
		if(lastSelectedIndex >= services.length)
		{
			lastSelectedIndex = 0;
		}

		return services[lastSelectedIndex];
	}
}	