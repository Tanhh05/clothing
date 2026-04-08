package com.clothing.elasticsearch.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "products")
public class ProductDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Keyword)
    private String slug;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Text)
    private String brand;

    @Field(type = FieldType.Keyword)
    private Long categoryId;

    @Field(type = FieldType.Text)
    private String categoryName;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String mainImageUrl;

    @Field(type = FieldType.Long)
    private Long minPrice;

    @Field(type = FieldType.Long)
    private Long maxPrice;

    @Field(type = FieldType.Keyword)
    private String createdAt;
}
