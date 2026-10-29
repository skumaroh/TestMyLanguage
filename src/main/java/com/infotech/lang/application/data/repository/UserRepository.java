package com.infotech.lang.application.data.repository;


import com.infotech.lang.application.data.entity.Teacher;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.entity.UserRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"teacher", "status"})
    @Query("select u from User u " +
	        "where lower(u.firstName) like lower(concat('%', :searchTerm, '%')) " +
	        "or lower(u.lastName) like lower(concat('%', :searchTerm, '%')) " +
            "or lower(u.email) like lower(concat('%', :searchTerm, '%')) " +
            "or lower(u.userName) like lower(concat('%', :searchTerm, '%')) " +
            "order by lower(u.lastName), lower(u.firstName)")
	 List<User> search(@Param("searchTerm") String searchTerm);

    @EntityGraph(attributePaths = {"teacher", "status"})
    @Query("select u from User u " +
            "order by lower(u.lastName), lower(u.firstName)")
    List<User> findAllForManagement();

    @Query("select u from User u " +
	        "where lower(u.firstName) like lower(concat('%', :searchTerm, '%')) " +
	        "or lower(u.lastName) like lower(concat('%', :searchTerm, '%'))")
    List<User> searchByName(@Param("searchTerm") String searchTerm);
	 
    @EntityGraph(attributePaths = {"teacher"})
    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByTeacher(Teacher teacher);

    @Query("SELECT u FROM User u WHERE u.teacher IS NOT NULL AND LOWER(u.teacher.userName) = LOWER(:teacherUserName)")
    List<User> findStudentsByTeacherUserName(@Param("teacherUserName") String teacherUserName);

    // All students (any teacher), for the admin view of the Feature 6 student level
    // promotion UI — admin can set level for any student, not just one teacher's roster.
    @EntityGraph(attributePaths = {"teacher"})
    @Query("select u from User u where u.role = :role order by lower(u.lastName), lower(u.firstName)")
    List<User> findByRoleOrderByName(@Param("role") UserRole role);

    boolean existsByEmailIgnoreCase(String email);
    
    // Find user by userName for login
    Optional<User> findByUserName(String userName);
    
    // Check if userName already exists (for registration validation)
    boolean existsByUserName(String userName);

    List<User> findByEmailIgnoreCaseIn(java.util.Collection<String> emails);
}
