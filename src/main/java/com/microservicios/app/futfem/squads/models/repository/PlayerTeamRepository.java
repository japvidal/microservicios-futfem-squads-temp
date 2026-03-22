package com.microservicios.app.futfem.squads.models.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.microservicios.app.futfem.squads.models.entity.PlayerTeam;

public interface PlayerTeamRepository extends CrudRepository<PlayerTeam, Long> {

	@Query("select pt from PlayerTeam pt where pt.idTeam = ?1 and pt.season = ?2 order by CAST(pt.dorsal AS int) asc ")  
	public List<PlayerTeam> findByIdTeam(Long id, String season );
	
	@Query("select pt from PlayerTeam pt where pt.season = ?1 order by CAST(pt.dorsal AS int) asc ")  
	public List<PlayerTeam> findBySeason(String season );
}
