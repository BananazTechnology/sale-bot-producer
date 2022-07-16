package tech.bananaz.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.bananaz.bot.models.SaleEvent;

@Repository
public interface SaleEventRepository extends JpaRepository<SaleEvent, Long> {}
