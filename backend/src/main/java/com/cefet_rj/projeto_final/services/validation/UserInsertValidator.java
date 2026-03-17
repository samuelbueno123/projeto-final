package com.cefet_rj.projeto_final.services.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.cefet_rj.projeto_final.controllers.exceptions.FieldMessage;
import com.cefet_rj.projeto_final.dtos.UserInsertDTO;
import com.cefet_rj.projeto_final.entities.User;
import com.cefet_rj.projeto_final.repositories.UserRepository;

public class UserInsertValidator implements ConstraintValidator<UserInsertValid, UserInsertDTO> {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void initialize(UserInsertValid ann) {
    }

    @Override
    public boolean isValid(UserInsertDTO dto, ConstraintValidatorContext context) {

        List<FieldMessage> list = new ArrayList<>();

        User user = userRepository.findByEmail(dto.email()).orElse(null);

        if (user != null) {
            list.add(new FieldMessage("email", "Email já existe"));
        }

        for (FieldMessage e : list) {
            context.disableDefaultConstraintViolation();
            context
                    .buildConstraintViolationWithTemplate(e.message())
                    .addPropertyNode(e.fieldName())
                    .addConstraintViolation();
        }

        return list.isEmpty();
    }
}