package com.clothing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "categories")
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String name;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(name = "name_vi", length = 100)
    private String nameVi;

    @Column(name = "name_my", length = 100)
    private String nameMy;

    @Column(length = 255, unique = true)
    private String slug;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 150)
    private String subtitle;

    @Column(length = 500)
    private String externalLink;

    @Column(length = 50)
    private String pageType;

    @Column(length = 2000)
    private String shortContent;

    @Column
    private Integer displayOrder;

    @Column
    private Boolean showInMenu;

    @Column(length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    private CategoryEntity parent;
}
