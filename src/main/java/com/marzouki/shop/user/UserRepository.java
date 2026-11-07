package com.marzouki.shop.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<User> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByOrderByLastNameAscFirstNameAsc();

    long countByRole(Role role);

    /** Accepts an authenticator step only if it is newer than the last one used, so a code works once. */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("update User u set u.totpLastStep = :step where u.id = :id and (u.totpLastStep is null or u.totpLastStep < :step)")
    int claimTotpStep(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("step") long step);

    java.util.List<User> findByRoleInAndEnabledTrue(java.util.Collection<Role> roles);

    java.util.List<User> findByRoleInAndEnabledTrueAndLowStockAlertsTrue(java.util.Collection<Role> roles);
}
