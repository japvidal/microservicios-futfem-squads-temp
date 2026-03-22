package com.microservicios.app.futfem.squads.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.microservicios.app.common.services.CommonServiceImpl;
import com.microservicios.app.futfem.squads.models.entity.PlayerTeam;
import com.microservicios.app.futfem.squads.models.repository.PlayerTeamRepository;

@Service
public class PlayerTeamServiceImpl extends CommonServiceImpl<PlayerTeam, PlayerTeamRepository> implements PlayerTeamService {

	public List<PlayerTeam> findByIdTeam(Long idTeam, String season){
		return repository.findByIdTeam(idTeam, season);
	}
	
	public List<PlayerTeam> findBySeason(String season){
		return repository.findBySeason(season);
	}
}
