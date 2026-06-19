package com.syskewer.api.repository.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syskewer.api.model.user.Role;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByAuthority(String authority);
}
