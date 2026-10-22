package com.cefet_rj.projeto_final.condominiums.services;

import com.cefet_rj.projeto_final.condominiums.dtos.MembershipDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.MembershipInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.MembershipUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Membership;
import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.users.repositories.RoleRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.shared.exceptions.DatabaseException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipService {

	private final MembershipRepository membershipRepository;

	private final CondominiumRepository condominiumRepository;

	private final RoleRepository roleRepository;

	private final UserRepository userRepository;
	private final AuthenticatedUserService authenticatedUserService;

	@Transactional(readOnly = true)
	public List<MembershipDTO> findMine() {
		Long userId = authenticatedUserService.getAuthenticatedUser().getId();
		return membershipRepository.findAllByUserId(userId).stream()
				.map(MembershipDTO::new)
				.toList();
	}

	@Transactional(readOnly = true)
	public Page<MembershipDTO> findAllPaged(Pageable pageable) {
		Page<Membership> list = membershipRepository.findAll(pageable);
		return list.map(MembershipDTO::new);
	}

	@Transactional(readOnly = true)
	public MembershipDTO findById(Long id) {
		Optional <Membership> obj = membershipRepository.findById(id);
		Membership entity = obj.orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
		return new MembershipDTO(entity);
	}

	@Transactional
	public MembershipDTO insert(MembershipInsertDTO dto) {
		Membership entity = new Membership();
		linkAssociations(dto.condominiumId(), entity, dto.authority(), dto.userId());
		entity = membershipRepository.save(entity);
		return new MembershipDTO(entity);
	}

	@Transactional
	public MembershipDTO update(Long id, MembershipUpdateDTO dto){
		try {
			Membership entity = membershipRepository.getReferenceById(id);
			linkAssociations(dto.condominiumId(), entity, dto.authority(), dto.userId());
			entity = membershipRepository.save(entity);
			return new MembershipDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id " + id + " not found");
		}
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	public void delete(Long id) {
		if (!membershipRepository.existsById(id)) {
			throw new ResourceNotFoundException("Resource not found");
		}
		try {
			membershipRepository.deleteById(id);
		}
		catch (DataIntegrityViolationException e) {
			throw new DatabaseException("Integrity violation");
		}
	}

	private void linkAssociations(
			Long condominiumId,
			Membership entity,
			String authority,
			Long userId
	) {
		Condominium condominium = condominiumRepository.findById(condominiumId)
				.orElseThrow(() -> new ResourceNotFoundException("Condominium not found: " + condominiumId));

		entity.setCondominium(condominium);

		Role role = roleRepository.findByAuthority(authority);
		entity.setRole(role);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

		entity.setUser(user);
	}
}
