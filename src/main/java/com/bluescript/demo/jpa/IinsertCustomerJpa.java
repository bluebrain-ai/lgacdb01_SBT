package com.bluescript.demo.jpa;

import javax.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import com.bluescript.demo.entity.CustomerEntity;
import com.bluescript.demo.entity.CustomerEntity;

public interface IinsertCustomerJpa extends JpaRepository<CustomerEntity, String> {
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO CUSTOMER ( CUSTOMERNUMBER , FIRSTNAME , LASTNAME , DATEOFBIRTH , HOUSENAME , HOUSENUMBER , POSTCODE , PHONEMOBILE , PHONEHOME , EMAILADDRESS ) VALUES ( :db2CustomernumInt , :caFirstName , :caLastName , :caDob , :caHouseName , :caHouseNum , :caPostcode , :caPhoneMobile , :caPhoneHome , :caEmailAddress )", nativeQuery = true)
    void insertCustomerForDb2CustomernumIntAndCaFirstNameAndCaLastName(
            @Param("db2CustomernumInt") int db2CustomernumInt, @Param("caFirstName") String caFirstName,
            @Param("caLastName") String caLastName, @Param("caDob") String caDob,
            @Param("caHouseName") String caHouseName, @Param("caHouseNum") String caHouseNum,
            @Param("caPostcode") String caPostcode, @Param("caPhoneMobile") String caPhoneMobile,
            @Param("caPhoneHome") String caPhoneHome, @Param("caEmailAddress") String caEmailAddress);
}
