package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.UserDto;
import com.esprit.helma_backend.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {


    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto.Response create(@Valid @RequestBody UserDto.Create req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public UserDto.Response getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public List<UserDto.Response> getAll() {
        return service.getAll();
    }

    @PutMapping("/{id}")
    public UserDto.Response update(@PathVariable Long id, @Valid @RequestBody UserDto.Update req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
