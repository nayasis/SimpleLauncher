package com.nayasis.simplelauncher.service;

import com.nayasis.simplelauncher.vo.Country;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class CountryService {

    public Set<Country> getAllCountries() {
        Set<Country> result = new HashSet<>();
        result.add(new Country("AU", "Australia"));
        result.add(new Country("BR", "Brazil"));
        result.add(new Country("BE", "Belgium"));
        return result;
    }

}
