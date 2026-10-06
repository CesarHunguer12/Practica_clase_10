package com.criscano.project;

import com.criscano.project.application.dto.UsuarioDto;
import com.criscano.project.application.service.UsuarioService;
import com.criscano.project.domain.Usuario;
import com.criscano.project.infrastructure.persistence.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:auditing;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"app.demo.enabled=false"
})
class ProjectApplicationTests {

	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	private UsuarioRepository usuarioRepository;

	private Long usuarioId;

	@Test
	void contextLoads() {
	}

	@Test
	void auditFieldsArePopulatedWhenUserIsCreatedUpdatedAndSoftDeleted() {
		setCurrentUser("auditoria-creacion");
		UsuarioDto creado = usuarioService.crear(new UsuarioDto(
				null, "Usuario de auditoria", "auditoria-" + System.nanoTime() + "@example.com",
				null, null, null, null, null, null, false
		));
		usuarioId = creado.getId();

		assertEquals("auditoria-creacion", creado.getCreatedBy());
		assertNotNull(creado.getCreatedAt());

		setCurrentUser("auditoria-actualizacion");
		UsuarioDto actualizado = usuarioService.actualizar(usuarioId, new UsuarioDto(
				null, "Usuario actualizado", creado.getEmail(),
				null, null, null, null, null, null, false
		));

		assertEquals("auditoria-actualizacion", actualizado.getUpdatedBy());
		assertNotNull(actualizado.getUpdatedAt());

		setCurrentUser("auditoria-eliminacion");
		usuarioService.eliminar(usuarioId);
		Usuario eliminado = usuarioRepository.findById(usuarioId).orElseThrow();

		assertTrue(eliminado.isDeleted());
		assertEquals("auditoria-eliminacion", eliminado.getDeletedBy());
		assertNotNull(eliminado.getDeletedAt());
	}

	@AfterEach
	void limpiar() {
		if (usuarioId != null) {
			usuarioRepository.deleteById(usuarioId);
		}
		RequestContextHolder.resetRequestAttributes();
	}

	private void setCurrentUser(String user) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-User", user);
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}
}
