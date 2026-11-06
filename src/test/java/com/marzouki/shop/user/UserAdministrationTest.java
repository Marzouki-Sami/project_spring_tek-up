package com.marzouki.shop.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.support.TestPasswords;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserAdministrationTest {

    @Autowired UserService users;

    private User admin() {
        return users.create("Ada", "Admin", "ada@example.com", TestPasswords.strong(), Role.ADMIN);
    }

    @Test
    void anAdminCanPromoteAndDisableOtherUsers() {
        User admin = admin();
        User customer = users.create("Cy", "Customer", "cy@example.com", TestPasswords.strong(), Role.CUSTOMER);

        assertThat(users.changeRole(admin.getId(), customer.getId(), Role.MANAGER).getRole()).isEqualTo(Role.MANAGER);
        assertThat(users.setEnabled(admin.getId(), customer.getId(), false).isEnabled()).isFalse();
    }

    @Test
    void anAdminCannotChangeOrDisableTheirOwnAccount() {
        User admin = admin();
        assertThatThrownBy(() -> users.changeRole(admin.getId(), admin.getId(), Role.CUSTOMER))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> users.setEnabled(admin.getId(), admin.getId(), false))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void changingAPasswordNeedsTheCurrentOne() {
        User admin = admin();
        var form = new com.marzouki.shop.user.dto.PasswordChangeForm();
        form.setCurrentPassword(TestPasswords.strong());
        form.setNewPassword(TestPasswords.strong());
        form.setConfirmPassword(form.getNewPassword());
        assertThatThrownBy(() -> users.changePassword(admin.getId(), form)).isInstanceOf(ConflictException.class);
    }
}