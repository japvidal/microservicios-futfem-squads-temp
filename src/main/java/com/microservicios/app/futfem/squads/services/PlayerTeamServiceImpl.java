package com.microservicios.app.futfem.squads.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.microservicios.app.common.services.CommonServiceImpl;
import com.microservicios.app.futfem.squads.models.entity.PlayerTeam;
import com.microservicios.app.futfem.squads.models.repository.PlayerTeamRepository;

@Service
public class PlayerTeamServiceImpl extends CommonServiceImpl<PlayerTeam, PlayerTeamRepository> implements PlayerTeamService {

	private static final Logger log = LoggerFactory.getLogger(PlayerTeamServiceImpl.class);
	private static final String PLAYERS_SERVICE_URL = "http://microservicio-futfem-players-temp";
	private static final String TEAMS_SERVICE_URL = "http://microservicio-futfem-teams-temp";
	private static final String DEFAULT_TEAM_COUNTRY = "ES";
	private static final DateTimeFormatter INPUT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

	@Autowired
	private RestTemplate restTemplate;

	@Override
	public List<PlayerTeam> findByIdTeam(Long idTeam, String season){
		return repository.findByIdTeam(idTeam, season);
	}
	
	@Override
	public List<PlayerTeam> findBySeason(String season){
		return repository.findBySeason(season);
	}

