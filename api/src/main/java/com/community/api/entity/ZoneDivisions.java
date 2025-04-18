package com.community.api.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name ="zone_divisions")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ZoneDivisions {
    @Id
    @GeneratedValue
    Integer zoneDivisionId;

    @ManyToOne
    @JoinColumn(name = "zone_id",nullable = false)
    private Zone zone;

    @ManyToOne
    @JoinColumn(name = "division_id",nullable = false)
    private StateCode divisions;
}
