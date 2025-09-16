package com.lostfound.capstonebackend.domain.lostitem;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LostItemRepository extends JpaRepository<LostItem, Long> {
}
