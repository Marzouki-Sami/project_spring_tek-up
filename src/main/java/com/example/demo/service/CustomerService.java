package com.example.demo.service;

import com.example.demo.model.Customer;
import com.example.demo.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public Customer getCustomerById(Long id) {
        if (customerRepository.findById(id).isPresent())
            return customerRepository.findById(id).get();
        else
            throw new RuntimeException("Customer not found");
    }

    public void deleteCustomerById(Long id) {
        customerRepository.deleteById(id);
    }

    public void updateCustomer(Customer customer) {
        Customer existingCustomer = getCustomerById(customer.getId_user());

        if (customer.getName() != null) {
            existingCustomer.setName(customer.getName());
        }
        if (customer.getSalary() != null) {
            existingCustomer.setSalary(customer.getSalary());
        }
        if (customer.getPhone() != null) {
            existingCustomer.setPhone(customer.getPhone());
        }
        if (customer.getAge() != null) {
            existingCustomer.setAge(customer.getAge());
        }
        if (customer.getEmail() != null) {
            existingCustomer.setEmail(customer.getEmail());
        }
        if (customer.getPassword() != null) {
            existingCustomer.setPassword(customer.getPassword());
        }
        if (customer.getAddress() != null) {
            existingCustomer.setAddress(customer.getAddress());
        }
        if (customer.getCity() != null) {
            existingCustomer.setCity(customer.getCity());
        }

        customerRepository.saveAndFlush(existingCustomer);
    }

    public boolean customerExist(Long id) {
        return customerRepository.existsById(id);
    }

    public void addCustomer(Customer customer) {
        customerRepository.save(customer);
    }
}
