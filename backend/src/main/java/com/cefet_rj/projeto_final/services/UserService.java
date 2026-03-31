package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.dtos.UserDTO;
import com.cefet_rj.projeto_final.dtos.insert.UserInsertDTO;
import com.cefet_rj.projeto_final.dtos.update.UserUpdateDTO;
import com.cefet_rj.projeto_final.entities.Role;
import com.cefet_rj.projeto_final.entities.User;
import com.cefet_rj.projeto_final.mapper.UserMapper;
import com.cefet_rj.projeto_final.repositories.RoleRepository;
import com.cefet_rj.projeto_final.repositories.UserRepository;
import com.cefet_rj.projeto_final.services.exceptions.ResourceNotFoundException;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserMapper userMapper;

	private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

	private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public Page<UserDTO> findAllPaged(Pageable pageable) {
        Page<User> list = userRepository.findAll(pageable);
        return list.map(UserDTO::new);
    }

    @Transactional(readOnly = true)
    public UserDTO findById(Long id) {
        Optional<User> obj = userRepository.findById(id);
        User entity = obj.orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
        return new UserDTO(entity);
    }

	@Transactional
	public UserDTO insert(UserInsertDTO dto) {
		User entity = new User();
		userMapper.copyInsertDtoToEntity(dto, entity);

		entity.getRoles().clear();
		Role role = roleRepository.findByAuthority("ROLE_RESIDENT");
		entity.getRoles().add(role);
		entity.setPassword(passwordEncoder.encode(dto.password()));

		entity = userRepository.save(entity);

		return new UserDTO(entity);
	}

	@Transactional
	public UserDTO update(Long id, UserUpdateDTO dto) {
		try {
			User entity = userRepository.getReferenceById(id);
			userMapper.copyUpdateDtoToEntity(dto, entity);
			entity = userRepository.save(entity);
			return new UserDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id " + id + " not found");
		}
	}
}
