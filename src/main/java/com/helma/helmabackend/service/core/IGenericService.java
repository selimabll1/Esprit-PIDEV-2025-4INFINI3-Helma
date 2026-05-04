package com.helma.helmabackend.service.core;

import java.util.List;

public interface IGenericService<T, ID> {
    T add(T entity);
    T update(T entity);
    T retrieve(ID id);
    List<T> retrieveAll();
    void remove(ID id);
}
