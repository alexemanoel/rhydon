package com.rhydon.rhydon.service;

import com.rhydon.rhydon.dto.PageResponse;
import com.rhydon.rhydon.dto.UserCreateRequest;
import com.rhydon.rhydon.dto.UserResponse;
import com.rhydon.rhydon.dto.UserUpdateRequest;
import com.rhydon.rhydon.entity.User;
import com.rhydon.rhydon.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.dao.EmptyResultDataAccessException;
import java.util.NoSuchElementException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public UserResponse create(UserCreateRequest req) {
        if (repo.existsByEmail(req.email())) {
            throw new IllegalArgumentException("E-mail already in use");
        }
        var u = new User();
        u.setFullName(req.fullName());
        u.setEmail(req.email());
        u.setPasswordHash(encoder.encode(req.password()));
        var saved = repo.save(u);
        return new UserResponse(saved.getId(), saved.getFullName(), saved.getEmail(), saved.getRole());
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        var u = repo.findById(id).orElseThrow(() -> new NoSuchElementException("User not found"));
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole());
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest req) {
        User u = repo.findById(id).orElseThrow(() -> new NoSuchElementException("User not found"));

        if (req.fullName() != null) {
            String name = req.fullName().trim();
            if (name.isEmpty()) throw new IllegalArgumentException("Full name cannot be blank");
            u.setFullName(name);
        }
        if (req.email() != null && !req.email().equalsIgnoreCase(u.getEmail())) {
            if (repo.existsByEmail(req.email())) {
            throw new IllegalArgumentException("E-mail already in use");
            }
            u.setEmail(req.email());
        }
        if (req.password() != null) {
            u.setPasswordHash(encoder.encode(req.password()));
        }

        User saved = repo.save(u);
        return new UserResponse(saved.getId(), saved.getFullName(), saved.getEmail(), saved.getRole());

    }
    @Transactional 
        public void delete(Long id) {
        try {
            repo.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
           
        }
    }

    public PageResponse<UserResponse> list(int page, int size, String sort, String dir) {
        Sort.Direction direction;
        
        try {
            direction = Sort.Direction.fromString(dir);
        } catch (IllegalArgumentException ex) {
            direction = Sort.Direction.DESC;
        }

        String sortBy = (sort == null || sort.isBlank()) ? "createdAt" : sort;

        var pageReq = PageRequest.of(page, size, Sort.by(direction, sortBy));
        var pageData = repo.findAll(pageReq);

        var items = pageData.getContent().stream()
            .map(u -> new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole()))
            .collect(Collectors.toList());

        return new PageResponse<>(
            items,
            pageData.getNumber(),
            pageData.getSize(),
            pageData.getTotalElements(),
            pageData.getTotalPages(),
            pageData.isFirst(),
            pageData.isLast(),
            sortBy,
            direction.name().toLowerCase()
        );
    }
}
