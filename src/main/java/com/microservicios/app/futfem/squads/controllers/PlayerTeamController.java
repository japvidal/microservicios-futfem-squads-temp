package com.microservicios.app.futfem.squads.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.microservicios.app.common.controllers.CommonController;
import com.microservicios.app.futfem.squads.models.entity.PlayerTeam;
import com.microservicios.app.futfem.squads.services.PlayerTeamService;

@RestController
public class PlayerTeamController extends CommonController<PlayerTeam, PlayerTeamService> {
	
	@GetMapping ("/{idTeam}/{season}/listByIdTeam")  // sirve para dar una ruta al RestController, es un método Handler
	public ResponseEntity<?> listByIdTeam(@PathVariable Long idTeam,@PathVariable String season){
		return ResponseEntity.ok().body(service.findByIdTeam(idTeam, season));
	}
	
	@GetMapping ("/{season}/listBySeason")  // sirve para dar una ruta al RestController, es un método Handler
	public ResponseEntity<?> listBySeason(@PathVariable String season){
		return ResponseEntity.ok().body(service.findBySeason(season));
	}
}
