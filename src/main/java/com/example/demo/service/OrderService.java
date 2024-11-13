package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    public Order getOrderById(Long id) {
        if (orderRepository.findById(id).isPresent())
            return orderRepository.findById(id).get();
        else
            throw new RuntimeException("Order not found");
    }

    public void deleteOrderById(Long id) {
        orderRepository.deleteById(id);
    }

    public void updateOrder(Order order) {
        Order existingOrder = getOrderById(order.getId_order());

        if (order.getRef() != null) {
            existingOrder.setRef(order.getRef());
        }
        if (order.getPrice() != null) {
            existingOrder.setPrice(order.getPrice());
        }
        if (order.getDate() != null) {
            existingOrder.setDate(order.getDate());
        }

        orderRepository.saveAndFlush(existingOrder);
    }

    public boolean orderExist(Long id) {
        return orderRepository.existsById(id);
    }

    public void addOrder(Order order) {
        orderRepository.save(order);
    }
}