	@Override
	@Transactional
	public byte[] exportPlayerTeam(MultipartFile excel, String season) throws IOException {
		try (Workbook workbook = WorkbookFactory.create(excel.getInputStream());
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.getSheetAt(0);
			DataFormatter formatter = new DataFormatter();
			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
			Map<String, Integer> headers = readHeaders(sheet.getRow(0), formatter, evaluator);

			int playerColumn = appendHeader(sheet.getRow(0), "Player");
			int teamColumn = appendHeader(sheet.getRow(0), "Team");
			int squadColumn = appendHeader(sheet.getRow(0), "Squad");

			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				if (row == null || isRowEmpty(row, formatter, evaluator)) {
					continue;
				}

				ImportedRow importedRow = buildImportedRow(row, headers, formatter, evaluator);
				PlayerResolution player = resolvePlayer(importedRow);
				TeamResolution team = resolveTeam(importedRow);
				SquadResolution squad = resolveSquad(team.id(), player.id(), season, importedRow.dorsal());

				row.createCell(playerColumn).setCellValue(player.message());
				row.createCell(teamColumn).setCellValue(team.message());
				row.createCell(squadColumn).setCellValue(squad.message());
			}

			workbook.write(outputStream);
			byte[] content = outputStream.toByteArray();
			saveLogFile(content);
			return content;
		}
	}

	private ImportedRow buildImportedRow(Row row, Map<String, Integer> headers, DataFormatter formatter,
			FormulaEvaluator evaluator) {
		log.debug("Init method PlayerTeamServiceImpl.buildImportedRow");
		String club = readCell(row, headers, formatter, evaluator, "club");
		String dorsal = readCell(row, headers, formatter, evaluator, "dorsal");
		String name = readCell(row, headers, formatter, evaluator, "nombre");
		String firstSurname = readCell(row, headers, formatter, evaluator, "primer apellido");
		String secondSurname = readCell(row, headers, formatter, evaluator, "segundo apellido");
		String nickname = readCell(row, headers, formatter, evaluator, "apodo");
		String country = readCell(row, headers, formatter, evaluator, "pais");
		String birthdate = normalizeBirthdate(readCell(row, headers, formatter, evaluator, "fecha de nacimiento"));
		if (birthdate == null) {
			birthdate = normalizeBirthdate(readCell(row, headers, formatter, evaluator, "fecha nacimiento"));
		}
		String position = readCell(row, headers, formatter, evaluator, "posicion");

		return new ImportedRow(club, dorsal, name, mergeSurnames(firstSurname, secondSurname), nickname, country,
				birthdate, position);
	}

	private PlayerResolution resolvePlayer(ImportedRow importedRow) {
		log.debug("Init method PlayerTeamServiceImpl.resolvePlayer");
		PlayerLookupRequest lookupRequest = new PlayerLookupRequest(importedRow.name(), importedRow.surname(),
				importedRow.birthdate());
		try {
			ResponseEntity<Long> response = restTemplate.postForEntity(
					PLAYERS_SERVICE_URL + "/getIdByName",
					lookupRequest,
					Long.class);
			Long id = response.getBody();
			if (id == null) {
				throw new IllegalStateException("No se pudo recuperar el id de la jugadora existente.");
			}
			return new PlayerResolution(id, false);
		} catch (HttpClientErrorException.NotFound ex) {
			PlayerPayload playerPayload = new PlayerPayload();
			playerPayload.setName(importedRow.name());
			playerPayload.setSurname(importedRow.surname());
			playerPayload.setNickname(importedRow.nickname());
			playerPayload.setCountry(importedRow.country());
			playerPayload.setPosition(importedRow.position());
			playerPayload.setBirthdate(parseDate(importedRow.birthdate()));

			ResponseEntity<PlayerPayload> response = restTemplate.postForEntity(
					PLAYERS_SERVICE_URL + "/",
					playerPayload,
					PlayerPayload.class);
			PlayerPayload createdPlayer = response.getBody();
			if (createdPlayer == null || createdPlayer.getId() == null) {
				throw new IllegalStateException("No se pudo crear la jugadora.");
			}
			return new PlayerResolution(createdPlayer.getId(), true);
		}
	}

	private TeamResolution resolveTeam(ImportedRow importedRow) {
		log.debug("Init method PlayerTeamServiceImpl.resolveTeam");
		TeamLookupRequest lookupRequest = new TeamLookupRequest(importedRow.club(), DEFAULT_TEAM_COUNTRY);
		try {
			ResponseEntity<Long> response = restTemplate.postForEntity(
					TEAMS_SERVICE_URL + "/getIdByName",
					lookupRequest,
					Long.class);
			Long id = response.getBody();
			if (id == null) {
				throw new IllegalStateException("No se pudo recuperar el id del equipo existente.");
			}
			return new TeamResolution(id, false);
		} catch (HttpClientErrorException.NotFound ex) {
			TeamPayload teamPayload = new TeamPayload();
			teamPayload.setName(importedRow.club());
			teamPayload.setCountry(DEFAULT_TEAM_COUNTRY);

			ResponseEntity<TeamPayload> response = restTemplate.postForEntity(
					TEAMS_SERVICE_URL + "/",
					teamPayload,
					TeamPayload.class);
			TeamPayload createdTeam = response.getBody();
			if (createdTeam == null || createdTeam.getId() == null) {
				throw new IllegalStateException("No se pudo crear el equipo.");
			}
			return new TeamResolution(createdTeam.getId(), true);
		}
	}

	private SquadResolution resolveSquad(Long teamId, Long playerId, String season, String dorsal) {
		log.debug("Init method PlayerTeamServiceImpl.resolveSquad");
		Optional<PlayerTeam> existing = repository.findByIdTeamAndIdPlayerAndSeason(teamId, playerId, season);
		if (existing.isPresent()) {
			return new SquadResolution(existing.get().getId(), false);
		}

		PlayerTeam playerTeam = new PlayerTeam();
		playerTeam.setIdTeam(teamId);
		playerTeam.setIdPlayer(playerId);
		playerTeam.setSeason(season);
		playerTeam.setDorsal(dorsal);

		PlayerTeam created = repository.save(playerTeam);
		return new SquadResolution(created.getId(), true);
	}

	private Map<String, Integer> readHeaders(Row headerRow, DataFormatter formatter, FormulaEvaluator evaluator) {
		log.debug("Init method PlayerTeamServiceImpl.readHeaders");
		Map<String, Integer> headers = new HashMap<>();
		for (Cell cell : headerRow) {
			headers.put(normalizeHeader(formatter.formatCellValue(cell, evaluator)), cell.getColumnIndex());
		}
		return headers;
	}

	private int appendHeader(Row headerRow, String value) {
		log.debug("Init method PlayerTeamServiceImpl.appendHeader");
		int columnIndex = headerRow.getLastCellNum() >= 0 ? headerRow.getLastCellNum() : 0;
		headerRow.createCell(columnIndex).setCellValue(value);
		return columnIndex;
	}

	private boolean isRowEmpty(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
		log.debug("Init method PlayerTeamServiceImpl.isRowEmpty");
		for (Cell cell : row) {
			if (!formatter.formatCellValue(cell, evaluator).trim().isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private String readCell(Row row, Map<String, Integer> headers, DataFormatter formatter, FormulaEvaluator evaluator,
			String headerName) {
		log.debug("Init method PlayerTeamServiceImpl.readCell");
		Integer index = headers.get(normalizeHeader(headerName));
		if (index == null) {
			return null;
		}

		Cell cell = row.getCell(index);
		if (cell == null) {
			return null;
		}

		if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
			return ISO_DATE_FORMATTER.format(cell.getLocalDateTimeCellValue().toLocalDate());
		}

		return normalizeText(formatter.formatCellValue(cell, evaluator));
	}

	private String normalizeBirthdate(String value) {
		log.debug("Init method PlayerTeamServiceImpl.normalizeBirthdate");
		String normalized = normalizeText(value);
		if (normalized == null) {
			return null;
		}

		try {
			return INPUT_DATE_FORMATTER.format(LocalDate.parse(normalized, INPUT_DATE_FORMATTER));
		} catch (DateTimeParseException e) {
			try {
				return INPUT_DATE_FORMATTER.format(LocalDate.parse(normalized, ISO_DATE_FORMATTER));
			} catch (DateTimeParseException ignored) {
				return null;
			}
		}
	}

	private Date parseDate(String birthdate) {
		log.debug("Init method PlayerTeamServiceImpl.parseDate");
		if (birthdate == null) {
			return null;
		}
		LocalDate localDate = LocalDate.parse(birthdate, INPUT_DATE_FORMATTER);
		return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	private String mergeSurnames(String firstSurname, String secondSurname) {
		log.debug("Init method PlayerTeamServiceImpl.mergeSurnames");
		StringBuilder builder = new StringBuilder();
		if (firstSurname != null) {
			builder.append(firstSurname);
		}
		if (secondSurname != null) {
			if (builder.length() > 0) {
				builder.append(' ');
			}
			builder.append(secondSurname);
		}
		return normalizeText(builder.toString());
	}

	private String normalizeHeader(String value) {
		log.debug("Init method PlayerTeamServiceImpl.normalizeHeader");
		return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT)
				.replaceAll("\\s+", " ")
				.trim();
	}

	private String normalizeText(String value) {
		log.debug("Init method PlayerTeamServiceImpl.normalizeText");
		if (value == null) {
			return null;
		}
		String normalized = value.trim().replaceAll("\\s+", " ");
		return normalized.isEmpty() ? null : normalized;
	}

	private void saveLogFile(byte[] content) throws IOException {
		log.debug("Init method PlayerTeamServiceImpl.saveLogFile");
		Path downloadsPath = Paths.get(System.getProperty("user.home"), "Downloads");
		Files.createDirectories(downloadsPath);
		Files.write(downloadsPath.resolve("log_registros.xlsx"), content);
	}

	private record ImportedRow(String club, String dorsal, String name, String surname, String nickname, String country,
			String birthdate, String position) {
	}

	private record PlayerResolution(Long id, boolean inserted) {
		String message() {
			return inserted ? "Inserted with ID " + id : "Existing with ID " + id;
		}
	}

	private record TeamResolution(Long id, boolean inserted) {
		String message() {
			return inserted ? "Inserted with ID " + id : "Existing with ID " + id;
		}
	}

	private record SquadResolution(Long id, boolean inserted) {
		String message() {
			return inserted ? "Inserted with ID " + id : "Existing with ID " + id;
		}
	}

	private record PlayerLookupRequest(String name, String surname, String birthdate) {
	}

	private record TeamLookupRequest(String name, String country) {
	}

	private static class PlayerPayload {
		private Long id;
		private String name;
		private String surname;
		private String nickname;
		private String position;
		private String country;
		private Date birthdate;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getSurname() {
			return surname;
		}

		public void setSurname(String surname) {
			this.surname = surname;
		}

		public String getNickname() {
			return nickname;
		}

		public void setNickname(String nickname) {
			this.nickname = nickname;
		}

		public String getPosition() {
			return position;
		}

		public void setPosition(String position) {
			this.position = position;
		}

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}

		public Date getBirthdate() {
			return birthdate;
		}

		public void setBirthdate(Date birthdate) {
			this.birthdate = birthdate;
		}
	}

	private static class TeamPayload {
		private Long id;
		private String name;
		private String country;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}
	}
}
