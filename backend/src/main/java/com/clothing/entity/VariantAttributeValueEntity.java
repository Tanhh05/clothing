package com.clothing.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "variant_attribute_values")
@IdClass(VariantAttributeValueId.class)
public class VariantAttributeValueEntity {

    @Id
    @Column(name = "variant_id")
    private Long variantId;

    @Id
    @Column(name = "attribute_value_id")
    private Long attributeValueId;
}
