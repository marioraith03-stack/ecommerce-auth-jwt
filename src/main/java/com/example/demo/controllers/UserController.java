package com.example.demo.controllers;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

	private static final Logger log = LoggerFactory.getLogger(UserController.class);

	private final UserRepository userRepository;
	private final CartRepository cartRepository;
	private final PasswordEncoder passwordEncoder;

	public UserController(UserRepository userRepository,
						  CartRepository cartRepository,
						  PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.cartRepository = cartRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping("/id/{id}")
	public ResponseEntity<User> findById(@PathVariable Long id) {
		return userRepository.findById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/{username}")
	public ResponseEntity<User> findByUserName(@PathVariable String username) {
		User user = userRepository.findByUsername(username);
		return (user == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(user);
	}

	@PostMapping("/create")
	public ResponseEntity<User> createUser(@RequestBody CreateUserRequest req) {
		// 1) Validierung
		if (req.getPassword() == null || req.getPassword().length() < 7) {
			log.warn("CreateUser failure reason=pwd_too_short username={}", req.getUsername());
			return ResponseEntity.badRequest().build();
		}
		if (!req.getPassword().equals(req.getConfirmPassword())) {
			log.warn("CreateUser failure reason=pwd_mismatch username={}", req.getUsername());
			return ResponseEntity.badRequest().build();
		}

		// 2) User + Cart
		User user = new User();
		user.setUsername(req.getUsername());

		Cart cart = new Cart();
		cartRepository.save(cart);
		user.setCart(cart);

		// 3) Passwort hashen
		user.setPassword(passwordEncoder.encode(req.getPassword()));

		// 4) Speichern
		userRepository.save(user);
		log.info("CreateUser success username={}", user.getUsername());

		// 5) Response (password ist per @JsonIgnore nicht im Body)
		return ResponseEntity.ok(user);
	}
}
