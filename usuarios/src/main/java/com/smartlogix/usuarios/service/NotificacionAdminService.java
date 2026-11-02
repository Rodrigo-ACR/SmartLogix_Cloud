package com.smartlogix.usuarios.service;

import com.smartlogix.usuarios.dto.LogisticaEventoDTO;
import com.smartlogix.usuarios.model.Rol;
import com.smartlogix.usuarios.model.Usuario;
import com.smartlogix.usuarios.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Logica de dominio: Usuarios es quien conoce a las personas, asi que cuando
 * Inventario emite una alerta de stock bajo, Usuarios decide a quien avisar
 * (los administradores). El envio real del correo se simula con un log.
 */
@Service
public class NotificacionAdminService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionAdminService.class);

    private final UsuarioRepository repository;

    public NotificacionAdminService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public void procesar(LogisticaEventoDTO evento) {
        if (evento.tipo() != LogisticaEventoDTO.Tipo.STOCK_BAJO) {
            return; // los eventos de envio no generan notificaciones a administradores
        }

        List<Usuario> admins = repository.findByRol(Rol.ADMIN);
        if (admins.isEmpty()) {
            log.warn("[KAFKA][USUARIOS] Stock bajo del producto {} pero no hay administradores registrados",
                    evento.productoId());
            return;
        }

        for (Usuario admin : admins) {
            log.info("[KAFKA][USUARIOS][NOTIFICACION] Alerta de stock bajo -> {} <{}>: '{}' (id={}) quedo con {} unidades (umbral {})",
                    admin.getNombre(), admin.getCorreo(), evento.nombreProducto(), evento.productoId(),
                    evento.stockActual(), evento.umbral());
        }
    }
}
