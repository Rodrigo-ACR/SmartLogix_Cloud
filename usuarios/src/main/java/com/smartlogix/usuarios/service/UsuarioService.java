package com.smartlogix.usuarios.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.smartlogix.usuarios.model.Rol;
import com.smartlogix.usuarios.model.Usuario;
import com.smartlogix.usuarios.repository.UsuarioRepository;

import com.smartlogix.usuarios.dto.LoginResponse;
import com.smartlogix.usuarios.dto.RegisterRequest;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public List<Usuario> listarTodos() {
        return repository.findAll();
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Usuario guardar(Usuario usuario) {
        return repository.save(usuario);
    }

    public Usuario actualizar(Long id, Usuario usuario) {

        Usuario existente = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (usuario.getNombre() != null)
            existente.setNombre(usuario.getNombre());
        if (usuario.getCorreo() != null)
            existente.setCorreo(usuario.getCorreo());
        if (usuario.getPassword() != null && !usuario.getPassword().isEmpty())
            existente.setPassword(usuario.getPassword());
        if (usuario.getRol() != null)
            existente.setRol(usuario.getRol());
        if (usuario.getTelefono() != null)
            existente.setTelefono(usuario.getTelefono());
        if (usuario.getDireccion() != null)
            existente.setDireccion(usuario.getDireccion());
        if (usuario.getActivo() != null)
            existente.setActivo(usuario.getActivo());

        return repository.save(existente);
    }

    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado");
        }
        repository.deleteById(id);
    }

    public LoginResponse login(String correo, String password) {

        Usuario usuario = repository
                .findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Validación de usuario inactivo (corrige TF-03)
        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new RuntimeException("Usuario inhabilitado");
        }

        if (!usuario.getPassword().equals(password)) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        return new LoginResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol().name(),
                usuario.getDireccion(),
                usuario.getTelefono());
    }

    public Usuario register(RegisterRequest request) {

        if (repository.findByCorreo(request.getCorreo()).isPresent()) {
            throw new RuntimeException("El correo ya está registrado");
        }

        Usuario usuario = new Usuario();

        usuario.setNombre(request.getNombre());
        usuario.setCorreo(request.getCorreo());
        usuario.setPassword(request.getPassword());
        usuario.setTelefono(request.getTelefono());
        usuario.setDireccion(request.getDireccion());
        usuario.setRol(Rol.CLIENTE);
        usuario.setActivo(true);

        return repository.save(usuario);
    }
}