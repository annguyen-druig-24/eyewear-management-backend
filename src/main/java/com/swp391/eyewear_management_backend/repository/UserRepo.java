package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@RepositoryRestResource(path = "show-all-users")
public interface UserRepo extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByIdNumber(String idNumber);

    boolean existsByIdNumberAndUserIdNot(String idNumber, Long userId);

    boolean existsByEmailAndUserIdNot(String email, Long userId);

    public User findByName(String name);

    Optional<User> findByUsername(String username);

    @Query(value = """
            SELECT *
            FROM [User]
            WHERE Username COLLATE Latin1_General_CS_AS =
                  CAST(:username AS NVARCHAR(50)) COLLATE Latin1_General_CS_AS
            """, nativeQuery = true)
    Optional<User> findByUsernameCaseSensitive(@Param("username") String username);
}
