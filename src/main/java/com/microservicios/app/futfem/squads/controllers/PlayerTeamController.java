package com.microservicios.app.futfem.squads.controllers;

import java.io.IOException;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.microservicios.app.common.controllers.CommonController;
import com.microservicios.app.futfem.squads.models.entity.PlayerTeam;
import com.microservicios.app.futfem.squads.services.PlayerTeamService;

@RestController
public class PlayerTeamController extends CommonController<PlayerTeam, PlayerTeamService> {
	
	@GetMapping ("/{idTeam}/{season}/listByIdTeam")
	public ResponseEntity<?> listByIdTeam(@PathVariable Long idTeam,@PathVariable String season){
		return ResponseEntity.ok().body(service.findByIdTeam(idTeam, season));
	}
	
	@GetMapping ("/{season}/listBySeason")
	public ResponseEntity<?> listBySeason(@PathVariable String season){
		return ResponseEntity.ok().body(service.findBySeason(season));
	}

	@PostMapping(value = "/exportPlayerTeam", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> exportPlayerTeam(@RequestParam("excel") MultipartFile excel,
			@RequestParam("season") String season) throws IOException {
		if (excel.isEmpty() || season == null || season.trim().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		byte[] content = service.exportPlayerTeam(excel, season.trim());
		ByteArrayResource resource = new ByteArrayResource(content);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=log_registros.xlsx")
				.contentType(MediaType.parseMediaType(
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.contentLength(content.length)
				.body(resource);
	}
}
