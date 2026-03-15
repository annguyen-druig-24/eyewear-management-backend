//package com.swp391.eyewear_management_backend.service.impl;
//
//import com.swp391.eyewear_management_backend.dto.response.ProductInventoryResponse;
//import com.swp391.eyewear_management_backend.repository.ProductRepo;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class InventoryServiceImplTest {
//
//    @Mock
//    ProductRepo productRepo;
//
//    @InjectMocks
//    InventoryServiceImpl inventoryService;
//
//    @Test
//    void getAllProductsWithLatestInventoryQuantity_shouldDelegateToRepo() {
//        List<ProductInventoryResponse> expected = List.of(
//                new ProductInventoryResponse(1L, "P1", "SKU1", "Brand", null, null, null, null, null, null, null, null, null, null, null, 10)
//        );
//
//        when(productRepo.findAllProductsWithLatestInventoryQuantity()).thenReturn(expected);
//
//        List<ProductInventoryResponse> actual = inventoryService.getAllProductsWithLatestInventoryQuantity();
//
//        assertThat(actual).isSameAs(expected);
//        verify(productRepo).findAllProductsWithLatestInventoryQuantity();
//    }
//}
