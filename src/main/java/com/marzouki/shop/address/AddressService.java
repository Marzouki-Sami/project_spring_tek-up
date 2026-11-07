package com.marzouki.shop.address;

import com.marzouki.shop.address.dto.AddressForm;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.NotFoundException;
import com.marzouki.shop.user.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A customer's saved delivery addresses. Everything is scoped to the owner: other people's addresses do not exist. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressService {

    public static final int MAX_ADDRESSES = 10;

    private final AddressRepository addresses;
    private final UserRepository users;

    public List<Address> forUser(Long userId) {
        return addresses.findByUserIdOrderByDefaultAddressDescCreatedAtDescIdDesc(userId);
    }

    public Address get(Long id, Long userId) {
        return addresses.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Address", id));
    }

    public AddressForm formFor(Long id, Long userId) {
        Address a = get(id, userId);
        AddressForm form = new AddressForm();
        form.setLabel(a.getLabel());
        form.setFullName(a.getFullName());
        form.setPhone(a.getPhone());
        form.setAddress(a.getStreet());
        form.setPostalCode(a.getPostalCode());
        form.setCity(a.getCity());
        form.setCountry(a.getCountry());
        form.setLatitude(a.getLatitude());
        form.setLongitude(a.getLongitude());
        form.setDefaultAddress(a.isDefaultAddress());
        return form;
    }

    @Transactional
    public Address create(Long userId, AddressForm form) {
        if (addresses.countByUserId(userId) >= MAX_ADDRESSES) {
            throw new ConflictException("You can save at most " + MAX_ADDRESSES + " addresses. Delete one first.");
        }
        Address address = new Address();
        address.setUser(users.getReferenceById(userId));
        apply(address, form);
        boolean first = addresses.countByUserId(userId) == 0;
        if (form.isDefaultAddress() || first) {
            addresses.clearDefault(userId);
            address.setDefaultAddress(true);
        }
        return addresses.save(address);
    }

    @Transactional
    public Address update(Long id, Long userId, AddressForm form) {
        Address address = get(id, userId);
        apply(address, form);
        if (form.isDefaultAddress() && !address.isDefaultAddress()) {
            addresses.clearDefault(userId);
            address = get(id, userId);
            apply(address, form);
            address.setDefaultAddress(true);
        }
        return address;
    }

    @Transactional
    public void makeDefault(Long id, Long userId) {
        get(id, userId);
        addresses.clearDefault(userId);
        get(id, userId).setDefaultAddress(true);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Address address = get(id, userId);
        boolean wasDefault = address.isDefaultAddress();
        addresses.delete(address);
        if (wasDefault) {
            addresses.flush();
            forUser(userId).stream().findFirst().ifPresent(next -> next.setDefaultAddress(true));
        }
    }

    private void apply(Address address, AddressForm form) {
        address.setLabel(form.getLabel().trim());
        address.setFullName(form.getFullName().trim());
        address.setPhone(form.getPhone() == null || form.getPhone().isBlank() ? null : form.getPhone().trim());
        address.setStreet(form.getAddress().trim());
        address.setPostalCode(form.getPostalCode().trim());
        address.setCity(form.getCity().trim());
        address.setCountry(form.getCountry().trim());
        boolean pinned = form.getLatitude() != null && form.getLongitude() != null;
        address.setLatitude(pinned ? form.getLatitude().setScale(6, RoundingMode.HALF_UP) : null);
        address.setLongitude(pinned ? form.getLongitude().setScale(6, RoundingMode.HALF_UP) : null);
    }
}