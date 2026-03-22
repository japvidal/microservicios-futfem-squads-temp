package com.microservicios.app.futfem.squads.services;

import java.util.List;

import com.microservicios.app.common.services.CommonService;
import com.microservicios.app.futfem.squads.models.entity.PlayerTeam;

public interface PlayerTeamService extends CommonService<PlayerTeam>{
	
	public List<PlayerTeam> findByIdTeam(Long idTeam, String season);

	public List<PlayerTeam> findBySeason(String season);
}
