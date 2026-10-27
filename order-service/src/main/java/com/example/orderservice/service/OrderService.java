package com.example.orderservice.service;

import com.example.orderservice.dto.UserDTO;
import com.example.orderservice.entity.Order;
import com.example.orderservice.exception.UserNotFoundException;
import com.example.orderservice.exception.UserServiceUnavailableException;
import com.example.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    // user.service.url comes from application.properties,
    // which reads the USER_SERVICE_URL environment variable
    public OrderService(OrderRepository orderRepository,
                        RestTemplate restTemplate,
                        @Value("${user.service.url}") String userServiceUrl) {
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order createOrder(Order order) {
        validateUserExists(order.getUserId());
        return orderRepository.save(order);
    }

    // Synchronous REST call: GET {USER_SERVICE_URL}/api/users/{id}
    private void validateUserExists(Long userId) {
        String url = userServiceUrl + "/api/users/" + userId;
        try {
            restTemplate.getForObject(url, UserDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new UserNotFoundException(userId);
        } catch (ResourceAccessException e) {
            throw new UserServiceUnavailableException(userServiceUrl);
        }
    }
}
