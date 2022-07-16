package tech.bananaz.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.bananaz.bot.models.SaleConfig;

@Repository
public interface SaleConfigRepository extends JpaRepository<SaleConfig, Long> {}
