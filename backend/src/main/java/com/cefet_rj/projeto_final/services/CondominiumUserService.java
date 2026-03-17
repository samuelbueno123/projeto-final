package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.dtos.CondominiumDTO;
import com.cefet_rj.projeto_final.dtos.CondominiumUserDTO;
import com.cefet_rj.projeto_final.dtos.UserDTO;
import com.cefet_rj.projeto_final.entities.Condominium;
import com.cefet_rj.projeto_final.entities.CondominiumUser;
import com.cefet_rj.projeto_final.entities.Role;
import com.cefet_rj.projeto_final.entities.User;
import com.cefet_rj.projeto_final.mapper.UserMapper;
import com.cefet_rj.projeto_final.repositories.CondominiumUserRepository;
import com.cefet_rj.projeto_final.repositories.RoleRepository;
import com.cefet_rj.projeto_final.services.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CondominiumUserService {

	private final CondominiumUserRepository condominiumUserRepository;

	private final UserService userService;
	private final UserMapper userMapper;
	private final CondominiumService condominiumService;
	private final RoleRepository roleRepository;

	@Transactional(readOnly = true)
	public Page<CondominiumUserDTO> findAllPaged(Pageable pageable) {
		Page<CondominiumUser> list = condominiumUserRepository.findAll(pageable);
		return list.map(CondominiumUserDTO::new);
	}

	@Transactional(readOnly = true)
	public CondominiumUserDTO findById(Long id) {
		Optional <CondominiumUser> obj = condominiumUserRepository.findById(id);
		CondominiumUser entity = obj.orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
		return new CondominiumUserDTO(entity);
	}

	@Transactional
	public CondominiumUserDTO update(Long id, CondominiumUserDTO dto){
		try {
			CondominiumUser entity = condominiumUserRepository.getReferenceById(id);
			copyDtoToEntity(dto, entity);
			entity = condominiumUserRepository.save(entity);
			return new CondominiumUserDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id " + id + " not found");
		}
	}

	public void copyDtoToEntity(CondominiumUserDTO dto, CondominiumUser entity) {
		User userEntity = new User();
		Condominium condominiumEntity = new Condominium();

		UserDTO user = userService.findById(dto.userId());
		CondominiumDTO condominium = condominiumService.findById(dto.condominiumId());
		Role role = roleRepository.findByAuthority(dto.authority());

		userMapper.copyDtoToEntity(user, userEntity);
		condominiumService.copyDtoToEntity(condominium, condominiumEntity);

		entity.setUser(userEntity);
		entity.setCondominium(condominiumEntity);
		entity.setRole(role);
	}

}
