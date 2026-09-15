package com.cefet_rj.projeto_final.users.services;

import com.cefet_rj.projeto_final.users.dtos.UserDTO;
import com.cefet_rj.projeto_final.users.dtos.insert.UserInsertDTO;
import com.cefet_rj.projeto_final.users.dtos.update.UserUpdateDTO;
import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.RoleRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import com.cefet_rj.projeto_final.shared.exceptions.DatabaseException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

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
		linkAssociations(dto.firstName(), dto.lastName(), dto.email(), entity);

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
			linkAssociations(dto.firstName(), dto.lastName(), dto.email(), entity);
			entity = userRepository.save(entity);
			return new UserDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id " + id + " not found");
		}
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	public void delete(Long id) {
		if (!userRepository.existsById(id)) {
			throw new ResourceNotFoundException("Resource not found");
		}
		try {
			userRepository.deleteById(id);
		}
		catch (DataIntegrityViolationException e) {
			throw new DatabaseException("Integrity violation");
		}
	}

	private void linkAssociations(String firstName, String lastName, String email, User entity) {
		entity.setFirstName(firstName);
		entity.setLastName(lastName);
		entity.setEmail(email);
	}
}
