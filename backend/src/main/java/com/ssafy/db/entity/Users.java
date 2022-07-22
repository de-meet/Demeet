package com.ssafy.db.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int uid;
    @Column(length = 30, nullable = false, unique = true)
    String email;
    @Column(length = 40, nullable = false)
    String password;
    @Column(length = 15, nullable = false)
    String nickname;
    @Column(nullable = false)
    Timestamp regDate;


}
