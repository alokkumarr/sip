package com.sncr.saw.security.app.repository;

import com.sncr.saw.security.common.bean.Product;

import java.util.Map;

public interface ProductRepository {

    // returns how many products got created
    Map<Integer, String> createProductForOnboarding(Product prod);

    // updates one product
    boolean updateProduct(Product prod);

    // deletes product based on "ID"
    boolean deleteProduct(Long prodId);

    // returns product based on "ID"
    Product getProduct(Long prodId);

}
