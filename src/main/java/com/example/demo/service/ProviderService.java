package com.example.demo.service;

import com.example.demo.model.Provider;
import com.example.demo.repository.ProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProviderService {

    @Autowired
    private ProviderRepository providerRepository;

    public Provider getProviderById(Long id) {
        if (providerRepository.findById(id).isPresent())
            return providerRepository.findById(id).get();
        else
            throw new RuntimeException("Provider not found");
    }

    public void deleteProviderById(Long id) {
        providerRepository.deleteById(id);
    }

    public void updateProvider(Provider provider) {
        Provider existingProvider = getProviderById(provider.getId_user());

        if (provider.getName() != null) {
            existingProvider.setName(provider.getName());
        }
        if (provider.getSalary() != null) {
            existingProvider.setSalary(provider.getSalary());
        }
        if (provider.getPhone() != null) {
            existingProvider.setPhone(provider.getPhone());
        }
        if (provider.getAge() != null) {
            existingProvider.setAge(provider.getAge());
        }
        if (provider.getEmail() != null) {
            existingProvider.setEmail(provider.getEmail());
        }
        if (provider.getPassword() != null) {
            existingProvider.setPassword(provider.getPassword());
        }

        if (provider.getMatricule() != null) {
            existingProvider.setMatricule(provider.getMatricule());
        }
        if (provider.getService() != null) {
            existingProvider.setService(provider.getService());
        }
        if (provider.getCompany() != null) {
            existingProvider.setCompany(provider.getCompany());
        }

        providerRepository.saveAndFlush(existingProvider);
    }

    public boolean providerExist(Long id) {
        return providerRepository.existsById(id);
    }

    public void addProvider(Provider provider) {
        providerRepository.save(provider);
    }

    public List<Provider> getAllProviders() {
        return providerRepository.findAll();
    }
}
