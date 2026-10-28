package com.spring.userauth.entity;

import java.io.Serializable;

import com.spring.userauth.utills.BaseEntity;

import jakarta.persistence.Column;

// Legacy DTO only: User.roles owns user_roles as a many-to-many join table.
// Mapping this table a second time as an entity adds mandatory BaseEntity
// columns that Hibernate cannot populate when inserting role memberships.
public class UserRole extends BaseEntity implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

    @Column(name = "user_id")
    private Long userId;

    @Column(name="role_id")
    private Long roleId;
    
    public UserRole(Long userId, Long roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getRoleId() {
		return roleId;
	}

	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}
}
