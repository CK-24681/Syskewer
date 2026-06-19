package com.syskewer.api.model.user;

import com.syskewer.api.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// Perfil de acesso do usuario (Administrador, Garcom)
@Entity
@Table(name = "tb_role")
public class Role extends BaseEntity<Integer> {

    @Column(nullable = false, unique = true, length = 50)
    private String authority;

    public Role() {}

    public Role(String authority) {
        this.authority = authority;
    }

    public String getAuthority() { return authority; }

    public void setAuthority(String authority) { this.authority = authority; }
}
