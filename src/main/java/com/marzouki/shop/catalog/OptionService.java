package com.marzouki.shop.catalog;

import com.marzouki.shop.catalog.dto.OptionTypeForm;
import com.marzouki.shop.catalog.dto.OptionValueForm;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Option types (Size, Color) and their values. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OptionService {

    private final OptionTypeRepository optionTypes;
    private final OptionValueRepository optionValues;
    private final ProductTypeRepository productTypes;

    public List<OptionType> findAll() {
        return optionTypes.findAllByOrderByNameAsc();
    }

    @Transactional
    public OptionType createType(OptionTypeForm form) {
        String name = form.getName().trim();
        if (optionTypes.existsByNameIgnoreCase(name)) {
            throw new ConflictException("An option called '" + name + "' already exists");
        }
        OptionType type = new OptionType();
        type.setName(name);
        type.setDisplay(form.getDisplay());
        return optionTypes.save(type);
    }

    @Transactional
    public void deleteType(Long id) {
        OptionType type = optionTypes.findById(id).orElseThrow(() -> new NotFoundException("Option", id));
        if (productTypes.usesOptionType(id)) {
            throw new ConflictException("'" + type.getName() + "' is used by a product type. Remove it there first.");
        }
        if (optionValues.isTypeUsed(id)) {
            throw new ConflictException("'" + type.getName() + "' is used by product variants and cannot be deleted");
        }
        optionTypes.delete(type);
    }

    @Transactional
    public OptionValue addValue(Long typeId, OptionValueForm form) {
        OptionType type = optionTypes.findById(typeId).orElseThrow(() -> new NotFoundException("Option", typeId));
        String label = form.getLabel().trim();
        if (type.getValues().stream().anyMatch(v -> v.getLabel().equalsIgnoreCase(label))) {
            throw new ConflictException("'" + label + "' already exists for " + type.getName());
        }
        String hex = form.getColorHex() == null || form.getColorHex().isBlank() ? null : form.getColorHex().toLowerCase();
        if (type.getDisplay() == OptionType.Display.COLOR && hex == null) {
            throw new ConflictException("Pick a colour for this value");
        }
        return type.addValue(label, type.getDisplay() == OptionType.Display.COLOR ? hex : null);
    }

    @Transactional
    public void deleteValue(Long valueId) {
        OptionValue value = optionValues.findById(valueId).orElseThrow(() -> new NotFoundException("Value", valueId));
        if (optionValues.isUsed(valueId)) {
            throw new ConflictException("'" + value.getLabel() + "' is used by product variants and cannot be deleted");
        }
        value.getOptionType().getValues().remove(value);
    }
}