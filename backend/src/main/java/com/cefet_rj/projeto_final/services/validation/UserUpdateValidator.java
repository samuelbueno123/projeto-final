package com.cefet_rj.projeto_final.services.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerMapping;

import com.cefet_rj.projeto_final.controllers.exceptions.FieldMessage;
import com.cefet_rj.projeto_final.dtos.update.UserUpdateDTO;
import com.cefet_rj.projeto_final.entities.User;
import com.cefet_rj.projeto_final.repositories.UserRepository;

public class UserUpdateValidator implements ConstraintValidator<UserUpdateValid, UserUpdateDTO> {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Override
    public void initialize(UserUpdateValid ann) {
    }

    @Override
    public boolean isValid(UserUpdateDTO dto, ConstraintValidatorContext context) {

        @SuppressWarnings("unchecked")
        Map<String, String> uriVars =
                (Map<String, String>) httpServletRequest.getAttribute(
                        HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
                );

        Long userId = Long.parseLong(uriVars.get("id"));

        List<FieldMessage> list = new ArrayList<>();

        User user = userRepository.findByEmail(dto.email()).orElse(null);

        if (user != null && !userId.equals(user.getId())) {
            list.add(new FieldMessage("email", "Email já existe"));
        }

        for (FieldMessage e : list) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(e.message())
                    .addPropertyNode(e.fieldName())
                    .addConstraintViolation();
        }

        return list.isEmpty();
    }
}