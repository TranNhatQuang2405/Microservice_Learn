package learn.microservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import learn.microservice.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
