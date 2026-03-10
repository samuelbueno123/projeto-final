//package com.cefet_rj.projeto_final.config.customgrant;
//
//
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Set;
//
//import jakarta.servlet.http.HttpServletRequest;
//
//import org.springframework.lang.Nullable;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.oauth2.core.AuthorizationGrantType;
//import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
//import org.springframework.security.oauth2.core.OAuth2Error;
//import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
//import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
//import org.springframework.security.web.authentication.AuthenticationConverter;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//import org.springframework.util.StringUtils;
//
//public class CustomPasswordAuthenticationConverter implements AuthenticationConverter {
//
//    @Override
//    @Nullable
//    public Authentication convert(HttpServletRequest request) {
//
//        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
//        if (!AuthorizationGrantType.PASSWORD.getValue().equals(grantType)) {
//            return null;
//        }
//
//        MultiValueMap<String, String> parameters = getParameters(request);
//
//        String scope = parameters.getFirst(OAuth2ParameterNames.SCOPE);
//        if (StringUtils.hasText(scope) &&
//                parameters.get(OAuth2ParameterNames.SCOPE).size() != 1) {
//            throw invalidRequest("scope");
//        }
//
//        String username = parameters.getFirst(OAuth2ParameterNames.USERNAME);
//        if (!StringUtils.hasText(username) ||
//                parameters.get(OAuth2ParameterNames.USERNAME).size() != 1) {
//            throw invalidRequest("username");
//        }
//
//        String password = parameters.getFirst(OAuth2ParameterNames.PASSWORD);
//        if (!StringUtils.hasText(password) ||
//                parameters.get(OAuth2ParameterNames.PASSWORD).size() != 1) {
//            throw invalidRequest("password");
//        }
//
//        Set<String> requestedScopes = null;
//        if (StringUtils.hasText(scope)) {
//            requestedScopes = new HashSet<>(
//                    Arrays.asList(StringUtils.delimitedListToStringArray(scope, " ")));
//        }
//
//        Map<String, Object> additionalParameters = new HashMap<>();
//        parameters.forEach((key, values) -> {
//            if (!OAuth2ParameterNames.GRANT_TYPE.equals(key) &&
//                    !OAuth2ParameterNames.SCOPE.equals(key)) {
//                additionalParameters.put(key, values.get(0));
//            }
//        });
//
//        Authentication clientPrincipal =
//                SecurityContextHolder.getContext().getAuthentication();
//
//        return new CustomPasswordAuthenticationToken(
//                clientPrincipal,
//                requestedScopes,
//                additionalParameters
//        );
//    }
//
//    private static OAuth2AuthenticationException invalidRequest(String parameterName) {
//        OAuth2Error error = new OAuth2Error(
//                OAuth2ErrorCodes.INVALID_REQUEST,
//                "Invalid or missing parameter: " + parameterName,
//                null
//        );
//        return new OAuth2AuthenticationException(error);
//    }
//
//    private static MultiValueMap<String, String> getParameters(HttpServletRequest request) {
//
//        Map<String, String[]> parameterMap = request.getParameterMap();
//        MultiValueMap<String, String> parameters =
//                new LinkedMultiValueMap<>(parameterMap.size());
//
//        parameterMap.forEach((key, values) -> {
//            for (String value : values) {
//                parameters.add(key, value);
//            }
//        });
//
//        return parameters;
//    }
//}