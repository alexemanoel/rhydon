package com.rhydon.rhydon.service;

import com.rhydon.rhydon.dto.UserCreateRequest;
import com.rhydon.rhydon.dto.UserResponse;
import com.rhydon.rhydon.dto.UserUpdateRequest;
import com.rhydon.rhydon.entity.User;
import com.rhydon.rhydon.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  UserRepository repo;

  UserService service;

  @BeforeEach
  void setUp() {
    service = new UserService(repo); 
  }

  // --- helpers ---
  private static User makeUser(Long id, String name, String email, String hash) {
    User u = new User();
    u.setId(id);
    u.setFullName(name);
    u.setEmail(email);
    u.setPasswordHash(hash);
    u.setRole("USER");
    return u;
  }

  // --- create ---

  @Test
  void shouldCreateUser_whenEmailNotExists_andHashPassword() {
    when(repo.existsByEmail("alex@example.com")).thenReturn(false);
    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    when(repo.save(any(User.class))).thenAnswer(inv -> {
        User u = inv.getArgument(0);
        u.setId(1L); 
        return u;
    });

    UserResponse out = service.create(new UserCreateRequest("Alex", "alex@example.com", "123456"));

    verify(repo).save(captor.capture());
    User saved = captor.getValue();

    assertThat(saved.getFullName()).isEqualTo("Alex");
    assertThat(saved.getEmail()).isEqualTo("alex@example.com");

    var encoder = new BCryptPasswordEncoder();
    assertThat(saved.getPasswordHash())
        .isNotBlank()
        .isNotEqualTo("123456"); 

    assertThat(encoder.matches("123456", saved.getPasswordHash())).isTrue();
    assertThat(encoder.matches("senhaErrada", saved.getPasswordHash())).isFalse();

    assertThat(out.id()).isEqualTo(1L);
    assertThat(out.email()).isEqualTo("alex@example.com");
    assertThat(out.fullName()).isEqualTo("Alex");
    }

    @Test
    void shouldThrowConflict_whenEmailAlreadyInUse_onCreate() {
        when(repo.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() ->
            service.create(new UserCreateRequest("X", "dup@example.com", "abcdef"))
        ).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("E-mail already in use");

        verify(repo, never()).save(any());
    }

    // --- get ---

    @Test
    void shouldGetUserById() {
        when(repo.findById(10L)).thenReturn(Optional.of(
            makeUser(10L, "Ana", "ana@example.com", "$2a$10$abcdefghijklmnopqrstuvxyz012345678901234567890123456")
        ));

        UserResponse out = service.get(10L);

        assertThat(out.id()).isEqualTo(10L);
        assertThat(out.fullName()).isEqualTo("Ana");
        assertThat(out.email()).isEqualTo("ana@example.com");
    }

    @Test
    void shouldThrowNotFound_whenIdDoesNotExist_onGet() {
        when(repo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(999L))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("User not found");
    }

    // --- update ---

    @Test
    void shouldUpdatePartialFields_andRehashPassword_whenProvided() {
        User existing = makeUser(5L, "Bruno", "bruno@old.com",
            "$2a$10$abcdefghijklmnopqrstuvxyz012345678901234567890123456");
        when(repo.findById(5L)).thenReturn(Optional.of(existing));
        when(repo.existsByEmail("bruno@new.com")).thenReturn(false);
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse out = service.update(5L, new UserUpdateRequest(
            "Bruno N.", "bruno@new.com", "senhaNova123"
        ));

        assertThat(out.id()).isEqualTo(5L);
        assertThat(out.fullName()).isEqualTo("Bruno N.");
        assertThat(out.email()).isEqualTo("bruno@new.com");

        
        var encoder = new BCryptPasswordEncoder();
        assertThat(existing.getPasswordHash())
            .isNotBlank()
            .isNotEqualTo("senhaNova123");
        assertThat(encoder.matches("senhaNova123", existing.getPasswordHash())).isTrue();
        assertThat(encoder.matches("outraSenha", existing.getPasswordHash())).isFalse();
    }
    @Test
    void shouldRejectUpdate_whenChangingEmailToExistingOne() {
        User existing = makeUser(6L, "Carla", "carla@a.com",
            "$2a$10$abcdefghijklmnopqrstuvxyz012345678901234567890123456");
        when(repo.findById(6L)).thenReturn(Optional.of(existing));
        when(repo.existsByEmail("dup@a.com")).thenReturn(true);

        assertThatThrownBy(() -> service.update(6L, new UserUpdateRequest(
            null, "dup@a.com", null
        ))).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("E-mail already in use");
    }

    @Test
    void shouldThrowNotFound_onUpdate_whenIdDoesNotExist() {
        when(repo.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(404L, new UserUpdateRequest(
            "Z", null, null
        ))).isInstanceOf(NoSuchElementException.class)
        .hasMessage("User not found");
    }

    @Test
    void updateShouldNotChangePassword_whenPasswordNotProvided() {
        var oldHash = "$2a$10$abcdefghijklmnopqrstuvxyz012345678901234567890123456";
        User existing = makeUser(8L, "Duda", "duda@a.com", oldHash);
        when(repo.findById(8L)).thenReturn(Optional.of(existing));
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse out = service.update(8L, new UserUpdateRequest(
            "Duda M.", null, null 
        ));

        assertThat(out.fullName()).isEqualTo("Duda M.");
        assertThat(existing.getPasswordHash()).isEqualTo(oldHash); 
    }

    // --- delete (idempotente) ---

    @Test
    void deleteShouldBeIdempotent_whenIdDoesNotExist() {
        doThrow(new EmptyResultDataAccessException(1)).when(repo).deleteById(123L);

        assertThatCode(() -> service.delete(123L)).doesNotThrowAnyException();
    }

    @Test
    void deleteShouldCallRepository_whenIdExists() {
        assertThatCode(() -> service.delete(7L)).doesNotThrowAnyException();
        verify(repo).deleteById(7L);
    }
}
