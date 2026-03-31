package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.dtos.MembershipDTO;
import com.cefet_rj.projeto_final.entities.Membership;
import com.cefet_rj.projeto_final.mapper.MembershipMapper;
import com.cefet_rj.projeto_final.repositories.MembershipRepository;
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

	private final MembershipRepository condominiumUserRepository;

	private final MembershipMapper condominiumUserMapper;

	@Transactional(readOnly = true)
	public Page<MembershipDTO> findAllPaged(Pageable pageable) {
		Page<Membership> list = condominiumUserRepository.findAll(pageable);
		return list.map(MembershipDTO::new);
	}

	@Transactional(readOnly = true)
	public MembershipDTO findById(Long id) {
		Optional <Membership> obj = condominiumUserRepository.findById(id);
		Membership entity = obj.orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
		return new MembershipDTO(entity);
	}

	@Transactional
	public MembershipDTO insert(MembershipDTO dto) {
		Membership entity = new Membership();
		condominiumUserMapper.copyDtoToEntity(dto, entity);
		entity = condominiumUserRepository.save(entity);
		return new MembershipDTO(entity);
	}

	@Transactional
	public MembershipDTO update(Long id, MembershipDTO dto){
		try {
			Membership entity = condominiumUserRepository.getReferenceById(id);
			condominiumUserMapper.copyDtoToEntity(dto, entity);
			entity = condominiumUserRepository.save(entity);
			return new MembershipDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id " + id + " not found");
		}
	}
}
