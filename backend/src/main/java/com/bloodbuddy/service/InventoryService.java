package com.bloodbuddy.service;

import com.bloodbuddy.dto.InventoryBoard;
import com.bloodbuddy.model.BloodGroup;
import com.bloodbuddy.model.BloodInventory;
import com.bloodbuddy.model.Hospital;
import com.bloodbuddy.repository.BloodInventoryRepository;
import com.bloodbuddy.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Blood-bank stock, always scoped to ONE hospital (BR-5): the adjust-stock
 * modal and a fulfilment may touch only the hospital the caller is entitled to,
 * and the hospital id is a parameter the controller takes from the session —
 * never from the request body.
 *
 * <p>Board reads always answer all eight groups, filling groups with no row in
 * at 0, because {@code hospital-dashboard.html} iterates the whole set and an
 * absent key would render as {@code undefined} units.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final BloodInventoryRepository inventory;
    private final HospitalRepository hospitals;

    /** The hospital's whole board (Appendix B {@code GET /api/hospitals/{id}/inventory}). */
    @Transactional(readOnly = true)
    public InventoryBoard board(Long hospitalId) {
        Hospital h = require(hospitalId);
        Map<String, Integer> units = new LinkedHashMap<>();
        for (BloodGroup g : BloodGroup.values()) {
            units.put(g.getLabel(), 0);
        }
        inventory.findByHospital_HospitalIdOrderByBloodGroupAsc(hospitalId)
                .forEach(row -> units.put(row.getBloodGroup().getLabel(), row.getUnits()));
        return InventoryBoard.of(String.valueOf(h.getHospitalId()), h.getLabel(), units);
    }

    /**
     * Absolute stock set (the modal's Save). Creates the row when the hospital
     * has none for that group yet — a hospital registered through the profile
     * editor has no seeded rows, and refusing the first stock entry would be a
     * strange dead end.
     */
    @Transactional
    public InventoryBoard setUnits(Long hospitalId, BloodGroup group, int units) {
        Hospital h = require(hospitalId);
        if (units < 0) {
            throw new BusinessException("Stock cannot be negative.");
        }
        BloodInventory row = inventory.findByHospital_HospitalIdAndBloodGroup(hospitalId, group)
                .orElseGet(() -> {
                    BloodInventory fresh = new BloodInventory();
                    fresh.setHospital(h);
                    fresh.setBloodGroup(group);
                    return fresh;
                });
        row.setUnits(units);
        row.setUpdatedAt(Instant.now());
        inventory.save(row);
        return board(hospitalId);
    }

    /**
     * BR-4 — the fulfilment decrement: take up to {@code units} of
     * {@code group} out of this hospital's stock, never below zero, and return
     * what was ACTUALLY deducted.
     *
     * <p>The return value is why this is not a plain {@code -=}: the caller
     * records the real number in the request timeline, so a hospital that was
     * short is visible in its own audit trail instead of silently issuing blood
     * it does not have.
     */
    @Transactional
    public int decrement(Long hospitalId, BloodGroup group, int units) {
        if (hospitalId == null || group == null || units <= 0) {
            return 0;
        }
        BloodInventory row = inventory.findByHospital_HospitalIdAndBloodGroup(hospitalId, group)
                .orElse(null);
        if (row == null) {
            return 0;
        }
        int deducted = Math.min(row.getUnits(), units);
        row.setUnits(row.getUnits() - deducted);
        row.setUpdatedAt(Instant.now());
        inventory.save(row);
        return deducted;
    }

    private Hospital require(Long hospitalId) {
        if (hospitalId == null) {
            throw new BusinessException("No hospital selected.");
        }
        return hospitals.findById(hospitalId)
                .orElseThrow(() -> new NotFoundException("Unknown hospital: " + hospitalId));
    }
}
