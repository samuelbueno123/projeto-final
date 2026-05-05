package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.dtos.MembershipDTO;
import com.cefet_rj.projeto_final.dtos.insert.MembershipInsertDTO;
import com.cefet_rj.projeto_final.dtos.update.MembershipUpdateDTO;
import com.cefet_rj.projeto_final.entities.Condominium;
import com.cefet_rj.projeto_final.entities.Membership;
import com.cefet_rj.projeto_final.entities.Role;
import com.cefet_rj.projeto_final.entities.User;
import com.cefet_rj.projeto_final.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.repositories.RoleRepository;
import com.cefet_rj.projeto_final.repositories.UserRepository;
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
public class MembershipService {

	private final MembershipRepository membershipRepository;

	private final CondominiumRepository condominiumRepository;

	private final RoleRepository roleRepository;

	private final UserRepository userRepository;

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

	private void linkAssociations(Long dto, Membership entity, String dto1, Long dto2) {
		Condominium condominium = condominiumRepository.getReferenceById(dto);
		entity.setCondominium(condominium);
		Role role = roleRepository.findByAuthority(dto1);
		entity.setRole(role);
		User user = userRepository.getReferenceById(dto2);
		entity.setUser(user);
	}
}
