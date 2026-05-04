package com.helma.helmabackend.service.core;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public abstract class GenericServiceImpl<T, ID> implements IGenericService<T, ID> {

    protected abstract JpaRepository<T, ID> getRepository();

    @Override
    public T add(T entity) {
        return getRepository().save(entity);
    }

    @Override
    public T update(T entity) {
        return getRepository().save(entity);
    }

    @Override
    public T retrieve(ID id) {
        return getRepository().findById(id).orElse(null);
    }

    @Override
    public List<T> retrieveAll() {
        return getRepository().findAll();
    }

    @Override
    public void remove(ID id) {
        getRepository().deleteById(id);
    }
}
