package com.infotech.lang.application.data.repository;


import com.infotech.lang.application.data.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, String> {

    @Query("select t from Teacher t where t.firstName = ?1")
    List<Teacher> find(String stringFilter);
    @Query("select t from Teacher t " +
	        "where lower(t.firstName) like lower(concat('%', :searchTerm, '%')) " +
	        "or lower(t.lastName) like lower(concat('%', :searchTerm, '%'))")
	List<Teacher> search(@Param("searchTerm") String searchTerm);

    List<Teacher> findByEmailIgnoreCase(String email);

    Optional<Teacher> findByStudentCodeIgnoreCase(String studentCode);

    Optional<Teacher> findByUserNameIgnoreCase(String userName);
}
