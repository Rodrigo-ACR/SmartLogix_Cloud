package com.smartlogix.usuarios.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.smartlogix.usuarios.dto.LoginRequest;
import com.smartlogix.usuarios.dto.LoginResponse;
import com.smartlogix.usuarios.dto.RegisterRequest;
import com.smartlogix.usuarios.model.Usuario;
import com.smartlogix.usuarios.service.UsuarioService;

@RestController
@RequestMapping("/usuarios")
@CrossOrigin("*")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    // GET todos
    @GetMapping
    public List<Usuario> listar() {
        return service.listarTodos();
    }

    // GET por id
    @GetMapping("/{id}")
    public ResponseEntity<Object> buscar(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(u -> ResponseEntity.ok((Object) u))
                .orElse(ResponseEntity.status(404).body("Usuario no encontrado"));
    }

    // POST crear (admin)
    @PostMapping
    public ResponseEntity<Usuario> guardar(@RequestBody Usuario usuario) {
        return ResponseEntity.status(201).body(service.guardar(usuario));
    }

    // PUT actualizar
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizar(@PathVariable Long id,
                                               @RequestBody Usuario usuario) {
        return ResponseEntity.ok(service.actualizar(id, usuario));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.ok("Usuario eliminado");
    }

    // POST login
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(service.login(request.getCorreo(), request.getPassword()));
    }

    // POST register
    @PostMapping("/register")
    public ResponseEntity<Usuario> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(service.register(request));
    }
}