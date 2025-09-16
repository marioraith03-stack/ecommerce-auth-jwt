package com.example.demo.controllers;

import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.repositories.ItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/item")
public class ItemController {

	private final ItemRepository itemRepository;

	public ItemController(ItemRepository itemRepository) {
		this.itemRepository = itemRepository;
	}

	@GetMapping
	public List<Item> getItems() {
		return itemRepository.findAll();
	}

	@GetMapping("/{id}")
	public ResponseEntity<Item> getItemById(@PathVariable Long id) {
		return itemRepository.findById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}
	@GetMapping("/id/{id}")
	public ResponseEntity<Item> getItemByIdCompat(@PathVariable Long id) {
		return getItemById(id);
	}

	@GetMapping("/name/{name}")
	public List<Item> getItemsByName(@PathVariable String name) {
		return itemRepository.findByName(name);
	}
}
