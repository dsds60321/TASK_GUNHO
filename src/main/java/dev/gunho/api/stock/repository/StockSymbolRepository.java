package dev.gunho.api.stock.repository;

import dev.gunho.api.stock.entity.StockSymbol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockSymbolRepository extends JpaRepository<StockSymbol, String> {


}
