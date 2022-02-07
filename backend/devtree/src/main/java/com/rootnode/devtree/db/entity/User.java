package com.rootnode.devtree.db.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import javax.persistence.*;

/**
 * 유저 모델 정의.
 */
@Table(name = "tb_user")
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_seq")
    private Long userSeq;
    @Column(name = "user_name")
    private String userName;
    @Column(name = "user_email")
    private String userEmail;
    @Column(name = "user_id")
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,name = "user_role")
    private UserRole userRole;
    @Column(name = "user_desc")
    private String userDesc;
    @Column(name = "user_nickname")
    private String userNickname;

    @JsonIgnore
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String user_password;

}