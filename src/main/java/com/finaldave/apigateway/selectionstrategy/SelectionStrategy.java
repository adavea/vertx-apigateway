package com.finaldave.apigateway.selectionstrategy;

public interface SelectionStrategy<T>
{
	T selectService(T[] services);
}

