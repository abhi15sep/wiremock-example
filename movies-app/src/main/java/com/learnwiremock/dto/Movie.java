package com.learnwiremock.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Movie {

    private Long movie_id;

    private String name;

    private String cast;

    private Integer year;

    private LocalDate release_date;
}
