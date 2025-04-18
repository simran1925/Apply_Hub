package com.community.api.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "vacancy_distribution_type")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VacancyDistributionType
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer vacancyDistributionTypeId;

    @Column(name = "vacancyDistributionTypeName")
    String vacancyDistributionTypeName;
}
