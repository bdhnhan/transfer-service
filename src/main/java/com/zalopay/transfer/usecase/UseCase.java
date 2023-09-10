package com.zalopay.transfer.usecase;

public interface UseCase<T,V> {
    V handle(T request);
}
