package com.cefet_rj.projeto_final.users.repositories;

import com.cefet_rj.projeto_final.users.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.googleSubject = :googleSubject")
    Optional<User> findByGoogleSubject(@Param("googleSubject") String googleSubject);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"roles"})
    @org.springframework.lang.NonNull
    Page<User> findAll(@org.springframework.lang.NonNull Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"roles"})
    @Query("select u from User u where "
            + "lower(concat(concat(u.firstName, ' '), u.lastName)) like lower(concat('%', :query, '%')) "
            + "or lower(u.firstName) like lower(concat('%', :query, '%')) "
            + "or lower(u.lastName) like lower(concat('%', :query, '%')) "
            + "or lower(u.email) like lower(concat('%', :query, '%'))")
    Page<User> searchByName(@Param("query") String query, Pageable pageable);
}
